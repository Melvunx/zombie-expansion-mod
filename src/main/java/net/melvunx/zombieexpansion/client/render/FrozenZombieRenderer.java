package net.melvunx.zombieexpansion.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.melvunx.zombieexpansion.ZombieExpansion;
import net.melvunx.zombieexpansion.entity.FrozenZombieEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
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

	/** Réduction du modèle pour un bébé (la hitbox est déjà réduite par le jeu, pas le modèle GeckoLib). */
	private static final float BABY_SCALE = 0.5f;

	@Override
	public void scaleModelForRender(float widthScale, float heightScale, MatrixStack poseStack, FrozenZombieEntity animatable,
									BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
		float babyScale = animatable.isBaby() ? BABY_SCALE : 1.0f;
		super.scaleModelForRender(widthScale * babyScale, heightScale * babyScale, poseStack, animatable, model,
			isReRender, partialTick, packedLight, packedOverlay);
	}

	/** Fait trembler le modèle pendant la conversion en zombie (comme le Stray qui se transforme en squelette). */
	@Override
	public boolean isShaking(FrozenZombieEntity animatable) {
		return animatable.isConvertingToZombie() || super.isShaking(animatable);
	}
}
