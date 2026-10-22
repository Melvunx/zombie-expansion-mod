package net.melvunx.zombieexpansion.entity.ai;

import java.util.EnumSet;

import net.melvunx.zombieexpansion.entity.ThrowerZombieEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;

/**
 * Le zombie lanceur s'arrête, vise, puis lance son bras.
 *
 * Il ne lance que s'il a son bras (ni lancé, ni en train de repousser), que la cible est à plus de
 * {@link ThrowerZombieEntity#MELEE_RANGE} blocs et visible. Plus près, il se bat au corps à corps
 * (ZombieAttackGoal, de priorité plus basse) : le joueur peut donc l'empêcher de tirer en lui collant.
 * Si le joueur se rapproche pendant la visée, le tir est annulé.
 */
public class ThrowArmGoal extends Goal {
	/** En dessous, le zombie frappe au corps à corps ; au-delà, il lance de nouveau. */
	private static final double MIN_RANGE = ThrowerZombieEntity.MELEE_RANGE;
	private static final double MAX_RANGE = 12.0;
	/** Temps de visée avant le tir : 0,75 s, pendant lesquelles le joueur peut réagir. */
	private static final int WINDUP_TICKS = 15;

	private final ThrowerZombieEntity mob;
	private int windup;

	public ThrowArmGoal(ThrowerZombieEntity mob) {
		this.mob = mob;
		// MOVE : il reste immobile pendant la visée ; LOOK : il regarde sa cible.
		setControls(EnumSet.of(Control.MOVE, Control.LOOK));
	}

	@Override
	public boolean canStart() {
		LivingEntity target = mob.getTarget();
		if (target == null || !target.isAlive() || mob.isArmThrown() || mob.isThrowing() || mob.isRegrowing()) {
			return false;
		}
		double distanceSq = mob.squaredDistanceTo(target);
		return distanceSq >= MIN_RANGE * MIN_RANGE && distanceSq <= MAX_RANGE * MAX_RANGE && mob.canSee(target);
	}

	@Override
	public boolean shouldContinue() {
		LivingEntity target = mob.getTarget();
		// Si le joueur se rapproche pendant la visée, on annule : la mêlée reprend.
		return target != null && target.isAlive() && !mob.isArmThrown() && !mob.isThrowing()
			&& mob.squaredDistanceTo(target) >= MIN_RANGE * MIN_RANGE && mob.canSee(target);
	}

	@Override
	public void start() {
		windup = WINDUP_TICKS;
		mob.getNavigation().stop();
		mob.setAiming(true);
	}

	@Override
	public void stop() {
		mob.setAiming(false);
	}

	@Override
	public void tick() {
		LivingEntity target = mob.getTarget();
		if (target == null) {
			return;
		}
		mob.getLookControl().lookAt(target, 30.0f, 30.0f);
		if (--windup <= 0) {
			mob.throwArm(target);
		}
	}
}
