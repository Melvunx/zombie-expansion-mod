package net.melvunx.zombieexpansion.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.melvunx.zombieexpansion.faction.FrostVulnerability;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Zombie gelé : variante des biomes enneigés dont les coups font geler la cible.
 *
 * Hérite de {@link ZombieEntity} pour récupérer gratuitement toute l'IA vanilla
 * (poursuite du joueur, attaque des villageois, casse de portes, etc.) : on ne
 * redéfinit que ce qui change.
 *
 * Implémente {@link GeoEntity} : c'est le contrat GeckoLib qui dit "cette entité
 * est animée par un modèle .geo.json et un fichier .animation.json".
 */
public class FrozenZombieEntity extends ZombieEntity implements GeoEntity {
	/** Ticks de gel ajoutés au-dessus du seuil de dégâts (140 ticks = 7 s de neige poudreuse). */
	private static final int FREEZE_BONUS_TICKS = 60;

	// Noms définis dans frozen_zombie.animation.json.
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.frozen_zombie.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.frozen_zombie.walk");
	private static final RawAnimation HITTING = RawAnimation.begin().thenPlay("animation.frozen_zombie.hitting");

	private static final String ATTACK_CONTROLLER = "attack";
	private static final String HITTING_TRIGGER = "hitting";

	/**
	 * Durées de retour en zombie, calquées sur SkeletonEntity (squelette → Stray) :
	 * 140 ticks (7 s) d'exposition, puis 300 ticks (15 s) de transformation visible (tremblement).
	 */
	public static final int HOT_EXPOSURE_TICKS = 140;
	public static final int REVERSION_TICKS = 300;

	/** Synchronisé serveur → clients : le renderer en a besoin pour faire trembler le modèle. */
	private static final TrackedData<Boolean> CONVERTING_TO_ZOMBIE =
		DataTracker.registerData(FrozenZombieEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	/** Ticks passés d'affilée en biome chaud (-1 = pas en biome chaud). Non persisté, comme chez le squelette. */
	private int hotExposureTicks;

	/** Ticks restants avant de redevenir un zombie (valide seulement si isConvertingToZombie()). */
	private int conversionTicks;

	/** Cache GeckoLib : stocke l'état d'animation de cette instance précise. */
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public FrozenZombieEntity(EntityType<? extends ZombieEntity> entityType, World world) {
		super(entityType, world);
	}

	/**
	 * Règle de spawn : celle du zombie vanilla (sombre, sur le sol) + ciel visible.
	 * Sous terre, c'est le zombie vanilla qui apparaît (voir SpawnRestrictionMixin).
	 * Les œufs, spawners et commandes ne passent pas par ici ou sont exemptés.
	 */
	public static boolean canSpawn(EntityType<FrozenZombieEntity> type, ServerWorldAccess world, SpawnReason reason, BlockPos pos, Random random) {
		boolean natural = reason == SpawnReason.NATURAL || reason == SpawnReason.CHUNK_GENERATION;
		return HostileEntity.canSpawnInDark(type, world, reason, pos, random) && (!natural || world.isSkyVisible(pos));
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(CONVERTING_TO_ZOMBIE, false);
	}

	public boolean isConvertingToZombie() {
		return getDataTracker().get(CONVERTING_TO_ZOMBIE);
	}

	private void setConvertingToZombie(boolean converting) {
		getDataTracker().set(CONVERTING_TO_ZOMBIE, converting);
	}

	/**
	 * Logique côté serveur uniquement. Même squelette que SkeletonEntity#tick :
	 * on compte l'exposition, on lance la conversion, on l'annule dès qu'on quitte la zone chaude.
	 */
	@Override
	public void tick() {
		if (!getWorld().isClient && isAlive() && !isAiDisabled()) {
			boolean inHotBiome = FrostVulnerability.isInHotBiome(getWorld(), getBlockPos());
			FrostVulnerability.updateHeatSlowdown(this, inHotBiome);

			if (!inHotBiome) {
				hotExposureTicks = -1;
				setConvertingToZombie(false);
			} else if (isConvertingToZombie()) {
				if (--conversionTicks < 0) {
					convertToZombie();
				}
			} else if (++hotExposureTicks >= HOT_EXPOSURE_TICKS) {
				conversionTicks = REVERSION_TICKS;
				setConvertingToZombie(true);
			}
		}
		super.tick();
	}

	/** Redevient un zombie vanilla (bébé, équipement et nom conservés par convertTo). */
	private void convertToZombie() {
		convertTo(EntityType.ZOMBIE, true);
		if (!isSilent()) {
			// Réutilise le son vanilla de la conversion squelette → Stray, faute de son dédié.
			getWorld().syncWorldEvent(null, WorldEvents.SKELETON_CONVERTS_TO_STRAY, getBlockPos(), 0);
		}
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("ZombieConversionTime", isConvertingToZombie() ? conversionTicks : -1);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.contains("ZombieConversionTime", NbtElement.NUMBER_TYPE) && nbt.getInt("ZombieConversionTime") > -1) {
			conversionTicks = nbt.getInt("ZombieConversionTime");
			setConvertingToZombie(true);
		}
	}

	/**
	 * Un contrôleur = une "piste" d'animation. On en utilise deux pour pouvoir
	 * superposer l'attaque à la marche sans qu'elles se coupent mutuellement.
	 */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// 5 = ticks de fondu entre deux animations (évite les sauts de pose).
		controllers.add(new AnimationController<>(this, "movement", 5, this::movementPredicate));

		// Ne joue rien tout seul : ne démarre que sur triggerAnim("attack", "hitting").
		controllers.add(new AnimationController<>(this, ATTACK_CONTROLLER, 0, state -> PlayState.STOP)
			.triggerableAnim(HITTING_TRIGGER, HITTING));
	}

	private PlayState movementPredicate(AnimationState<FrozenZombieEntity> state) {
		return state.setAndContinue(state.isMoving() ? WALK : IDLE);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	/**
	 * Appelé à chaque coup porté. On laisse le vanilla infliger les dégâts,
	 * puis on gèle la cible si le coup a touché.
	 */
	@Override
	public boolean tryAttack(Entity target) {
		boolean hit = super.tryAttack(target);

		if (hit) {
			// Serveur -> clients : GeckoLib envoie un paquet pour jouer l'animation partout.
			triggerAnim(ATTACK_CONTROLLER, HITTING_TRIGGER);
		}

		// canFreeze() respecte le tag freeze_immune_entity_types et l'armure en cuir.
		if (hit && target instanceof LivingEntity living && living.canFreeze()) {
			int frozenTicks = living.getMinFreezeDamageTicks() + FREEZE_BONUS_TICKS;
			// max() pour ne jamais réduire un gel déjà plus avancé (neige poudreuse).
			living.setFrozenTicks(Math.max(living.getFrozenTicks(), frozenTicks));
		}
		return hit;
	}

	/**
	 * Faiblesse au feu de la faction Givre : on amplifie les dégâts *avant* de laisser
	 * le vanilla appliquer armure, absorption et résistances.
	 */
	@Override
	public boolean damage(DamageSource source, float amount) {
		return super.damage(source, FrostVulnerability.amplify(source, amount));
	}

	/** Un zombie de glace ne brûle pas au soleil (contrairement au zombie vanilla). */
	@Override
	protected boolean burnsInDaylight() {
		return false;
	}
}
