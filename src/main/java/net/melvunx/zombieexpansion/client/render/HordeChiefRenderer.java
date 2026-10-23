package net.melvunx.zombieexpansion.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.melvunx.zombieexpansion.entity.HordeChiefEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ZombieBaseEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.ZombieEntityModel;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Renderer provisoire du chef de horde : modèle et texture vanilla du zombie, agrandi de 15 % pour qu'on
 * le distingue de sa troupe. Sera remplacé par un modèle Blockbench.
 */
@Environment(EnvType.CLIENT)
public class HordeChiefRenderer extends ZombieBaseEntityRenderer<HordeChiefEntity, ZombieEntityModel<HordeChiefEntity>> {
	private static final float SCALE = 1.15f;

	public HordeChiefRenderer(EntityRendererFactory.Context context) {
		super(context,
			new ZombieEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE)),
			new ZombieEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE_INNER_ARMOR)),
			new ZombieEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE_OUTER_ARMOR)));
	}

	@Override
	protected void scale(HordeChiefEntity entity, MatrixStack matrices, float amount) {
		matrices.scale(SCALE, SCALE, SCALE);
	}
}
