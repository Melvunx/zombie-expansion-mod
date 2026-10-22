package net.melvunx.zombieexpansion.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.melvunx.zombieexpansion.ZombieExpansion;
import net.melvunx.zombieexpansion.registry.ModStatusEffects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;

/**
 * Bave verte à l'écran tant que le joueur a l'effet Englué.
 *
 * Un effet de statut ne dessine rien lui-même, et le jeu envoie déjà au client les effets du joueur :
 * il suffit donc de lire {@code player.getStatusEffect} dans un callback de HUD. Cette façon de faire
 * est propre à Fabric ({@link HudRenderCallback}) ; sur NeoForge, ce serait un événement de couche d'interface.
 *
 * Même technique que le contour de neige poudreuse vanilla : une texture étirée sur tout l'écran,
 * dont l'opacité varie.
 */
@Environment(EnvType.CLIENT)
public final class GluedOverlay {
	private static final Identifier TEXTURE = ZombieExpansion.id("textures/misc/glued_overlay.png");
	/** L'overlay s'estompe sur les 20 derniers ticks de l'effet (1 s) au lieu de disparaître d'un coup. */
	private static final float FADE_TICKS = 20.0f;

	private GluedOverlay() {
	}

	public static void register() {
		HudRenderCallback.EVENT.register(GluedOverlay::render);
	}

	private static void render(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.options.hudHidden) {
			return;
		}
		StatusEffectInstance glued = client.player.getStatusEffect(ModStatusEffects.GLUED);
		if (glued == null) {
			return;
		}
		float alpha = Math.min(1.0f, glued.getDuration() / FADE_TICKS);

		int width = context.getScaledWindowWidth();
		int height = context.getScaledWindowHeight();
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		context.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
		context.drawTexture(TEXTURE, 0, 0, -90, 0.0f, 0.0f, width, height, width, height);
		context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		RenderSystem.disableBlend();
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
	}
}
