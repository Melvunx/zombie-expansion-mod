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
		builder.add(ModItems.FROZEN_ZOMBIE_SPAWN_EGG, "Frozen Zombie Spawn Egg");
	}
}
