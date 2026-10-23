package net.melvunx.zombieexpansion.entity;

import net.melvunx.zombieexpansion.damage.DifficultyDamage;
import net.melvunx.zombieexpansion.faction.Faction;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.minecraft.entity.EntityData;
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

/**
 * Chef de horde : le « général » des Déterrés (ce n'est pas un boss : pas de barre de vie).
 *
 * Exception validée au budget de la faction : 30 PV, 3 points d'armure, et des dégâts égaux à ceux du
 * zombie vanilla + 0,5 cœur (1 PV) à chaque difficulté. Ces dégâts ne peuvent pas venir de l'attribut
 * d'attaque (voir {@link DifficultyDamage}) : la classe de base {@link FixedDamageZombieEntity} les applique.
 *
 * Spawn : surface uniquement (voir {@link #canSpawn}). Il arrive avec une escorte de 2 à 4 sous-fifres
 * ({@link HordeMinionEntity}), créée dans {@link #initialize}.
 */
public class HordeChiefEntity extends FixedDamageZombieEntity {
	private static final double MAX_HEALTH = 30.0;
	private static final double ARMOR = 3.0;
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

	/** Un chef n'est jamais un bébé. */
	@Override
	public void setBaby(boolean baby) {
		super.setBaby(false);
	}
}
