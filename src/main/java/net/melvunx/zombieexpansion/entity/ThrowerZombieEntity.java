package net.melvunx.zombieexpansion.entity;

import net.melvunx.zombieexpansion.entity.ai.ThrowArmGoal;
import net.melvunx.zombieexpansion.faction.Faction;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

/**
 * Zombie lanceur (faction Les Déterrés) : arrache son bras et le lance sur sa cible.
 *
 * Reste un {@link ZombieEntity} : mêmes stats et même mêlée que le zombie vanilla (budget de la
 * faction), on ajoute seulement un goal de tir prioritaire. Le bras repousse après quelques
 * secondes ; pendant ce temps le zombie se bat au corps à corps.
 *
 * L'état « bras lancé » est synchronisé par un {@link DataTracker} car le client en a besoin pour
 * masquer le bras sur le modèle (voir ThrowerZombieModel). Le décompte de repousse, lui, reste
 * côté serveur.
 */
public class ThrowerZombieEntity extends ZombieEntity {
	/** Repousse du bras : 5 secondes. À ajuster en jeu. */
	private static final int ARM_REGROW_TICKS = 100;
	private static final float THROW_SPEED = 1.2f;
	/** Imprécision du tir (en degrés) : volontairement large, c'est un mob de début de partie. */
	private static final float THROW_DIVERGENCE = 6.0f;

	private static final TrackedData<Boolean> ARM_THROWN =
		DataTracker.registerData(ThrowerZombieEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	private int armRegrowTicks;

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
	}

	/** Pas de version bébé : un bébé qui lance un bras n'aurait pas de modèle cohérent. */
	@Override
	public void setBaby(boolean baby) {
		super.setBaby(false);
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

	@Override
	public void tick() {
		super.tick();
		if (!getWorld().isClient && isArmThrown() && --armRegrowTicks <= 0) {
			setArmThrown(false);
		}
	}

	/** Lance le bras vers la cible, avec la même balistique que le golem de neige. */
	public void throwArm(LivingEntity target) {
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
		swingHand(Hand.MAIN_HAND);

		armRegrowTicks = ARM_REGROW_TICKS;
		setArmThrown(true);
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
