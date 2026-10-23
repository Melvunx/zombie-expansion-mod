package net.melvunx.zombieexpansion.entity;

import net.melvunx.zombieexpansion.damage.DifficultyDamage;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Zombie dont la mêlée inflige des dégâts fixés par difficulté ({@link DifficultyDamage.Table}), et non
 * ceux de l'attribut d'attaque. Base commune des membres des Déterrés qui sortent du budget vanilla
 * (Chef de horde, Sous-fifre, plus tard Gonflé et Bûcheron).
 *
 * {@code tryAttack} est recopié de MobEntity (dégâts, recul, enchantements) et de ZombieEntity (mise à feu),
 * car le seul point d'entrée de la valeur des dégâts y est interne : on ne peut pas se contenter de
 * l'appeler puis d'en corriger le résultat. Seuls changent le montant et le type de dégâts.
 */
public abstract class FixedDamageZombieEntity extends ZombieEntity {
	protected FixedDamageZombieEntity(EntityType<? extends ZombieEntity> entityType, World world) {
		super(entityType, world);
	}

	protected abstract DifficultyDamage.Table getDamageTable();

	@Override
	public boolean tryAttack(Entity target) {
		float amount = getDamageTable().amountAgainst(getWorld().getDifficulty(), target);
		DamageSource source = DifficultyDamage.source(this);
		if (getWorld() instanceof ServerWorld serverWorld) {
			amount = EnchantmentHelper.getDamage(serverWorld, getWeaponStack(), target, source, amount);
		}

		boolean hit = target.damage(source, amount);
		if (hit) {
			float knockback = getKnockbackAgainst(target, source);
			if (knockback > 0.0f && target instanceof LivingEntity living) {
				living.takeKnockback(knockback * 0.5f,
					MathHelper.sin(getYaw() * (float) (Math.PI / 180.0)), -MathHelper.cos(getYaw() * (float) (Math.PI / 180.0)));
				setVelocity(getVelocity().multiply(0.6, 1.0, 0.6));
			}
			if (getWorld() instanceof ServerWorld serverWorld) {
				EnchantmentHelper.onTargetDamaged(serverWorld, target, source);
			}
			onAttacking(target);
			playAttackSound();

			// Comme ZombieEntity : un zombie en feu, les mains vides, enflamme sa cible.
			float local = getWorld().getLocalDifficulty(getBlockPos()).getLocalDifficulty();
			if (getMainHandStack().isEmpty() && isOnFire() && random.nextFloat() < local * 0.3f) {
				target.setOnFireFor(2 * (int) local);
			}
		}
		return hit;
	}
}
