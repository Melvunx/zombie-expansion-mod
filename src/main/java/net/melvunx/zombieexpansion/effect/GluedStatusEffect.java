package net.melvunx.zombieexpansion.effect;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.melvunx.zombieexpansion.ZombieExpansion;

/**
 * Effet « Englué » : la bave du bras du zombie lanceur colle le joueur au sol.
 *
 * Un {@link StatusEffect} n'a pas de logique à lui : il déclare des modificateurs d'attributs
 * que le jeu applique tant que l'effet est actif et retire à sa fin (ou au lait). La lenteur est
 * donc un simple modificateur de vitesse, comme celui de l'effet vanilla Lenteur.
 *
 * Le constructeur de StatusEffect est protégé : c'est pour ça qu'on passe par une sous-classe.
 */
public class GluedStatusEffect extends StatusEffect {
	/** Durée habituelle de l'effet (3 s) : bras lancé et coups du zombie lanceur. */
	public static final int DEFAULT_DURATION_TICKS = 60;

	/** -30 % de vitesse (Lenteur I vanilla fait -15 %). À ajuster en jeu. */
	private static final double SLOWDOWN = -0.30;

	public GluedStatusEffect() {
		// HARMFUL : affiché en rouge, retiré par le lait, ignoré par les potions « bénéfiques ».
		super(StatusEffectCategory.HARMFUL, 0x6FBF3A);
		// L'identifiant du modificateur doit être unique par effet : il sert de clé pour le retirer.
		addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED, ZombieExpansion.id("effect.glued"),
			SLOWDOWN, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}
}
