package net.melvunx.zombieexpansion.damage;

import net.melvunx.zombieexpansion.ZombieExpansion;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageScaling;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.Difficulty;

/**
 * Dégâts de mêlée « finaux » choisis par difficulté, pour les membres des Déterrés qui sortent du
 * budget du zombie vanilla (Chef de horde, Sous-fifre, Gonflé, Bûcheron).
 *
 * Pourquoi un type de dégâts à nous : l'attribut d'attaque est rééchelonné par le jeu quand la cible est
 * un joueur (Facile : min(x/2 + 1, x), Difficile : x × 1,5). Les valeurs voulues (par ex. vanilla + 1 PV
 * à toutes les difficultés) ne s'obtiennent donc pas avec un seul attribut. Le type {@link #HORDE_ATTACK}
 * a {@code scaling: never} : le jeu n'y touche plus, ce que l'on écrit est ce que le joueur reçoit.
 *
 * Le type est une entrée de registre dynamique : déclaré ici, écrit en JSON par la datagen
 * ({@code ModDamageTypeProvider}), chargé par le jeu comme n'importe quel type vanilla.
 */
public final class DifficultyDamage {
	public static final RegistryKey<DamageType> HORDE_ATTACK =
		RegistryKey.of(RegistryKeys.DAMAGE_TYPE, ZombieExpansion.id("horde_attack"));

	private DifficultyDamage() {
	}

	/** Appelé par la datagen (voir ZombieExpansionDataGenerator#buildRegistry). */
	public static void bootstrap(Registerable<DamageType> registerable) {
		// message_id : clé de traduction « death.attack.zombie_expansion.horde_attack ».
		registerable.register(HORDE_ATTACK, new DamageType("zombie_expansion.horde_attack", DamageScaling.NEVER, 0.1f));
	}

	/** Les trois montants d'une créature (en PV) selon la difficulté. */
	public record Table(float easy, float normal, float hard) {
		/** Même montant à toutes les difficultés. */
		public static Table flat(float amount) {
			return new Table(amount, amount, amount);
		}

		/**
		 * Contre un joueur : la valeur de la difficulté. Contre une autre créature, le jeu ne rééchelonne
		 * pas les dégâts : on prend la valeur Normale. En Paisible il n'y a pas de monstres, donc pas de cas.
		 */
		public float amountAgainst(Difficulty difficulty, Entity target) {
			if (!(target instanceof PlayerEntity)) {
				return normal;
			}
			return switch (difficulty) {
				case EASY -> easy;
				case HARD -> hard;
				default -> normal;
			};
		}
	}

	public static DamageSource source(LivingEntity attacker) {
		return new DamageSource(
			attacker.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(HORDE_ATTACK), attacker);
	}
}
