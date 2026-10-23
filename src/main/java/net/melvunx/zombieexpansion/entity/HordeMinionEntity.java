package net.melvunx.zombieexpansion.entity;

import java.util.UUID;

import net.melvunx.zombieexpansion.damage.DifficultyDamage;
import net.melvunx.zombieexpansion.entity.ai.FollowLeaderGoal;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

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
 */
public class HordeMinionEntity extends FixedDamageZombieEntity {
	private static final double MAX_HEALTH = 14.0;
	/** Mêmes dégâts que le zombie vanilla : 2,5 / 3 / 4,5 PV. */
	private static final DifficultyDamage.Table DAMAGE = new DifficultyDamage.Table(2.5f, 3.0f, 4.5f);
	/** Fréquence (en ticks) à laquelle il retrouve son chef et recopie sa cible. */
	private static final int LEADER_CHECK_TICKS = 20;

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
		return data;
	}

	@Override
	protected void initGoals() {
		super.initGoals();
		goalSelector.add(6, new FollowLeaderGoal(this));
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
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		leaderId = nbt.containsUuid("LeaderId") ? nbt.getUuid("LeaderId") : null;
	}
}
