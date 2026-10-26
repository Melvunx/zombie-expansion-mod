package net.melvunx.zombieexpansion.client.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.melvunx.zombieexpansion.ZombieExpansion;
import net.melvunx.zombieexpansion.entity.FrozenZombieEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modèle GeckoLib du zombie gelé.
 *
 * L'adulte et le bébé partagent le même modèle 3D et la même texture, mais le bébé a son
 * propre fichier d'animations (durées et amplitudes adaptées à sa petite taille).
 * {@link DefaultedEntityGeoModel} ne sait pas faire ce choix seul : on surcharge donc
 * uniquement {@link #getAnimationResource}, GeckoLib l'appelle avant de lire les animations.
 */
@Environment(EnvType.CLIENT)
public class FrozenZombieModel extends DefaultedEntityGeoModel<FrozenZombieEntity> {
	private static final Identifier ADULT_ANIMATIONS = ZombieExpansion.id("animations/entity/frost/frozen_zombie.animation.json");
	private static final Identifier BABY_ANIMATIONS = ZombieExpansion.id("animations/entity/frost/baby_frozen_zombie.animation.json");

	public FrozenZombieModel() {
		// true = le bone nommé "head" suit la direction du regard de l'entité.
		super(ZombieExpansion.id("frost/frozen_zombie"), true);
	}

	@Override
	public Identifier getAnimationResource(FrozenZombieEntity animatable) {
		return animatable.isBaby() ? BABY_ANIMATIONS : ADULT_ANIMATIONS;
	}
}
