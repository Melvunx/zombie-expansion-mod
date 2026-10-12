package net.melvunx.zombieexpansion.world;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.melvunx.zombieexpansion.registry.ModBiomeTags;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.minecraft.entity.SpawnGroup;

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
 *  - partout ailleurs : poids bas, pour qu'il soit rare à côté des 500 de poids des monstres vanilla.
 */
public final class AbyssSpawns {
	private static final int DEEP_DARK_WEIGHT = 20;
	private static final int ELSEWHERE_WEIGHT = 4;
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
		BiomeModifications.addSpawn(
			BiomeSelectors.foundInOverworld()
				.and(BiomeSelectors.tag(ModBiomeTags.ABYSS_FACTION_SPAWNS).negate())
				.and(context -> !context.getBiome().getSpawnSettings().getSpawnEntries(SpawnGroup.MONSTER).isEmpty()),
			SpawnGroup.MONSTER, ModEntities.BLINDED_ZOMBIE, ELSEWHERE_WEIGHT, MIN_GROUP, MAX_GROUP);
	}
}
