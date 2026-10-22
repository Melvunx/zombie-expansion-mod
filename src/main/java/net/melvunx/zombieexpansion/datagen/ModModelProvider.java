package net.melvunx.zombieexpansion.datagen;

import java.util.Optional;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.melvunx.zombieexpansion.registry.ModItems;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.Model;
import net.minecraft.util.Identifier;

/** Génère les modèles d'items (assets/zombie_expansion/models/item/*.json). */
public class ModModelProvider extends FabricModelProvider {
	/** Les œufs d'apparition vanilla héritent tous de ce modèle, teinté par les deux couleurs de l'item. */
	private static final Model SPAWN_EGG = new Model(Optional.of(Identifier.ofVanilla("item/template_spawn_egg")), Optional.empty());

	public ModModelProvider(FabricDataOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockStateModelGenerator generator) {
		// Aucun bloc pour l'instant.
	}

	@Override
	public void generateItemModels(ItemModelGenerator generator) {
		generator.register(ModItems.FROZEN_ZOMBIE_SPAWN_EGG, SPAWN_EGG);
		generator.register(ModItems.BLINDED_ZOMBIE_SPAWN_EGG, SPAWN_EGG);
		generator.register(ModItems.THROWER_ZOMBIE_SPAWN_EGG, SPAWN_EGG);
	}
}
