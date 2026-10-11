package net.melvunx.zombieexpansion.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.melvunx.zombieexpansion.registry.ModBiomeTags;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

/** Génère data/zombie_expansion/tags/worldgen/biome/*.json. */
public class ModBiomeTagProvider extends FabricTagProvider<Biome> {
	public ModBiomeTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
		super(output, RegistryKeys.BIOME, registriesFuture);
	}

	@Override
	protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
		getOrCreateTagBuilder(ModBiomeTags.FROST_FACTION_SPAWNS)
			.add(BiomeKeys.SNOWY_PLAINS)
			.add(BiomeKeys.ICE_SPIKES)
			.add(BiomeKeys.SNOWY_TAIGA)
			.add(BiomeKeys.SNOWY_BEACH)
			.add(BiomeKeys.GROVE)
			.add(BiomeKeys.SNOWY_SLOPES)
			.add(BiomeKeys.JAGGED_PEAKS)
			.add(BiomeKeys.FROZEN_PEAKS)
			.add(BiomeKeys.WINDSWEPT_HILLS)
			.add(BiomeKeys.WINDSWEPT_GRAVELLY_HILLS)
			.add(BiomeKeys.TAIGA)
			.add(BiomeKeys.OLD_GROWTH_PINE_TAIGA)
			.add(BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA)
			.add(BiomeKeys.FROZEN_RIVER)
			.add(BiomeKeys.FROZEN_OCEAN)
			.add(BiomeKeys.DEEP_FROZEN_OCEAN)
			.add(BiomeKeys.COLD_OCEAN);

		// Tags vanilla réutilisés (forceAddTag : la datagen ne connaît pas les tags vanilla et refuserait addTag).
		// Ils couvrent aussi les variantes (eroded/wooded badlands, savane venteuse).
		getOrCreateTagBuilder(ModBiomeTags.HOT_BIOMES)
			.add(BiomeKeys.DESERT)
			.forceAddTag(BiomeTags.IS_BADLANDS)
			.forceAddTag(BiomeTags.IS_SAVANNA)
			.forceAddTag(BiomeTags.IS_NETHER);

		getOrCreateTagBuilder(ModBiomeTags.FROST_FACTION_MIXED_SPAWNS)
			.add(BiomeKeys.WINDSWEPT_FOREST);
	}
}
