package net.melvunx.zombieexpansion.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.melvunx.zombieexpansion.ZombieExpansion;
import net.melvunx.zombieexpansion.entity.BlindedZombieEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer GeckoLib du zombie aveugle. L'identifiant « the_blinded » (nom des fichiers exportés
 * depuis Blockbench) donne les trois chemins :
 *   - geo/entity/the_blinded.geo.json
 *   - textures/entity/the_blinded.png
 *   - animations/entity/the_blinded.animation.json
 */
@Environment(EnvType.CLIENT)
public class BlindedZombieRenderer extends GeoEntityRenderer<BlindedZombieEntity> {
	public BlindedZombieRenderer(EntityRendererFactory.Context context) {
		// true = le bone « head » suit le regard (inutile pour un aveugle mais cohérent avec le zombie gelé).
		super(context, new DefaultedEntityGeoModel<>(ZombieExpansion.id("the_blinded"), true));
	}
}
