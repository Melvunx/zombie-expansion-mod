package net.melvunx.zombieexpansion.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.melvunx.zombieexpansion.client.model.ThrowerZombieModel;
import net.melvunx.zombieexpansion.entity.ThrowerZombieEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ZombieBaseEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;

/**
 * Renderer provisoire du zombie lanceur : modèle et texture vanilla du zombie, avec le bras droit
 * masqué après le tir ({@link ThrowerZombieModel}). Hérite de ZombieBaseEntityRenderer pour garder
 * le calque d'armure et le tremblement de conversion. Sera remplacé par un modèle Blockbench.
 */
@Environment(EnvType.CLIENT)
public class ThrowerZombieRenderer extends ZombieBaseEntityRenderer<ThrowerZombieEntity, ThrowerZombieModel> {
	public ThrowerZombieRenderer(EntityRendererFactory.Context context) {
		super(context,
			new ThrowerZombieModel(context.getPart(EntityModelLayers.ZOMBIE)),
			new ThrowerZombieModel(context.getPart(EntityModelLayers.ZOMBIE_INNER_ARMOR)),
			new ThrowerZombieModel(context.getPart(EntityModelLayers.ZOMBIE_OUTER_ARMOR)));
	}
}
