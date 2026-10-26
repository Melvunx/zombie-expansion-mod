package net.melvunx.zombieexpansion.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.melvunx.zombieexpansion.client.model.HordeMinionModel;
import net.melvunx.zombieexpansion.entity.HordeMinionEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Renderer GeckoLib du sous-fifre : 15 % plus petit que le zombie vanilla (la hitbox de l'EntityType suit).
 * Le calque d'objet tenu dessine l'arme de la main principale (l'épée en bois) sur l'os « right_hand_item »
 * du modèle, qui suit donc le bras droit pendant les animations.
 */
@Environment(EnvType.CLIENT)
public class HordeMinionRenderer extends GeoEntityRenderer<HordeMinionEntity> {
	private static final float SCALE = 0.85f;
	private static final String HAND_BONE = "right_hand_item";

	public HordeMinionRenderer(EntityRendererFactory.Context context) {
		super(context, new HordeMinionModel());
		withScale(SCALE);
		addRenderLayer(new BlockAndItemGeoLayer<>(this) {
			@Override
			protected ItemStack getStackForBone(GeoBone bone, HordeMinionEntity entity) {
				return HAND_BONE.equals(bone.getName()) ? entity.getMainHandStack() : null;
			}

			@Override
			protected ModelTransformationMode getTransformTypeForStack(GeoBone bone, ItemStack stack, HordeMinionEntity entity) {
				return ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
			}
		});
	}
}
