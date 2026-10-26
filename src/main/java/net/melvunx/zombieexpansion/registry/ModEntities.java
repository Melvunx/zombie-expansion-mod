package net.melvunx.zombieexpansion.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.melvunx.zombieexpansion.ZombieExpansion;
import net.melvunx.zombieexpansion.entity.BlindedZombieEntity;
import net.melvunx.zombieexpansion.entity.FrozenZombieEntity;
import net.melvunx.zombieexpansion.entity.HordeChiefEntity;
import net.melvunx.zombieexpansion.entity.HordeMinionEntity;
import net.melvunx.zombieexpansion.entity.RunnerZombieEntity;
import net.melvunx.zombieexpansion.entity.ThrownArmEntity;
import net.melvunx.zombieexpansion.entity.ThrowerZombieEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.world.Heightmap;

/**
 * Registre des types d'entités du mod.
 *
 * Un {@link EntityType} est la "fiche d'identité" d'une entité (taille, groupe de
 * spawn, constructeur) ; l'instance vit dans le monde, le type est unique.
 */
public final class ModEntities {
	public static final EntityType<FrozenZombieEntity> FROZEN_ZOMBIE = register(
		"frozen_zombie",
		EntityType.Builder.create(FrozenZombieEntity::new, SpawnGroup.MONSTER)
			.dimensions(0.6f, 1.95f) // même hitbox que le zombie vanilla
			.maxTrackingRange(8)
	);

	public static final EntityType<BlindedZombieEntity> BLINDED_ZOMBIE = register(
		"blinded_zombie",
		EntityType.Builder.create(BlindedZombieEntity::new, SpawnGroup.MONSTER)
			.dimensions(0.6f, 1.95f)
			.maxTrackingRange(8)
	);

	public static final EntityType<ThrowerZombieEntity> THROWER_ZOMBIE = register(
		"thrower_zombie",
		EntityType.Builder.create(ThrowerZombieEntity::new, SpawnGroup.MONSTER)
			.dimensions(0.6f, 1.95f)
			.maxTrackingRange(8)
	);

	public static final EntityType<HordeChiefEntity> HORDE_CHIEF = register(
		"horde_chief",
		EntityType.Builder.create(HordeChiefEntity::new, SpawnGroup.MONSTER)
			.dimensions(0.66f, 2.15f) // zombie vanilla + 10 %, comme l'échelle du renderer
			.maxTrackingRange(8)
	);

	public static final EntityType<HordeMinionEntity> HORDE_MINION = register(
		"horde_minion",
		EntityType.Builder.create(HordeMinionEntity::new, SpawnGroup.MONSTER)
			.dimensions(0.51f, 1.66f) // zombie vanilla - 15 %, comme l'échelle du renderer
			.maxTrackingRange(8)
	);

	public static final EntityType<RunnerZombieEntity> RUNNER_ZOMBIE = register(
		"runner_zombie",
		EntityType.Builder.create(RunnerZombieEntity::new, SpawnGroup.MONSTER)
			.dimensions(0.6f, 1.95f)
			.maxTrackingRange(8)
	);

	/** Projectile : groupe MISC (hors plafond de monstres), petite hitbox, suivi réseau court comme la boule de neige. */
	public static final EntityType<ThrownArmEntity> THROWN_ARM = register(
		"thrown_arm",
		EntityType.Builder.<ThrownArmEntity>create(ThrownArmEntity::new, SpawnGroup.MISC)
			.dimensions(0.25f, 0.25f)
			.maxTrackingRange(4)
			.trackingTickInterval(10)
	);

	private ModEntities() {
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		return Registry.register(Registries.ENTITY_TYPE, ZombieExpansion.id(name), builder.build(name));
	}

	/** Appelé depuis {@link ZombieExpansion#onInitialize()} : force le chargement de la classe (donc des champs statiques). */
	public static void register() {
		// Sans attributs par défaut, le jeu plante au spawn de toute entité vivante.
		FabricDefaultAttributeRegistry.register(FROZEN_ZOMBIE, ZombieEntity.createZombieAttributes());

		FabricDefaultAttributeRegistry.register(BLINDED_ZOMBIE, BlindedZombieEntity.createAttributes());
		// Mêmes attributs que le zombie vanilla : le budget de la faction Les Déterrés l'impose.
		FabricDefaultAttributeRegistry.register(THROWER_ZOMBIE, ZombieEntity.createZombieAttributes());
		FabricDefaultAttributeRegistry.register(HORDE_CHIEF, HordeChiefEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(HORDE_MINION, HordeMinionEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(RUNNER_ZOMBIE, RunnerZombieEntity.createAttributes());

		// Où et quand le jeu a le droit de le faire apparaître (même emplacement que le zombie vanilla).
		SpawnRestriction.register(FROZEN_ZOMBIE, SpawnLocationTypes.ON_GROUND,
			Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, FrozenZombieEntity::canSpawn);
		SpawnRestriction.register(BLINDED_ZOMBIE, SpawnLocationTypes.ON_GROUND,
			Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, BlindedZombieEntity::canSpawn);
		SpawnRestriction.register(THROWER_ZOMBIE, SpawnLocationTypes.ON_GROUND,
			Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ThrowerZombieEntity::canSpawn);
		SpawnRestriction.register(RUNNER_ZOMBIE, SpawnLocationTypes.ON_GROUND,
			Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, RunnerZombieEntity::canSpawn);
		// Le sous-fifre n'apparaît jamais seul dans la nature (c'est le chef qui l'amène) : pas de règle de spawn naturel,
		// donc le jeu ne l'ajoutera à aucune liste ; l'œuf et /summon passent par la règle par défaut.
		SpawnRestriction.register(HORDE_CHIEF, SpawnLocationTypes.ON_GROUND,
			Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HordeChiefEntity::canSpawn);
	}
}
