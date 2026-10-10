package net.melvunx.zombieexpansion.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.melvunx.zombieexpansion.ZombieExpansion;
import net.melvunx.zombieexpansion.entity.FrozenZombieEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer GeckoLib du zombie gelé.
 *
 * {@link DefaultedEntityGeoModel} déduit trois chemins à partir d'un seul identifiant
 * ("zombie_expansion:frozen_zombie") :
 *   - geo/entity/frozen_zombie.geo.json
 *   - textures/entity/frozen_zombie.png
 *   - animations/entity/frozen_zombie.animation.json
 */
@Environment(EnvType.CLIENT)
public class FrozenZombieRenderer extends GeoEntityRenderer<FrozenZombieEntity> {
	public FrozenZombieRenderer(EntityRendererFactory.Context context) {
		// true = le bone nommé "head" suit la direction du regard de l'entité.
		super(context, new DefaultedEntityGeoModel<>(ZombieExpansion.id("frozen_zombie"), true));
	}
}
