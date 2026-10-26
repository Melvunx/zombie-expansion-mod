package net.melvunx.zombieexpansion.client.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.melvunx.zombieexpansion.ZombieExpansion;
import net.melvunx.zombieexpansion.entity.HordeMinionEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modèle GeckoLib du sous-fifre : un seul modèle et un seul fichier d'animations, mais une texture par
 * variante (horde_minion_dopey.png, horde_minion_cross_eyed.png, horde_minion_toothy.png). Les animations
 * de chaque variante sont choisies par l'entité elle-même (voir HordeMinionVariant).
 */
@Environment(EnvType.CLIENT)
public class HordeMinionModel extends DefaultedEntityGeoModel<HordeMinionEntity> {
	public HordeMinionModel() {
		super(ZombieExpansion.id("unearthed/horde_minion"), true);
	}

	@Override
	public Identifier getTextureResource(HordeMinionEntity entity) {
		return ZombieExpansion.id("textures/entity/unearthed/horde_minion_" + entity.getVariant().id() + ".png");
	}
}
