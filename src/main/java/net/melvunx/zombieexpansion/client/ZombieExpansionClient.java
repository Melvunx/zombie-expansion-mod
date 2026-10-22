package net.melvunx.zombieexpansion.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.melvunx.zombieexpansion.client.render.BlindedZombieRenderer;
import net.melvunx.zombieexpansion.client.render.FrozenZombieRenderer;
import net.melvunx.zombieexpansion.client.render.ThrowerZombieRenderer;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

/**
 * Point d'entrée client : n'est chargé que sur le client (jamais sur un serveur
 * dédié), c'est donc ici qu'on peut toucher au rendu sans faire planter le serveur.
 */
@Environment(EnvType.CLIENT)
public class ZombieExpansionClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.FROZEN_ZOMBIE, FrozenZombieRenderer::new);
		EntityRendererRegistry.register(ModEntities.BLINDED_ZOMBIE, BlindedZombieRenderer::new);
		EntityRendererRegistry.register(ModEntities.THROWER_ZOMBIE, ThrowerZombieRenderer::new);
		// Projectile rendu comme un objet en vol (sprite de chair putréfiée, en attendant un modèle dédié).
		EntityRendererRegistry.register(ModEntities.THROWN_ARM, FlyingItemEntityRenderer::new);
	}
}
