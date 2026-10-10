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
	}
}
