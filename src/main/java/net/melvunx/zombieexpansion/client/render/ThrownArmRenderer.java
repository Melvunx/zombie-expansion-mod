package net.melvunx.zombieexpansion.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.melvunx.zombieexpansion.ZombieExpansion;
import net.melvunx.zombieexpansion.entity.ThrownArmEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer GeckoLib du bras lancé (thrown_arm.*). Il tournoie autour de l'axe X (animation spin) :
 * pour que ce soit un culbutage dans le sens du vol, il faut orienter le modèle selon le lacet du projectile.
 */
@Environment(EnvType.CLIENT)
public class ThrownArmRenderer extends GeoEntityRenderer<ThrownArmEntity> {
	public ThrownArmRenderer(EntityRendererFactory.Context context) {
		super(context, new DefaultedEntityGeoModel<>(ZombieExpansion.id("thrown_arm")));
	}

	/**
	 * GeckoLib n'oriente que les créatures (via leur lacet de corps) : pour tout autre entité il passe 0.
	 * On lui donne le lacet interpolé du projectile. Le tangage est inutile : l'axe de rotation du spin
	 * est X, donc une rotation en X ne changerait rien de visible.
	 */
	@Override
	protected void applyRotations(ThrownArmEntity animatable, MatrixStack poseStack, float ageInTicks, float rotationYaw,
								  float partialTick, float nativeScale) {
		float yaw = MathHelper.lerpAngleDegrees(partialTick, animatable.prevYaw, animatable.getYaw());
		super.applyRotations(animatable, poseStack, ageInTicks, yaw, partialTick, nativeScale);
	}
}
