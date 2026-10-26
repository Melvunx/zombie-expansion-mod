package net.melvunx.zombieexpansion.entity;

import java.util.UUID;

import net.melvunx.zombieexpansion.damage.DifficultyDamage;
import net.melvunx.zombieexpansion.entity.ai.FollowLeaderGoal;
import net.melvunx.zombieexpansion.registry.ModSounds;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
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
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Sous-fifre : zombie faible qui accompagne un {@link HordeChiefEntity}.
 *
 * 14 PV (20 pour le zombie vanilla) et une épée en bois. L'épée ajouterait des dégâts à l'attribut
 * d'attaque, ce qui dépasse le budget de la faction : comme le chef, il utilise donc une table de dégâts
 * fixes ({@link FixedDamageZombieEntity}), ici ceux du zombie vanilla. C'est une exception validée.
 *
 * Il brûle au soleil (comportement vanilla). Le futur sous-fifre du joueur (propriétaire, rappel dans un
 * item) sera cette même entité avec un propriétaire : pas encore implémenté.
 *
 * Il suit son chef (UUID mémorisé en NBT, {@link FollowLeaderGoal}) et attaque la cible de son chef.
 * Il n'apparaît pas seul dans la nature : c'est le chef qui l'amène.
 *
 * Trois variantes visuelles ({@link HordeMinionVariant}) tirées au hasard à l'apparition : visage,
 * tatouages et animations changent, pas les stats. La variante est un état synchronisé par
 * {@link DataTracker} (le client en a besoin pour choisir texture et animations) et sauvegardé en NBT.
 * Aucun objet n'est ramassé : le modèle ne montre que l'épée en bois (calque d'objet tenu du renderer).
 */
public class HordeMinionEntity extends FixedDamageZombieEntity implements GeoEntity {
	private static final double MAX_HEALTH = 14.0;
	/** Mêmes dégâts que le zombie vanilla : 2,5 / 3 / 4,5 PV. */
	private static final DifficultyDamage.Table DAMAGE = new DifficultyDamage.Table(2.5f, 3.0f, 4.5f);
	/** Fréquence (en ticks) à laquelle il retrouve son chef et recopie sa cible. */
	private static final int LEADER_CHECK_TICKS = 20;

	private static final TrackedData<Integer> VARIANT =
		DataTracker.registerData(HordeMinionEntity.class, TrackedDataHandlerRegistry.INTEGER);
	/**
	 * Heure du monde (en ticks) où la lueur des tatouages s'éteint (0 = jamais allumée). On synchronise cette
	 * heure une seule fois plutôt qu'un compteur à chaque tick : le client en déduit l'intensité (voir
	 * {@link #getBoostGlow}).
	 */
	private static final TrackedData<Long> BOOST_GLOW_END =
		DataTracker.registerData(HordeMinionEntity.class, TrackedDataHandlerRegistry.LONG);
	/**
	 * Durée de la lueur : celle du bonus du chef (8 s), la même sur le serveur et le client. Sert à calculer
	 * la part restante (100 % au début, 0 % à la fin).
	 */
	private static final int BOOST_GLOW_TICKS = HordeChiefEntity.RALLY_BONUS_TICKS;
	private static final String ATTACK_CONTROLLER = "attack";
	private static final String BOOST_CONTROLLER = "boost";

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	@Nullable
	private UUID leaderId;
	@Nullable
	private HordeChiefEntity leader;

	public HordeMinionEntity(EntityType<? extends ZombieEntity> entityType, World world) {
		super(entityType, world);
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return ZombieEntity.createZombieAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, MAX_HEALTH);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(VARIANT, 0);
		builder.add(BOOST_GLOW_END, 0L);
	}

	public HordeMinionVariant getVariant() {
		return HordeMinionVariant.byOrdinal(getDataTracker().get(VARIANT));
	}

	/**
	 * Intensité de la lueur des tatouages, de 1 (début du boost du chef) à 0 (fin), de façon linéaire.
	 * Calculée côté client à partir de l'heure de fin synchronisée ; {@code partialTick} lisse l'image entre deux ticks.
	 */
	public float getBoostGlow(float partialTick) {
		long end = getDataTracker().get(BOOST_GLOW_END);
		if (end == 0L) {
			return 0.0f;
		}
		float remaining = end - (getWorld().getTime() + partialTick);
		return MathHelper.clamp(remaining / BOOST_GLOW_TICKS, 0.0f, 1.0f);
	}

	public void setVariant(HordeMinionVariant variant) {
		getDataTracker().set(VARIANT, variant.ordinal());
	}

	@Override
	protected DifficultyDamage.Table getDamageTable() {
		return DAMAGE;
	}

	@Override
	public void setBaby(boolean baby) {
		super.setBaby(false);
	}

	/** Toujours l'épée en bois, jamais l'équipement aléatoire du zombie vanilla. */
	@Override
	protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
		equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
	}

	@Override
	public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
		EntityData data = super.initialize(world, difficulty, spawnReason, entityData);
		// ZombieEntity#initialize a posé notre épée (initEquipment) mais peut avoir ajouté une citrouille d'Halloween.
		equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
		setVariant(HordeMinionVariant.byOrdinal(random.nextInt(HordeMinionVariant.values().length)));
		return data;
	}

	/** Le modèle n'affiche que l'épée : on évite un équipement invisible mais actif. */
	@Override
	public boolean canGather(ItemStack stack) {
		return false;
	}

	@Override
	protected void initGoals() {
		super.initGoals();
		goalSelector.add(6, new FollowLeaderGoal(this));
	}

	// ----- Sons -----

	/** Chaque variante a sa voix (grognement, ricanement, gloussement) ; blessure et mort restent ceux du zombie. */
	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.minion(getVariant(), "ambient");
	}

	private void playVariantSound(String action) {
		getWorld().playSound(null, getX(), getY(), getZ(), ModSounds.minion(getVariant(), action), SoundCategory.HOSTILE,
			1.0f, 0.9f + random.nextFloat() * 0.2f);
	}

	// ----- Animations -----

	/** Coup de mêlée : l'animation d'attaque de la variante démarre en même temps que les dégâts. */
	@Override
	public boolean tryAttack(Entity target) {
		triggerAnim(ATTACK_CONTROLLER, getVariant().trigger("attack"));
		playVariantSound("attack");
		return super.tryAttack(target);
	}

	/**
	 * Réaction visuelle au cri du chef (aucun effet de jeu : les sous-fifres sont exclus du ralliement) : petite
	 * animation, puis les tatouages s'allument à 100 % et s'éteignent linéairement pendant toute la durée du bonus du chef.
	 */
	public void playBoost() {
		triggerAnim(BOOST_CONTROLLER, getVariant().trigger("boost"));
		playVariantSound("boost");
		getDataTracker().set(BOOST_GLOW_END, getWorld().getTime() + BOOST_GLOW_TICKS);
	}

	/** La pose de fond (marche, repos) puis deux contrôleurs ponctuels, un par action, avec une animation par variante. */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "movement", 5, this::movementPredicate));
		AnimationController<HordeMinionEntity> attack = new AnimationController<>(this, ATTACK_CONTROLLER, 0, state -> PlayState.STOP);
		AnimationController<HordeMinionEntity> boost = new AnimationController<>(this, BOOST_CONTROLLER, 0, state -> PlayState.STOP);
		for (HordeMinionVariant variant : HordeMinionVariant.values()) {
			attack.triggerableAnim(variant.trigger("attack"), variant.attack());
			boost.triggerableAnim(variant.trigger("boost"), variant.boost());
		}
		controllers.add(attack);
		controllers.add(boost);
	}

	private PlayState movementPredicate(AnimationState<HordeMinionEntity> state) {
		HordeMinionVariant variant = getVariant();
		return state.setAndContinue(state.isMoving() ? variant.walk() : variant.idle());
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return geoCache;
	}

	// ----- Chef -----

	public void setLeader(HordeChiefEntity leader) {
		this.leader = leader;
		this.leaderId = leader.getUuid();
	}

	@Nullable
	public HordeChiefEntity getLeader() {
		return leader != null && leader.isAlive() ? leader : null;
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld() instanceof ServerWorld serverWorld && age % LEADER_CHECK_TICKS == 0) {
			if ((leader == null || !leader.isAlive()) && leaderId != null
				&& serverWorld.getEntity(leaderId) instanceof HordeChiefEntity found && found.isAlive()) {
				leader = found;
			}
			HordeChiefEntity chief = getLeader();
			// La troupe se bat ensemble : le sous-fifre reprend la cible de son chef.
			if (chief != null && getTarget() == null) {
				LivingEntity chiefTarget = chief.getTarget();
				if (chiefTarget != null && chiefTarget.isAlive()) {
					setTarget(chiefTarget);
				}
			}
		}
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (leaderId != null) {
			nbt.putUuid("LeaderId", leaderId);
		}
		nbt.putInt("Variant", getVariant().ordinal());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		leaderId = nbt.containsUuid("LeaderId") ? nbt.getUuid("LeaderId") : null;
		setVariant(HordeMinionVariant.byOrdinal(nbt.getInt("Variant")));
	}
}
