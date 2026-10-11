package net.melvunx.zombieexpansion.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.melvunx.zombieexpansion.ZombieExpansion;
import net.melvunx.zombieexpansion.entity.FrozenZombieEntity;
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

	private ModEntities() {
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		return Registry.register(Registries.ENTITY_TYPE, ZombieExpansion.id(name), builder.build(name));
	}

	/** Appelé depuis {@link ZombieExpansion#onInitialize()} : force le chargement de la classe (donc des champs statiques). */
	public static void register() {
		// Sans attributs par défaut, le jeu plante au spawn de toute entité vivante.
		FabricDefaultAttributeRegistry.register(FROZEN_ZOMBIE, ZombieEntity.createZombieAttributes());

		// Où et quand le jeu a le droit de le faire apparaître (même emplacement que le zombie vanilla).
		SpawnRestriction.register(FROZEN_ZOMBIE, SpawnLocationTypes.ON_GROUND,
			Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, FrozenZombieEntity::canSpawn);
	}
}
