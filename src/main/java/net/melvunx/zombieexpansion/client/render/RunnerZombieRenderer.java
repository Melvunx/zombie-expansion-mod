package net.melvunx.zombieexpansion.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.melvunx.zombieexpansion.entity.RunnerZombieEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ZombieBaseEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.ZombieEntityModel;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Renderer provisoire du coureur : modèle et texture vanilla du zombie, aminci de 20 % en largeur pour
 * qu'on le reconnaisse comme « maigre ». Hérite de ZombieBaseEntityRenderer pour garder l'armure et les
 * objets tenus. Sera remplacé par un modèle Blockbench (la hitbox de l'EntityType ne change pas).
 */
@Environment(EnvType.CLIENT)
public class RunnerZombieRenderer extends ZombieBaseEntityRenderer<RunnerZombieEntity, ZombieEntityModel<RunnerZombieEntity>> {
	private static final float WIDTH_SCALE = 0.8f;

	public RunnerZombieRenderer(EntityRendererFactory.Context context) {
		super(context,
			new ZombieEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE)),
			new ZombieEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE_INNER_ARMOR)),
			new ZombieEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE_OUTER_ARMOR)));
	}

	@Override
	protected void scale(RunnerZombieEntity entity, MatrixStack matrices, float amount) {
		matrices.scale(WIDTH_SCALE, 1.0f, WIDTH_SCALE);
	}
}
