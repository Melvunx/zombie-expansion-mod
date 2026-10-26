package net.melvunx.zombieexpansion.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.melvunx.zombieexpansion.ZombieExpansion;
import net.melvunx.zombieexpansion.entity.HordeChiefEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer GeckoLib du chef de horde. L'identifiant « horde_chief » donne les trois chemins :
 * geo/entity/unearthed/horde_chief.geo.json, textures/entity/unearthed/horde_chief.png et
 * animations/entity/unearthed/horde_chief.animation.json.
 *
 * Le modèle est dessiné aux proportions vanilla ; l'échelle de {@link #SCALE} (10 % de plus que le zombie)
 * est appliquée ici, et la hitbox de l'EntityType (0,66 x 2,15) est calculée avec le même facteur.
 * Limite : pas de calque d'armure ni d'objet tenu (le chef n'a aucun équipement).
 */
@Environment(EnvType.CLIENT)
public class HordeChiefRenderer extends GeoEntityRenderer<HordeChiefEntity> {
	private static final float SCALE = 1.1f;

	public HordeChiefRenderer(EntityRendererFactory.Context context) {
		super(context, new DefaultedEntityGeoModel<>(ZombieExpansion.id("unearthed/horde_chief"), true));
		withScale(SCALE);
	}
}
