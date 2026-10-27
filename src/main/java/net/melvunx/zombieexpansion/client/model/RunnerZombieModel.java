package net.melvunx.zombieexpansion.client.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.melvunx.zombieexpansion.ZombieExpansion;
import net.melvunx.zombieexpansion.entity.RunnerZombieEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modèle GeckoLib du coureur : un modèle, un fichier d'animations, deux textures. Le renderer ne peut pas
 * choisir la texture seul : c'est le modèle qui la fournit, selon la variante de l'entité (surface ou grottes).
 */
@Environment(EnvType.CLIENT)
public class RunnerZombieModel extends DefaultedEntityGeoModel<RunnerZombieEntity> {
	private static final Identifier SURFACE = ZombieExpansion.id("textures/entity/unearthed/runner_zombie.png");
	private static final Identifier CAVE = ZombieExpansion.id("textures/entity/unearthed/runner_zombie_cave.png");

	public RunnerZombieModel() {
		super(ZombieExpansion.id("unearthed/runner_zombie"), true);
	}

	@Override
	public Identifier getTextureResource(RunnerZombieEntity entity) {
		return entity.isCaveVariant() ? CAVE : SURFACE;
	}
}
