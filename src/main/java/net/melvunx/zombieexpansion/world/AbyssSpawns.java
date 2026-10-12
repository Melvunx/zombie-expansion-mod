package net.melvunx.zombieexpansion.world;

import java.util.Optional;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.melvunx.zombieexpansion.ZombieExpansion;
import net.melvunx.zombieexpansion.registry.ModBiomeTags;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.biome.SpawnSettings;

/**
 * Spawns de la faction des Abysses.
 *
 * Le zombie aveugle suit la logique de l'Enderman : originaire d'un biome précis (le Deep Dark),
 * mais présent partout où les conditions sont réunies (ici : sous Y -8) dans tout l'Overworld.
 * La limite d'altitude est dans {@code BlindedZombieEntity#canSpawn}, car un BiomeModifications
 * ne connaît pas l'altitude.
 *
 * Deux entrées :
 *  - Deep Dark : aucun monstre en vanilla, donc l'aveugle y est seul et son poids est sans effet
 *    relatif ; c'est le plafond de densité (canSpawn) qui borne sa fréquence ;
 *  - partout ailleurs : son poids est réglé pour qu'il représente {@link #BLINDED_SHARE} des zombies
 *    sous terre, quel que soit le biome (80 % zombie vanilla / 20 % aveugle).
 *
 * Limite connue : les Ancient Cities interdisent tout spawn de monstre sur leur boîte englobante
 * (spawn_overrides vanilla), donc rien n'y apparaît, même dans le biome Deep Dark.
 */
public final class AbyssSpawns {
	/** Part des zombies (vanilla + aveugle) occupée par l'aveugle sous Y -8. */
	private static final float BLINDED_SHARE = 0.20f;

	private static final int DEEP_DARK_WEIGHT = 20;
	/** Poids de repli si un biome a des monstres mais pas de zombie (modpack, datapack). */
	private static final int FALLBACK_WEIGHT = 4;
	private static final int MIN_GROUP = 2;
	private static final int MAX_GROUP = 3;

	private AbyssSpawns() {
	}

	public static void register() {
		BiomeModifications.addSpawn(
			BiomeSelectors.tag(ModBiomeTags.ABYSS_FACTION_SPAWNS),
			SpawnGroup.MONSTER, ModEntities.BLINDED_ZOMBIE, DEEP_DARK_WEIGHT, MIN_GROUP, MAX_GROUP);

		// Biomes de l'Overworld qui ont déjà des monstres : on respecte ainsi les biomes volontairement
		// sans monstres (champignons) et ceux d'autres mods ou datapacks.
		BiomeModifications.create(ZombieExpansion.id("blinded_zombie_underground"))
			.add(ModificationPhase.ADDITIONS,
				BiomeSelectors.foundInOverworld()
					.and(BiomeSelectors.tag(ModBiomeTags.ABYSS_FACTION_SPAWNS).negate())
					.and(context -> !context.getBiome().getSpawnSettings().getSpawnEntries(SpawnGroup.MONSTER).isEmpty()),
				(selection, modification) -> modification.getSpawnSettings().addSpawn(SpawnGroup.MONSTER,
					new SpawnSettings.SpawnEntry(ModEntities.BLINDED_ZOMBIE, blindedWeight(selection), MIN_GROUP, MAX_GROUP)));
	}

	/**
	 * Poids pour que aveugle / (vanilla + aveugle) = BLINDED_SHARE : w = vanilla × share / (1 - share).
	 * Le poids du vanilla est celui qui reste *après* FrostSpawns (réduit en biome mixte).
	 */
	private static int blindedWeight(BiomeSelectionContext selection) {
		Optional<SpawnSettings.SpawnEntry> zombie = FrostSpawns.findZombie(selection);
		if (zombie.isEmpty()) {
			return FALLBACK_WEIGHT;
		}
		int vanilla = FrostSpawns.vanillaZombieWeightAfterReplacement(selection, zombie.get().getWeight().getValue());
		return Math.max(1, Math.round(vanilla * BLINDED_SHARE / (1.0f - BLINDED_SHARE)));
	}
}
