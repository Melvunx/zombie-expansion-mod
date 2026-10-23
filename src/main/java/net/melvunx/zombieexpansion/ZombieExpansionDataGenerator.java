package net.melvunx.zombieexpansion;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryKeys;
import net.melvunx.zombieexpansion.damage.DifficultyDamage;
import net.melvunx.zombieexpansion.datagen.ModBiomeTagProvider;
import net.melvunx.zombieexpansion.datagen.ModDamageTypeProvider;
import net.melvunx.zombieexpansion.datagen.ModEnglishLangProvider;
import net.melvunx.zombieexpansion.datagen.ModEntityLootTableProvider;
import net.melvunx.zombieexpansion.datagen.ModEntityTypeTagProvider;
import net.melvunx.zombieexpansion.datagen.ModFrenchLangProvider;
import net.melvunx.zombieexpansion.datagen.ModModelProvider;

public class ZombieExpansionDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

		pack.addProvider(ModEnglishLangProvider::new);
		pack.addProvider(ModFrenchLangProvider::new);
		pack.addProvider(ModModelProvider::new);
		pack.addProvider(ModEntityLootTableProvider::new);
		pack.addProvider(ModBiomeTagProvider::new);
		pack.addProvider(ModEntityTypeTagProvider::new);
		pack.addProvider(ModDamageTypeProvider::new);
	}

	/** Déclare les entrées de registres dynamiques générées par ce mod (ici : les types de dégâts). */
	@Override
	public void buildRegistry(RegistryBuilder registryBuilder) {
		registryBuilder.addRegistry(RegistryKeys.DAMAGE_TYPE, DifficultyDamage::bootstrap);
	}
}
