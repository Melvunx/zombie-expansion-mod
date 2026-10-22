package net.melvunx.zombieexpansion.entity.ai;

import java.util.EnumSet;

import net.melvunx.zombieexpansion.entity.ThrowerZombieEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;

/**
 * Le zombie lanceur s'arrête, vise, puis lance son bras.
 *
 * Il ne lance que s'il a encore son bras, que la cible est à bonne distance et visible. Trop près,
 * il se bat au corps à corps (ZombieAttackGoal, de priorité plus basse) : le joueur peut donc
 * l'empêcher de tirer en lui collant. Une fois le bras parti, le goal s'arrête jusqu'à la repousse.
 */
public class ThrowArmGoal extends Goal {
	/** En dessous, le zombie préfère frapper. */
	private static final double MIN_RANGE = 3.0;
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
		if (target == null || !target.isAlive() || mob.isArmThrown()) {
			return false;
		}
		double distanceSq = mob.squaredDistanceTo(target);
		return distanceSq >= MIN_RANGE * MIN_RANGE && distanceSq <= MAX_RANGE * MAX_RANGE && mob.canSee(target);
	}

	@Override
	public boolean shouldContinue() {
		LivingEntity target = mob.getTarget();
		return target != null && target.isAlive() && !mob.isArmThrown() && mob.canSee(target);
	}

	@Override
	public void start() {
		windup = WINDUP_TICKS;
		mob.getNavigation().stop();
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
