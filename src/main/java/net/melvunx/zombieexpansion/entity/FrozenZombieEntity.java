package net.melvunx.zombieexpansion.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.melvunx.zombieexpansion.faction.FrostVulnerability;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
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
