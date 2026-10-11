package net.melvunx.zombieexpansion.registry;

import net.melvunx.zombieexpansion.ZombieExpansion;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.biome.Biome;

/**
 * Tags de biomes du mod. Un tag est une liste de biomes définie en JSON de datapack
 * (générée ici par {@code ModBiomeTagProvider}) : un datapack ou un autre mod peut
 * donc y ajouter ses propres biomes sans toucher au code.
 */
public final class ModBiomeTags {
	/** Biomes où le zombie gelé remplace entièrement le zombie vanilla en surface. */
	public static final TagKey<Biome> FROST_FACTION_SPAWNS = of("frost_faction_spawns");

	/** Biomes mixtes : zombies vanilla et gelés cohabitent (voir FrostSpawns.MIXED_FROST_SHARE). */
	public static final TagKey<Biome> FROST_FACTION_MIXED_SPAWNS = of("frost_faction_mixed_spawns");

	private ModBiomeTags() {
	}

	private static TagKey<Biome> of(String name) {
		return TagKey.of(RegistryKeys.BIOME, ZombieExpansion.id(name));
	}
}
