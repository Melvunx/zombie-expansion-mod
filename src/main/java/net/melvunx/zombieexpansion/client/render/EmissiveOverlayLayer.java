package net.melvunx.zombieexpansion.client.render;

import java.util.function.Function;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Calque lumineux : redessine le modèle avec une texture « de lueur » (seuls les pixels qui brillent sont
 * peints, le reste est transparent) à pleine luminosité, par-dessus le rendu normal.
 *
 * On n'utilise pas {@code AutoGlowingGeoLayer} de GeckoLib : il découpe les pixels lumineux dans la texture
 * de base (ils y deviennent transparents). Ça convient à une lueur permanente, mais pas à celle du
 * sous-fifre, qui ne s'allume que pendant son boost : ses tatouages auraient disparu le reste du temps.
 * Ici la texture de base reste intacte, la lueur s'ajoute simplement dessus (rendu « yeux » : additif).
 *
 * L'intensité (0 à 1) est demandée à chaque image : 1 = lueur pleine, 0 = éteinte (rien n'est dessiné). Elle
 * multiplie la couleur du calque, donc la lueur s'atténue de façon linéaire quand l'intensité baisse.
 *
 * @param <T> l'entité dessinée
 */
@Environment(EnvType.CLIENT)
public class EmissiveOverlayLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
	/** Intensité de la lueur d'une entité à un instant donné (0 à 1). */
	@FunctionalInterface
	public interface Intensity<T> {
		float get(T entity, float partialTick);
	}

	private final Intensity<T> intensity;
	private final Function<T, Identifier> glowTexture;

	public EmissiveOverlayLayer(GeoRenderer<T> renderer, Intensity<T> intensity, Function<T, Identifier> glowTexture) {
		super(renderer);
		this.intensity = intensity;
		this.glowTexture = glowTexture;
	}

	@Override
	public void render(MatrixStack poseStack, T animatable, BakedGeoModel bakedModel, RenderLayer renderType,
					   VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		float strength = intensity.get(animatable, partialTick);
		if (strength <= 0.0f) {
			return;
		}
		RenderLayer glowLayer = RenderLayer.getEyes(glowTexture.apply(animatable));
		getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glowLayer, bufferSource.getBuffer(glowLayer), partialTick,
			LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV,
			scaleColor(getRenderer().getRenderColor(animatable, partialTick, packedLight).argbInt(), strength));
	}

	/** Multiplie les canaux de la couleur : le rendu « yeux » étant additif, une couleur plus sombre donne une lueur plus faible. */
	private static int scaleColor(int argb, float factor) {
		return ColorHelper.Argb.getArgb(
			ColorHelper.Argb.getAlpha(argb),
			Math.round(ColorHelper.Argb.getRed(argb) * factor),
			Math.round(ColorHelper.Argb.getGreen(argb) * factor),
			Math.round(ColorHelper.Argb.getBlue(argb) * factor));
	}
}
