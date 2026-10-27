package net.melvunx.zombieexpansion.entity;

import net.melvunx.zombieexpansion.damage.DifficultyDamage;
import net.melvunx.zombieexpansion.faction.Faction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.nbt.NbtCompound;
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
 * Coureur : un zombie maigre et très rapide des Déterrés. Sa vitesse est sa force, il est fragile et frappe
 * moins fort : le joueur doit se battre en reculant, sans pouvoir le semer à pied.
 *
 * Stats : 12 PV (20 pour le zombie vanilla), vitesse ×1,4 (0,23 → 0,322), dégâts de mêlée 1 PV (0,5 cœur) sous
 * ceux du zombie vanilla à chaque difficulté. Les dégâts passent par {@link FixedDamageZombieEntity} : l'attribut
 * d'attaque est rééchelonné par le jeu contre un joueur, il ne donnerait pas exactement « vanilla - 1 PV ».
 * Plus faible que le zombie vanilla, il reste dans le budget de la faction (aucune exception à valider).
 *
 * Il brûle au soleil comme un zombie vanilla. Aucun bébé : un bébé est déjà vif, et le modèle est un adulte maigre.
 *
 * Équipement : comme le zombie vanilla et le squelette, sans aucune restriction. Il garde donc l'équipement
 * initial et le ramassage d'objets de {@link ZombieEntity} (armure de tout niveau, armes, pelle), et le
 * modèle a un os par pièce d'armure (calque d'armure du renderer).
 *
 * Variante des grottes : s'il apparaît sans voir le ciel, il reçoit la texture {@code runner_zombie_cave}
 * (état synchronisé par {@link DataTracker}, le client en a besoin pour choisir la texture ; sauvegardé en NBT).
 * C'est un choix fait une fois à l'apparition : un coureur qui change de lieu garde sa texture.
 */
public class RunnerZombieEntity extends FixedDamageZombieEntity implements GeoEntity {
	private static final double MAX_HEALTH = 12.0;
	/** 40 % plus rapide que le zombie vanilla (0,23). */
	private static final double MOVEMENT_SPEED = 0.23 * 1.4;
	/** Zombie vanilla : 2,5 / 3 / 4,5 PV, moins 1 PV (0,5 cœur) à chaque difficulté. */
	private static final DifficultyDamage.Table DAMAGE = new DifficultyDamage.Table(1.5f, 2.0f, 3.5f);

	private static final TrackedData<Boolean> CAVE =
		DataTracker.registerData(RunnerZombieEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final String ATTACK_CONTROLLER = "attack";
	private static final String[] ATTACKS = {"attack_jab", "attack_swipe", "attack_lunge"};

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public RunnerZombieEntity(EntityType<? extends ZombieEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(CAVE, false);
	}

	/** Vrai pour la variante des grottes (texture {@code runner_zombie_cave}). */
	public boolean isCaveVariant() {
		return getDataTracker().get(CAVE);
	}

	public void setCaveVariant(boolean cave) {
		getDataTracker().set(CAVE, cave);
	}

	/**
	 * Variante choisie ici : même critère que le reste du mod pour « sous terre » (le ciel n'est pas visible,
	 * voir le gelé et le chef de horde). Vaut pour tous les modes d'apparition (naturel, œuf, commande).
	 */
	@Override
	public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
		EntityData data = super.initialize(world, difficulty, spawnReason, entityData);
		setCaveVariant(!world.isSkyVisible(getBlockPos()));
		return data;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Cave", isCaveVariant());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		setCaveVariant(nbt.getBoolean("Cave"));
	}

	// ----- Animations -----

	/** Coup de mêlée : une des trois animations d'attaque au hasard, jouée en même temps que les dégâts. */
	@Override
	public boolean tryAttack(Entity target) {
		triggerAnim(ATTACK_CONTROLLER, ATTACKS[random.nextInt(ATTACKS.length)]);
		return super.tryAttack(target);
	}

	/** Pose de fond (repos, marche, course) puis un contrôleur ponctuel pour les attaques. */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "movement", 5, this::movementPredicate));
		AnimationController<RunnerZombieEntity> attack = new AnimationController<>(this, ATTACK_CONTROLLER, 0, state -> PlayState.STOP);
		for (String name : ATTACKS) {
			attack.triggerableAnim(name, RawAnimation.begin().thenPlay("animation.runner_zombie." + name));
		}
		controllers.add(attack);
	}

	/** Il court dès qu'il a une cible (état « agressif » du mob), sinon il marche ; à l'arrêt, il s'échauffe. */
	private PlayState movementPredicate(AnimationState<RunnerZombieEntity> state) {
		if (!state.isMoving()) {
			return state.setAndContinue(RawAnimation.begin().thenLoop("animation.runner_zombie.idle"));
		}
		return state.setAndContinue(RawAnimation.begin().thenLoop(isAttacking()
			? "animation.runner_zombie.run" : "animation.runner_zombie.walk"));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return geoCache;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return ZombieEntity.createZombieAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, MAX_HEALTH)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, MOVEMENT_SPEED);
	}

	/** Règle vanilla (sombre, au sol) + territoire des Déterrés (surface et grottes jusqu'à Y -8). */
	public static boolean canSpawn(EntityType<RunnerZombieEntity> type, ServerWorldAccess world, SpawnReason reason, BlockPos pos, Random random) {
		boolean natural = reason == SpawnReason.NATURAL || reason == SpawnReason.CHUNK_GENERATION;
		return HostileEntity.canSpawnInDark(type, world, reason, pos, random)
			&& (!natural || Faction.allowsUnearthedSpawn(world, pos));
	}

	@Override
	protected DifficultyDamage.Table getDamageTable() {
		return DAMAGE;
	}

	@Override
	public void setBaby(boolean baby) {
		super.setBaby(false);
	}
}
