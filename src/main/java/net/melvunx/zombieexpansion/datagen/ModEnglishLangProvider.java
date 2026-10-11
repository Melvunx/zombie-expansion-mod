package net.melvunx.zombieexpansion.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.melvunx.zombieexpansion.registry.ModItems;
import net.minecraft.registry.RegistryWrapper;

/** Génère assets/zombie_expansion/lang/en_us.json. À garder strictement parallèle à {@link ModFrenchLangProvider}. */
public class ModEnglishLangProvider extends FabricLanguageProvider {
	public ModEnglishLangProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
		super(dataOutput, "en_us", registryLookup);
	}

	@Override
	public void generateTranslations(RegistryWrapper.WrapperLookup registryLookup, TranslationBuilder builder) {
		builder.add(ModEntities.FROZEN_ZOMBIE, "Frozen Zombie");
		builder.add(ModEntities.BLINDED_ZOMBIE, "The Blinded");
		builder.add(ModItems.BLINDED_ZOMBIE_SPAWN_EGG, "The Blinded Spawn Egg");
		builder.add(ModItems.FROZEN_ZOMBIE_SPAWN_EGG, "Frozen Zombie Spawn Egg");

		builder.add("command.zombie_expansion.biome.title", "Biome: %s (%s), temperature %s");
		builder.add("command.zombie_expansion.biome.heat.hot", "Hot biome: Frost faction zombies are slowed by %s%% and turn back into zombies after %s s");
		builder.add("command.zombie_expansion.biome.region.frost", "Region: Frost faction, vanilla zombies are replaced on the surface");
		builder.add("command.zombie_expansion.biome.region.mixed", "Region: mixed, %s%% frozen zombies / %s%% vanilla zombies");
		builder.add("command.zombie_expansion.biome.region.none", "Region: not part of the Frost faction (vanilla spawns)");
		builder.add("command.zombie_expansion.biome.sky.visible", "Position: surface (sky visible)");
		builder.add("command.zombie_expansion.biome.sky.hidden", "Position: underground (no sky visible), vanilla zombies can spawn");
		builder.add("command.zombie_expansion.biome.spawn.entry", "Active spawn: %s, weight %s, group %s-%s");
		builder.add("command.zombie_expansion.biome.spawn.none", "No zombie spawn entry in this biome");
	}
}
