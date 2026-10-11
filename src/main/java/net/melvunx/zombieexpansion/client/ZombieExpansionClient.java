package net.melvunx.zombieexpansion.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.melvunx.zombieexpansion.client.render.FrozenZombieRenderer;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.minecraft.client.render.entity.ZombieEntityRenderer;

/**
 * Point d'entrée client : n'est chargé que sur le client (jamais sur un serveur
 * dédié), c'est donc ici qu'on peut toucher au rendu sans faire planter le serveur.
 */
@Environment(EnvType.CLIENT)
public class ZombieExpansionClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.FROZEN_ZOMBIE, FrozenZombieRenderer::new);

		// Rendu PROVISOIRE : modèle et texture du zombie vanilla, en attendant le bbmodel du zombie aveugle.
		EntityRendererRegistry.register(ModEntities.BLINDED_ZOMBIE, ZombieEntityRenderer::new);
	}
}
