package net.melvunx.zombieexpansion.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.melvunx.zombieexpansion.registry.ModItems;
import net.minecraft.registry.RegistryWrapper;

/** Génère assets/zombie_expansion/lang/fr_fr.json. */
public class ModFrenchLangProvider extends FabricLanguageProvider {
	public ModFrenchLangProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
		super(dataOutput, "fr_fr", registryLookup);
	}

	@Override
	public void generateTranslations(RegistryWrapper.WrapperLookup registryLookup, TranslationBuilder builder) {
		builder.add(ModEntities.FROZEN_ZOMBIE, "Zombie gelé");
		builder.add(ModItems.FROZEN_ZOMBIE_SPAWN_EGG, "Œuf d'apparition de zombie gelé");

		builder.add("command.zombie_expansion.biome.title", "Biome : %s (%s), température %s");
		builder.add("command.zombie_expansion.biome.region.frost", "Région : faction Givre, les zombies vanilla sont remplacés en surface");
		builder.add("command.zombie_expansion.biome.region.mixed", "Région : mixte, %s %% de zombies gelés / %s %% de zombies vanilla");
		builder.add("command.zombie_expansion.biome.region.none", "Région : hors faction Givre (spawns vanilla)");
		builder.add("command.zombie_expansion.biome.sky.visible", "Position : surface (ciel visible)");
		builder.add("command.zombie_expansion.biome.sky.hidden", "Position : sous terre (ciel non visible), les zombies vanilla peuvent apparaître");
		builder.add("command.zombie_expansion.biome.spawn.entry", "Spawn actif : %s, poids %s, groupe %s-%s");
		builder.add("command.zombie_expansion.biome.spawn.none", "Aucune entrée de zombie dans ce biome");
	}
}
