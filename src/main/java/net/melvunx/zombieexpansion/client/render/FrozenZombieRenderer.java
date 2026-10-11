package net.melvunx.zombieexpansion.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.melvunx.zombieexpansion.client.model.FrozenZombieModel;
import net.melvunx.zombieexpansion.entity.FrozenZombieEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer GeckoLib du zombie gelé. Les chemins du modèle, de la texture et des animations
 * sont déduits par {@link FrozenZombieModel} (geo/entity, textures/entity, animations/entity).
 */
@Environment(EnvType.CLIENT)
public class FrozenZombieRenderer extends GeoEntityRenderer<FrozenZombieEntity> {
	public FrozenZombieRenderer(EntityRendererFactory.Context context) {
		super(context, new FrozenZombieModel());
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
