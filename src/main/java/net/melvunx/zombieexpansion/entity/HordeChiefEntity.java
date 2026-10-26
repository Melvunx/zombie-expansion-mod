package net.melvunx.zombieexpansion.entity;

import net.melvunx.zombieexpansion.damage.DifficultyDamage;
import net.melvunx.zombieexpansion.faction.Faction;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.melvunx.zombieexpansion.registry.ModSounds;
import net.minecraft.sound.SoundEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Box;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Chef de horde : le « général » des Déterrés (ce n'est pas un boss : pas de barre de vie).
 *
 * Exception validée au budget de la faction : 30 PV, 3 points d'armure, et des dégâts égaux à ceux du
 * zombie vanilla + 0,5 cœur (1 PV) à chaque difficulté. Ces dégâts ne peuvent pas venir de l'attribut
 * d'attaque (voir {@link DifficultyDamage}) : la classe de base {@link FixedDamageZombieEntity} les applique.
 *
 * Ralliement : pendant un combat, il lance une impulsion (cri) qui donne un bonus léger et bref aux autres
 * Déterrés proches, puis doit attendre. Ses sous-fifres n'en profitent pas : ils sont déjà sa troupe.
 * Le bonus est un effet de potion de quelques secondes : il s'éteint de lui-même, y compris si le chef meurt.
 *
 * Spawn : surface uniquement (voir {@link #canSpawn}). Il arrive avec une escorte de 2 à 4 sous-fifres
 * ({@link HordeMinionEntity}), créée dans {@link #initialize}.
 *
 * Rendu GeckoLib (horde_chief.*) : idle, walk, attack (sur chaque coup) et boost (sur chaque cri de ralliement).
 * Le modèle ne montre ni armure ni objet tenu : le chef ne ramasse rien et n'a aucun équipement.
 */
public class HordeChiefEntity extends FixedDamageZombieEntity implements GeoEntity {
	private static final double MAX_HEALTH = 30.0;
	private static final double ARMOR = 3.0;
	/** Rayon du cri, durée du bonus (8 s) et temps de recharge (20 s). Valeurs à régler en jeu. */
	private static final double RALLY_RADIUS = 12.0;
	static final int RALLY_BONUS_TICKS = 160;
	private static final int RALLY_COOLDOWN_TICKS = 400;
	/** Délai avant la première impulsion après l'apparition. */
	private static final int RALLY_FIRST_DELAY_TICKS = 100;

	// Noms définis dans horde_chief.animation.json.
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.horde_chief.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.horde_chief.walk");
	private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.horde_chief.attack");
	private static final RawAnimation BOOST = RawAnimation.begin().thenPlay("animation.horde_chief.boost");
	private static final String ATTACK_CONTROLLER = "attack";
	private static final String BOOST_CONTROLLER = "boost";
	/** Le cri part à ~0,4 s dans l'animation boost : 8 ticks après son début. */
	private static final int RALLY_RELEASE_TICKS = 8;

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	private int rallyCooldown = RALLY_FIRST_DELAY_TICKS;
	/** Décompte entre le début de l'animation boost et l'effet du cri (0 = pas de cri en cours). */
	private int rallyReleaseTicks;

	/** Taille de l'escorte à l'apparition : 2 à 4 sous-fifres. */
	private static final int MIN_ESCORT = 2;
	private static final int MAX_ESCORT = 4;

	/** Zombie vanilla : 2,5 / 3 / 4,5 PV ; le chef a 1 PV de plus. */
	private static final DifficultyDamage.Table DAMAGE = new DifficultyDamage.Table(3.5f, 4.0f, 5.5f);

	public HordeChiefEntity(EntityType<? extends ZombieEntity> entityType, World world) {
		super(entityType, world);
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return ZombieEntity.createZombieAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, MAX_HEALTH)
			.add(EntityAttributes.GENERIC_ARMOR, ARMOR);
	}

	/** Règle vanilla (sombre, au sol) + territoire des Déterrés, et ciel visible pour un spawn naturel. */
	public static boolean canSpawn(EntityType<HordeChiefEntity> type, ServerWorldAccess world, SpawnReason reason, BlockPos pos, Random random) {
		boolean natural = reason == SpawnReason.NATURAL || reason == SpawnReason.CHUNK_GENERATION;
		return HostileEntity.canSpawnInDark(type, world, reason, pos, random)
			&& (!natural || (Faction.allowsUnearthedSpawn(world, pos) && world.isSkyVisible(pos)));
	}

	@Override
	protected DifficultyDamage.Table getDamageTable() {
		return DAMAGE;
	}

	/**
	 * Crée l'escorte au moment où le chef apparaît (spawn naturel, œuf, commande). C'est le même point que
	 * celui où le zombie vanilla fait apparaître son poulet de jockey.
	 */
	@Override
	public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
		EntityData data = super.initialize(world, difficulty, spawnReason, entityData);
		equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
		int count = MIN_ESCORT + random.nextInt(MAX_ESCORT - MIN_ESCORT + 1);
		for (int i = 0; i < count; i++) {
			spawnMinion(world, difficulty, spawnReason);
		}
		return data;
	}

	private void spawnMinion(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason) {
		HordeMinionEntity minion = ModEntities.HORDE_MINION.create(getWorld());
		if (minion == null) {
			return;
		}
		// Quelques essais autour du chef pour ne pas apparaître dans un bloc ; à défaut, sur lui.
		for (int attempt = 0; attempt < 6; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = 1.5 + random.nextDouble() * 1.5;
			minion.refreshPositionAndAngles(getX() + Math.cos(angle) * distance, getY(), getZ() + Math.sin(angle) * distance,
				random.nextFloat() * 360.0f, 0.0f);
			if (world.isSpaceEmpty(minion)) {
				break;
			}
			minion.refreshPositionAndAngles(getX(), getY(), getZ(), random.nextFloat() * 360.0f, 0.0f);
		}
		minion.initialize(world, difficulty, spawnReason, null);
		minion.setLeader(this);
		world.spawnEntityAndPassengers(minion);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}
		if (rallyReleaseTicks > 0) {
			// L'animation a démarré : l'effet du cri tombe quand le chef pousse son cri.
			if (--rallyReleaseTicks == 0) {
				rally(serverWorld);
			}
		} else if (--rallyCooldown <= 0 && getTarget() != null) {
			triggerAnim(BOOST_CONTROLLER, BOOST_CONTROLLER);
			rallyReleaseTicks = RALLY_RELEASE_TICKS;
			rallyCooldown = RALLY_COOLDOWN_TICKS;
		}
	}

	/**
	 * Le cri de ralliement : Vitesse I et Résistance I pendant {@link #RALLY_BONUS_TICKS} ticks pour les autres
	 * membres de la faction à portée (zombies vanilla, villageois, lanceurs...). Les sous-fifres sont exclus.
	 * La faction se lit par tag (Faction.UNEARTHED), pas par instanceof ; seuls les sous-fifres sont écartés
	 * explicitement, parce que c'est une règle propre au chef.
	 */
	private void rally(ServerWorld world) {
		Box area = getBoundingBox().expand(RALLY_RADIUS);
		for (LivingEntity ally : world.getEntitiesByClass(LivingEntity.class, area,
			entity -> entity != this && !(entity instanceof HordeMinionEntity) && Faction.UNEARTHED.includes(entity.getType()))) {
			ally.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, RALLY_BONUS_TICKS, 0), this);
			ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, RALLY_BONUS_TICKS, 0), this);
		}
		// Les sous-fifres ne reçoivent aucun bonus, mais ils réagissent au cri (animation seulement).
		for (HordeMinionEntity minion : world.getEntitiesByClass(HordeMinionEntity.class, area, minion -> minion.getLeader() == this)) {
			minion.playBoost();
		}
		world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getEyeY() + 0.5, getZ(), 8, 0.4, 0.3, 0.4, 0.0);
		// Le cri de ralliement (audible de loin, d'où le volume de 2).
		world.playSound(null, getX(), getY(), getZ(), ModSounds.HORDE_CHIEF_BOOST, SoundCategory.HOSTILE, 2.0f, 1.0f);
	}

	/** Le modèle n'affiche aucun équipement : on évite une armure invisible mais active. */
	@Override
	public boolean canGather(ItemStack stack) {
		return false;
	}

	@Override
	protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
	}

	/** Grondement au repos, à la place du gémissement du zombie vanilla (blessure et mort restent ceux du zombie). */
	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.HORDE_CHIEF_AMBIENT;
	}

	/** Un coup de mêlée : l'animation démarre en même temps que les dégâts (le coup visuel arrive à 0,3 s). */
	@Override
	public boolean tryAttack(Entity target) {
		triggerAnim(ATTACK_CONTROLLER, ATTACK_CONTROLLER);
		getWorld().playSound(null, getX(), getY(), getZ(), ModSounds.HORDE_CHIEF_ATTACK, SoundCategory.HOSTILE, 1.0f, 0.9f + random.nextFloat() * 0.2f);
		return super.tryAttack(target);
	}

	/** La pose de fond (marche, repos), puis attaque et boost qui la recouvrent le temps de leur animation. */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "movement", 5, this::movementPredicate));
		controllers.add(new AnimationController<>(this, ATTACK_CONTROLLER, 0, state -> PlayState.STOP)
			.triggerableAnim(ATTACK_CONTROLLER, ATTACK));
		controllers.add(new AnimationController<>(this, BOOST_CONTROLLER, 0, state -> PlayState.STOP)
			.triggerableAnim(BOOST_CONTROLLER, BOOST));
	}

	private PlayState movementPredicate(AnimationState<HordeChiefEntity> state) {
		return state.setAndContinue(state.isMoving() ? WALK : IDLE);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return geoCache;
	}

	/** Un chef ne brûle pas au soleil : c'est un meneur à 30 PV, il ne doit pas mourir à l'aube. */
	@Override
	protected boolean burnsInDaylight() {
		return false;
	}

	/** Un chef n'est jamais un bébé. */
	@Override
	public void setBaby(boolean baby) {
		super.setBaby(false);
	}
}
