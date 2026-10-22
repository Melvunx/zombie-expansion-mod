package net.melvunx.zombieexpansion.client.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.melvunx.zombieexpansion.entity.ThrowerZombieEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.ZombieEntityModel;

/**
 * Modèle vanilla du zombie, dont le bras droit disparaît quand il vient d'être lancé.
 *
 * On réutilise la géométrie vanilla (les calques d'entités déjà enregistrés par le jeu) : pas de
 * fichier de modèle à écrire. Seule la visibilité de la partie « bras droit » change, à chaque image,
 * d'après l'état synchronisé par le DataTracker de l'entité.
 */
@Environment(EnvType.CLIENT)
public class ThrowerZombieModel extends ZombieEntityModel<ThrowerZombieEntity> {
	public ThrowerZombieModel(ModelPart root) {
		super(root);
	}

	@Override
	public void setAngles(ThrowerZombieEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
		super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
		rightArm.visible = !entity.isArmThrown();
	}
}
