package net.melvunx.zombieexpansion.entity.ai;

import java.util.EnumSet;

import net.melvunx.zombieexpansion.entity.HordeChiefEntity;
import net.melvunx.zombieexpansion.entity.HordeMinionEntity;
import net.minecraft.entity.ai.goal.Goal;

/**
 * Le sous-fifre reste près de son chef quand il n'a pas de cible : il le rejoint dès qu'il s'en éloigne
 * de plus de {@link #START_DISTANCE} blocs et s'arrête à {@link #STOP_DISTANCE}. Inspiré de FollowParentGoal
 * (le poussin suit sa mère), mais piloté par un chef quelconque et pas par la proximité d'un parent.
 */
public class FollowLeaderGoal extends Goal {
	private static final double START_DISTANCE = 8.0;
	private static final double STOP_DISTANCE = 4.0;
	private static final double SPEED = 1.0;
	/** Ne recalcule le chemin que tous les 10 ticks : un chemin est coûteux. */
	private static final int REPATH_TICKS = 10;

	private final HordeMinionEntity mob;
	private int repathCooldown;

	public FollowLeaderGoal(HordeMinionEntity mob) {
		this.mob = mob;
		setControls(EnumSet.of(Control.MOVE));
	}

	@Override
	public boolean canStart() {
		HordeChiefEntity leader = mob.getLeader();
		return leader != null && mob.getTarget() == null
			&& mob.squaredDistanceTo(leader) > START_DISTANCE * START_DISTANCE;
	}

	@Override
	public boolean shouldContinue() {
		HordeChiefEntity leader = mob.getLeader();
		return leader != null && mob.getTarget() == null
			&& mob.squaredDistanceTo(leader) > STOP_DISTANCE * STOP_DISTANCE;
	}

	@Override
	public void start() {
		repathCooldown = 0;
	}

	@Override
	public void stop() {
		mob.getNavigation().stop();
	}

	@Override
	public void tick() {
		HordeChiefEntity leader = mob.getLeader();
		if (leader != null && --repathCooldown <= 0) {
			repathCooldown = REPATH_TICKS;
			mob.getNavigation().startMovingTo(leader, SPEED);
		}
	}
}
