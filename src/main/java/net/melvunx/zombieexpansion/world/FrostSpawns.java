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
 * Ajoute le zombie gelé dans les biomes froids, à la place du zombie vanilla *en surface*.
 *
 * Le poids et la taille de groupe du zombie sont **recopiés depuis le biome lui-même**
 * (95 presque partout, 100 en Old Growth Pine Taiga) : on ne code aucun poids en dur.
 *
 * Le zombie vanilla **reste dans la liste** des biomes froids : c'est lui qui apparaît sous terre.
 * Le gelé exige le ciel visible (FrozenZombieEntity#canSpawn) et SpawnRestrictionMixin refuse le
 * vanilla en surface ; chaque position n'accepte donc qu'un des deux.
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
				(selection, modification) -> addFrozen(selection, modification, 1.0f));

		BiomeModifications.create(ZombieExpansion.id("frost_zombie_mix"))
			.add(ModificationPhase.REPLACEMENTS,
				BiomeSelectors.tag(ModBiomeTags.FROST_FACTION_MIXED_SPAWNS),
				(selection, modification) -> addFrozen(selection, modification, MIXED_FROST_SHARE));
	}

	/**
	 * Poids que garde le zombie vanilla dans un biome après nos modifications. Utile à qui veut
	 * se régler *sur* lui (voir AbyssSpawns) : en biome mixte il est réduit au profit du gelé.
	 */
	public static int vanillaZombieWeightAfterReplacement(BiomeSelectionContext selection, int originalWeight) {
		if (selection.hasTag(ModBiomeTags.FROST_FACTION_MIXED_SPAWNS)) {
			return originalWeight - Math.round(originalWeight * MIXED_FROST_SHARE);
		}
		return originalWeight;
	}

	private static void addFrozen(BiomeSelectionContext selection,
								  BiomeModificationContext modification,
								  float frostShare) {
		Optional<SpawnSettings.SpawnEntry> original = findZombie(selection);
		if (original.isEmpty()) {
			return; // biome sans zombie (ex. Deep Dark) : rien à remplacer
		}

		SpawnSettings.SpawnEntry zombie = original.get();
		int totalWeight = zombie.getWeight().getValue();
		int frostWeight = Math.round(totalWeight * frostShare);

		// Biome mixte : le vanilla cède une part de son poids au gelé. Biome entièrement froid : on ne
		// touche pas au vanilla, il reste le zombie des grottes.
		if (frostShare < 1.0f) {
			int vanillaWeight = totalWeight - frostWeight;
			modification.getSpawnSettings().removeSpawnsOfEntityType(EntityType.ZOMBIE);
			if (vanillaWeight > 0) {
				modification.getSpawnSettings().addSpawn(SpawnGroup.MONSTER,
					new SpawnSettings.SpawnEntry(EntityType.ZOMBIE, vanillaWeight, zombie.minGroupSize, zombie.maxGroupSize));
			}
		}
		modification.getSpawnSettings().addSpawn(SpawnGroup.MONSTER,
			new SpawnSettings.SpawnEntry(ModEntities.FROZEN_ZOMBIE, frostWeight, zombie.minGroupSize, zombie.maxGroupSize));
	}

	/** L'entrée du zombie vanilla dans un biome, telle que le jeu l'a avant toute modification. */
	static Optional<SpawnSettings.SpawnEntry> findZombie(BiomeSelectionContext selection) {
		return selection.getBiome().getSpawnSettings().getSpawnEntries(SpawnGroup.MONSTER).getEntries().stream()
			.filter(entry -> entry.type == EntityType.ZOMBIE)
			.findFirst();
	}
}
