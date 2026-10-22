package net.melvunx.zombieexpansion.entity;

import net.melvunx.zombieexpansion.entity.ai.ThrowArmGoal;
import net.melvunx.zombieexpansion.faction.Faction;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Zombie lanceur (faction Les Déterrés) : arrache son bras et le lance sur sa cible.
 *
 * Reste un {@link ZombieEntity} : mêmes stats et même mêlée que le zombie vanilla (budget de la
 * faction), on ajoute seulement un goal de tir prioritaire.
 *
 * Cycle : à plus de {@link #MELEE_RANGE} blocs il vise et lance son bras ; à moins de cette distance il
 * se bat au corps à corps. Une fois le bras lancé, il ne le fait repousser que lorsque sa cible n'est
 * pas collée à lui (décompte de {@link #ARM_REGROW_DELAY_TICKS}, puis animation de repousse) ; il peut
 * alors tirer de nouveau.
 *
 * Il ne ramasse aucun objet et ne porte aucun équipement : le modèle GeckoLib n'affiche ni armure ni
 * objet tenu.
 *
 * Deux états sont synchronisés par {@link DataTracker}, car le client en a besoin pour animer et
 * dessiner le modèle GeckoLib : « bras lancé » (l'os right_arm est masqué par le renderer) et « vise »
 * (animation aim). Les décomptes (repousse, délai de lâcher) restent côté serveur.
 *
 * Implémente {@link GeoEntity} : modèle, texture et animations GeckoLib (thrower_zombie.*).
 */
public class ThrowerZombieEntity extends ZombieEntity implements GeoEntity {
	/** En dessous de cette distance (blocs) il se bat au corps à corps ; au-delà, il lance son bras. */
	public static final double MELEE_RANGE = 5.0;
	/** Délai avant que le bras ne se mette à repousser (4 s), décompté seulement si la cible n'est pas à portée de mêlée. */
	private static final int ARM_REGROW_DELAY_TICKS = 80;
	/** Durées des animations de repousse (1,2 s et 0,7 s) : le goal ne tire pas avant leur fin. */
	private static final int REGROW_TICKS = 24;
	private static final int REGROW_BURST_TICKS = 14;
	private static final float THROW_SPEED = 1.2f;
	/** Imprécision du tir (en degrés) : volontairement large, c'est un mob de début de partie. */
	private static final float THROW_DIVERGENCE = 6.0f;

	/** Le bras part à 0,12 s dans l'animation throw : 2 ticks après son début (0,12 s = 2,4 ticks). */
	private static final int THROW_RELEASE_TICKS = 2;

	private static final TrackedData<Boolean> ARM_THROWN =
		DataTracker.registerData(ThrowerZombieEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	/** Vrai pendant la visée du goal : fait jouer l'animation aim (0,75 s, tenue sur la dernière image). */
	private static final TrackedData<Boolean> AIMING =
		DataTracker.registerData(ThrowerZombieEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	// Noms définis dans thrower_zombie.animation.json.
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.thrower_zombie.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.thrower_zombie.walk");
	// LoopType.DEFAULT : reprend le « hold_on_last_frame » du JSON (thenPlay forcerait un simple play-once).
	private static final RawAnimation AIM = RawAnimation.begin().then("animation.thrower_zombie.aim", Animation.LoopType.DEFAULT);
	private static final RawAnimation THROW = RawAnimation.begin().thenPlay("animation.thrower_zombie.throw");
	private static final String THROW_CONTROLLER = "throw";
	private static final String THROW_TRIGGER = "throw";

	// Mêlée : deux bras tant que le bras droit est là, un seul sinon ; chacune a une variante, tirée au hasard.
	private static final RawAnimation ATTACK_BOTH = RawAnimation.begin().thenPlay("animation.thrower_zombie.attack_both");
	private static final RawAnimation ATTACK_BOTH_ALT = RawAnimation.begin().thenPlay("animation.thrower_zombie.attack_both_alt");
	private static final RawAnimation ATTACK_SINGLE = RawAnimation.begin().thenPlay("animation.thrower_zombie.attack_single");
	private static final RawAnimation ATTACK_SINGLE_ALT = RawAnimation.begin().thenPlay("animation.thrower_zombie.attack_single_alt");
	private static final String ATTACK_CONTROLLER = "attack";
	// Repousse : l'animation anime l'échelle du bras de 0 à 1, l'os doit donc être visible dès son début.
	private static final RawAnimation REGROW = RawAnimation.begin().thenPlay("animation.thrower_zombie.regrow");
	private static final RawAnimation REGROW_BURST = RawAnimation.begin().thenPlay("animation.thrower_zombie.regrow_burst");
	private static final String REGROW_CONTROLLER = "regrow";

	/** Ticks avant le début de la repousse (valide si le bras est lancé). */
	private int armRegrowTicks;
	/** Ticks restants de l'animation de repousse (0 = pas en train de repousser). Serveur seulement. */
	private int regrowAnimTicks;
	/** Décompte avant que le bras ne parte réellement (0 = pas de lancer en cours) et sa cible. Serveur seulement. */
	private int releaseTicks;
	@Nullable
	private LivingEntity pendingTarget;

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public ThrowerZombieEntity(EntityType<? extends ZombieEntity> entityType, World world) {
		super(entityType, world);
	}

	/** Règle vanilla (sombre, au sol) + territoire des Déterrés pour les spawns naturels. */
	public static boolean canSpawn(EntityType<ThrowerZombieEntity> type, ServerWorldAccess world, SpawnReason reason, BlockPos pos, Random random) {
		boolean natural = reason == SpawnReason.NATURAL || reason == SpawnReason.CHUNK_GENERATION;
		return HostileEntity.canSpawnInDark(type, world, reason, pos, random)
			&& (!natural || Faction.allowsUnearthedSpawn(world, pos));
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(ARM_THROWN, false);
		builder.add(AIMING, false);
	}

	/** Pas de version bébé : un bébé qui lance un bras n'aurait pas de modèle cohérent. */
	@Override
	public void setBaby(boolean baby) {
		super.setBaby(false);
	}

	// ----- Pas d'objets ni d'équipement -----

	/** Le modèle n'affiche ni armure ni objet : on évite un équipement invisible mais actif. */
	@Override
	public boolean canGather(ItemStack stack) {
		return false;
	}

	@Override
	protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
	}

	/** Le zombie vanilla se coiffe parfois d'une citrouille (Halloween) : on la retire. */
	@Override
	public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
		EntityData data = super.initialize(world, difficulty, spawnReason, entityData);
		equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
		return data;
	}

	@Override
	protected void initGoals() {
		super.initGoals();
		// Priorité 1 : passe avant l'attaque au corps à corps (priorité 2 chez ZombieEntity).
		goalSelector.add(1, new ThrowArmGoal(this));
	}

	public boolean isArmThrown() {
		return getDataTracker().get(ARM_THROWN);
	}

	private void setArmThrown(boolean thrown) {
		getDataTracker().set(ARM_THROWN, thrown);
	}

	public boolean isAiming() {
		return getDataTracker().get(AIMING);
	}

	public void setAiming(boolean aiming) {
		getDataTracker().set(AIMING, aiming);
	}

	/** Vrai pendant l'animation de repousse : le bras est visible mais pas encore utilisable. */
	public boolean isRegrowing() {
		return regrowAnimTicks > 0;
	}

	/** Vrai entre le début de l'animation de lancer et le moment où le bras part. */
	public boolean isThrowing() {
		return releaseTicks > 0;
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			return;
		}
		if (releaseTicks > 0 && --releaseTicks == 0) {
			LivingEntity target = pendingTarget;
			pendingTarget = null;
			if (target != null && target.isAlive()) {
				releaseArm(target);
			}
		}
		if (regrowAnimTicks > 0) {
			regrowAnimTicks--;
		} else if (isArmThrown() && !targetInMeleeRange() && --armRegrowTicks <= 0) {
			startRegrow();
		}
	}

	/** Vrai si la cible est assez proche pour qu'il se batte au corps à corps (il n'a alors pas le temps de repousser). */
	private boolean targetInMeleeRange() {
		LivingEntity target = getTarget();
		return target != null && target.isAlive() && squaredDistanceTo(target) < MELEE_RANGE * MELEE_RANGE;
	}

	/**
	 * Lance la repousse : le bras redevient visible tout de suite (l'animation le fait grandir depuis
	 * une échelle de 0,01), puis le goal pourra tirer à la fin de l'animation. Deux variantes au hasard.
	 */
	private void startRegrow() {
		boolean burst = random.nextBoolean();
		regrowAnimTicks = burst ? REGROW_BURST_TICKS : REGROW_TICKS;
		setArmThrown(false);
		triggerAnim(REGROW_CONTROLLER, burst ? "regrow_burst" : "regrow");
	}

	/** Un coup de mêlée : animation à deux bras ou à un bras selon l'état du bras droit, variante au hasard. */
	@Override
	public boolean tryAttack(Entity target) {
		boolean singleArm = isArmThrown() || isRegrowing();
		boolean alt = random.nextBoolean();
		triggerAnim(ATTACK_CONTROLLER, (singleArm ? "attack_single" : "attack_both") + (alt ? "_alt" : ""));
		return super.tryAttack(target);
	}

	/**
	 * Déclenche le lancer : l'animation démarre tout de suite sur tous les clients, mais le bras ne
	 * part (et ne disparaît du modèle) que {@link #THROW_RELEASE_TICKS} ticks plus tard, au moment
	 * où la main le lâche dans l'animation.
	 */
	public void throwArm(LivingEntity target) {
		pendingTarget = target;
		releaseTicks = THROW_RELEASE_TICKS;
		// Serveur -> clients : GeckoLib envoie un paquet pour jouer l'animation partout.
		triggerAnim(THROW_CONTROLLER, THROW_TRIGGER);
	}

	/** Lance réellement le bras, avec la même balistique que le golem de neige. */
	private void releaseArm(LivingEntity target) {
		ThrownArmEntity arm = new ThrownArmEntity(getWorld(), this);
		double dx = target.getX() - getX();
		double dy = target.getBodyY(1.0 / 3.0) - arm.getY();
		double dz = target.getZ() - getZ();
		// Compense la gravité : plus la cible est loin, plus on vise haut.
		double lift = Math.sqrt(dx * dx + dz * dz) * 0.2;
		arm.setVelocity(dx, dy + lift, dz, THROW_SPEED, THROW_DIVERGENCE);

		getWorld().spawnEntity(arm);
		getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 1.0f,
			0.8f + MathHelper.nextFloat(random, 0.0f, 0.4f));
		armRegrowTicks = ARM_REGROW_DELAY_TICKS;
		setArmThrown(true);
	}

	/** Quatre contrôleurs : la pose de fond (marche, repos, visée), puis lancer, mêlée et repousse qui la recouvrent le temps de leur animation. */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "movement", 5, this::movementPredicate));
		// Ne joue rien tout seul : ne démarre que sur triggerAnim("throw", "throw").
		controllers.add(new AnimationController<>(this, THROW_CONTROLLER, 0, state -> PlayState.STOP)
			.triggerableAnim(THROW_TRIGGER, THROW));
		controllers.add(new AnimationController<>(this, ATTACK_CONTROLLER, 0, state -> PlayState.STOP)
			.triggerableAnim("attack_both", ATTACK_BOTH)
			.triggerableAnim("attack_both_alt", ATTACK_BOTH_ALT)
			.triggerableAnim("attack_single", ATTACK_SINGLE)
			.triggerableAnim("attack_single_alt", ATTACK_SINGLE_ALT));
		// Ajouté en dernier : pendant la repousse, il recouvre l'animation d'attaque sur l'os du bras droit.
		controllers.add(new AnimationController<>(this, REGROW_CONTROLLER, 0, state -> PlayState.STOP)
			.triggerableAnim("regrow", REGROW)
			.triggerableAnim("regrow_burst", REGROW_BURST));
	}

	private PlayState movementPredicate(AnimationState<ThrowerZombieEntity> state) {
		if (isAiming()) {
			return state.setAndContinue(AIM);
		}
		return state.setAndContinue(state.isMoving() ? WALK : IDLE);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return geoCache;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("ArmRegrowTicks", isArmThrown() ? armRegrowTicks : 0);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		armRegrowTicks = nbt.getInt("ArmRegrowTicks");
		setArmThrown(armRegrowTicks > 0);
	}
}
