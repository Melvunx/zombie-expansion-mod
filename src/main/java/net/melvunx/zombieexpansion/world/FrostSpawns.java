package net.melvunx.zombieexpansion.world;

import java.util.Optional;

import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.melvunx.zombieexpansion.ZombieExpansion;
import net.melvunx.zombieexpansion.registry.ModBiomeTags;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.biome.SpawnSettings;

/**
 * Remplace le zombie vanilla par le zombie gelé dans les biomes froids.
 *
 * Le poids et la taille de groupe du zombie sont **recopiés depuis le biome lui-même**
 * (95 presque partout, 100 en Old Growth Pine Taiga) : on ne code aucun poids en dur,
 * donc le remplacement est neutre pour l'équilibre global des monstres.
 *
 * Le zombie villageois n'est volontairement pas touché.
 */
public final class FrostSpawns {
	/** Part du poids d'origine donnée au zombie gelé dans les biomes mixtes (70 % gelé / 30 % vanilla). */
	public static final float MIXED_FROST_SHARE = 0.7f;

	private FrostSpawns() {
	}

	public static void register() {
		BiomeModifications.create(ZombieExpansion.id("frost_zombie_replacement"))
			.add(ModificationPhase.REPLACEMENTS,
				BiomeSelectors.tag(ModBiomeTags.FROST_FACTION_SPAWNS),
				(selection, modification) -> swap(selection, modification, 1.0f));

		BiomeModifications.create(ZombieExpansion.id("frost_zombie_mix"))
			.add(ModificationPhase.REPLACEMENTS,
				BiomeSelectors.tag(ModBiomeTags.FROST_FACTION_MIXED_SPAWNS),
				(selection, modification) -> swap(selection, modification, MIXED_FROST_SHARE));
	}

	private static void swap(BiomeSelectionContext selection,
							 BiomeModificationContext modification,
							 float frostShare) {
		Optional<SpawnSettings.SpawnEntry> original = findZombie(selection);
		if (original.isEmpty()) {
			return; // biome sans zombie (ex. Deep Dark) : rien à remplacer
		}

		SpawnSettings.SpawnEntry zombie = original.get();
		int totalWeight = zombie.getWeight().getValue();
		int frostWeight = Math.round(totalWeight * frostShare);
		int vanillaWeight = totalWeight - frostWeight;

		modification.getSpawnSettings().removeSpawnsOfEntityType(EntityType.ZOMBIE);
		if (vanillaWeight > 0) {
			modification.getSpawnSettings().addSpawn(SpawnGroup.MONSTER,
				new SpawnSettings.SpawnEntry(EntityType.ZOMBIE, vanillaWeight, zombie.minGroupSize, zombie.maxGroupSize));
		}
		modification.getSpawnSettings().addSpawn(SpawnGroup.MONSTER,
			new SpawnSettings.SpawnEntry(ModEntities.FROZEN_ZOMBIE, frostWeight, zombie.minGroupSize, zombie.maxGroupSize));
	}

	private static Optional<SpawnSettings.SpawnEntry> findZombie(BiomeSelectionContext selection) {
		return selection.getBiome().getSpawnSettings().getSpawnEntries(SpawnGroup.MONSTER).getEntries().stream()
			.filter(entry -> entry.type == EntityType.ZOMBIE)
			.findFirst();
	}
}
