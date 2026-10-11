package net.melvunx.zombieexpansion.world;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.biome.BiomeKeys;

/**
 * Spawns de la faction des Abysses.
 *
 * Contrairement à la faction Givre, rien n'est *remplacé* : le Deep Dark n'a aucune entrée de
 * monstre en vanilla, on en ajoute donc une. La limite d'altitude est dans
 * {@code BlindedZombieEntity#canSpawn}, car un BiomeModifications ne connaît pas l'altitude.
 */
public final class AbyssSpawns {
	/** Poids 10, groupes de 2 à 4 : en groupe ils sont dangereux, c'est voulu. À ajuster en jeu. */
	private static final int WEIGHT = 10;
	private static final int MIN_GROUP = 2;
	private static final int MAX_GROUP = 4;

	private AbyssSpawns() {
	}

	public static void register() {
		BiomeModifications.addSpawn(
			BiomeSelectors.includeByKey(BiomeKeys.DEEP_DARK),
			SpawnGroup.MONSTER, ModEntities.BLINDED_ZOMBIE, WEIGHT, MIN_GROUP, MAX_GROUP);
	}
}
