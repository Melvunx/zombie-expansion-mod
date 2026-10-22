package net.melvunx.zombieexpansion.registry;

import net.melvunx.zombieexpansion.ZombieExpansion;
import net.melvunx.zombieexpansion.effect.GluedStatusEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;

/** Registre des effets de statut du mod. */
public final class ModStatusEffects {
	/** Lenteur + bave verte à l'écran (l'overlay est dans le code client). */
	public static final RegistryEntry<StatusEffect> GLUED = register("glued", new GluedStatusEffect());

	private ModStatusEffects() {
	}

	/**
	 * Depuis la 1.20.5, les effets sont référencés par {@link RegistryEntry} (et non plus par l'effet
	 * brut) : c'est ce que demandent StatusEffectInstance et les modificateurs d'attributs.
	 */
	private static RegistryEntry<StatusEffect> register(String name, StatusEffect effect) {
		return Registry.registerReference(Registries.STATUS_EFFECT, ZombieExpansion.id(name), effect);
	}

	/** Force le chargement de la classe (donc l'enregistrement des champs statiques). */
	public static void register() {
	}
}
