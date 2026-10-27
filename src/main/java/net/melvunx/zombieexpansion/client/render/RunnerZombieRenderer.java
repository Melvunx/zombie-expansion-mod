package net.melvunx.zombieexpansion.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.melvunx.zombieexpansion.client.model.RunnerZombieModel;
import net.melvunx.zombieexpansion.entity.RunnerZombieEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Renderer GeckoLib du coureur, à l'échelle du modèle (hitbox vanilla de l'EntityType). Deux calques :
 * l'armure ({@link ZombieArmorGeoLayer}, os par os) et l'objet tenu en main principale, dessiné sur l'os
 * {@code right_hand_item} qui suit le bras droit pendant les animations. La texture (surface ou grottes)
 * vient de {@link RunnerZombieModel}.
 */
@Environment(EnvType.CLIENT)
public class RunnerZombieRenderer extends GeoEntityRenderer<RunnerZombieEntity> {
	private static final String HAND_BONE = "right_hand_item";

	public RunnerZombieRenderer(EntityRendererFactory.Context context) {
		super(context, new RunnerZombieModel());
		addRenderLayer(new ZombieArmorGeoLayer<>(this));
		addRenderLayer(new BlockAndItemGeoLayer<>(this) {
			@Override
			protected ItemStack getStackForBone(GeoBone bone, RunnerZombieEntity entity) {
				return HAND_BONE.equals(bone.getName()) ? entity.getMainHandStack() : null;
			}

			@Override
			protected ModelTransformationMode getTransformTypeForStack(GeoBone bone, ItemStack stack, RunnerZombieEntity entity) {
				return ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
			}
		});
	}
}
