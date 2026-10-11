package net.melvunx.zombieexpansion.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.EntityTypeTags;

/**
 * Génère les tags d'entités. Ici on *ajoute* nos entités à des tags vanilla
 * (data/minecraft/tags/entity_type/...) sans remplacer leur contenu.
 */
public class ModEntityTypeTagProvider extends FabricTagProvider.EntityTypeTagProvider {
	public ModEntityTypeTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
		super(output, registriesFuture);
	}

	@Override
	protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
		// Insensible au gel : sinon un zombie qui vient d'être transformé dans la neige poudreuse
		// y subirait aussitôt les dégâts de gel.
		getOrCreateTagBuilder(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES).add(ModEntities.FROZEN_ZOMBIE);
	}
}
