package net.melvunx.zombieexpansion.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.melvunx.zombieexpansion.ZombieExpansion;
import net.melvunx.zombieexpansion.entity.ThrowerZombieEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer GeckoLib du zombie lanceur. L'identifiant « thrower_zombie » donne les trois chemins :
 * geo/entity/thrower_zombie.geo.json, textures/entity/thrower_zombie.png et
 * animations/entity/thrower_zombie.animation.json.
 *
 * Limite : pas de calque d'armure ni d'objet tenu (le modèle n'a pas les os attendus par les calques
 * GeckoLib) ; un zombie lanceur qui ramasse un équipement ne l'affiche donc pas.
 */
@Environment(EnvType.CLIENT)
public class ThrowerZombieRenderer extends GeoEntityRenderer<ThrowerZombieEntity> {
	private static final String ARM_BONE = "right_arm";

	public ThrowerZombieRenderer(EntityRendererFactory.Context context) {
		super(context, new DefaultedEntityGeoModel<>(ZombieExpansion.id("thrower_zombie"), true));
	}

	/**
	 * Masque l'os du bras tant qu'il est lancé. Le modèle est partagé par toutes les instances : on
	 * règle donc la visibilité avant chaque rendu, d'après l'état de l'entité dessinée.
	 */
	@Override
	public void preRender(MatrixStack poseStack, ThrowerZombieEntity animatable, BakedGeoModel model, VertexConsumerProvider bufferSource,
						  VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
		super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
		model.getBone(ARM_BONE).ifPresent(bone -> bone.setHidden(animatable.isArmThrown()));
	}
}
