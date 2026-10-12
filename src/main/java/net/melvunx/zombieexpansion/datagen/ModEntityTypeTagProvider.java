package net.melvunx.zombieexpansion.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.melvunx.zombieexpansion.registry.ModEntityTypeTags;
import net.minecraft.entity.EntityType;
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

		// Une faction = un tag. Les futurs membres s'ajoutent ici (ou dans un datapack).
		getOrCreateTagBuilder(ModEntityTypeTags.FROST_FACTION).add(ModEntities.FROZEN_ZOMBIE);
		getOrCreateTagBuilder(ModEntityTypeTags.ABYSS_FACTION).add(ModEntities.BLINDED_ZOMBIE);
		getOrCreateTagBuilder(ModEntityTypeTags.VANILLA_ZOMBIE_FACTION)
			.add(EntityType.ZOMBIE)
			.add(EntityType.ZOMBIE_VILLAGER)
			.add(EntityType.HUSK)
			.add(EntityType.DROWNED);
	}
}
