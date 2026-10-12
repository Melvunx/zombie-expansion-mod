package net.melvunx.zombieexpansion.entity.ai;

import java.util.EnumSet;

import net.melvunx.zombieexpansion.entity.BlindedZombieEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Le zombie aveugle va voir d'où vient le bruit qu'il a entendu.
 *
 * Un {@link Goal} est une "envie" de l'IA : le jeu appelle {@link #canStart()} chaque tick,
 * et lance celle qui a la meilleure priorité. Ici : tant qu'il a une position à vérifier
 * et pas de cible à attaquer, il s'y rend. C'est ce qui permet de l'éloigner avec une boule de neige.
 */
public class InvestigateVibrationGoal extends Goal {
	private final BlindedZombieEntity mob;
	private final double speed;
	private BlockPos destination;

	public InvestigateVibrationGoal(BlindedZombieEntity mob, double speed) {
		this.mob = mob;
		this.speed = speed;
		// MOVE : ce goal pilote le déplacement, il ne peut donc pas tourner en même temps qu'un autre goal de mouvement.
		setControls(EnumSet.of(Control.MOVE));
	}

	@Override
	public boolean canStart() {
		return mob.getTarget() == null && mob.getInvestigatePos() != null;
	}

	@Override
	public void start() {
		goToInvestigatePos();
	}

	@Override
	public boolean shouldContinue() {
		return mob.getTarget() == null && mob.getInvestigatePos() != null && !mob.getNavigation().isIdle();
	}

	@Override
	public void tick() {
		BlockPos pos = mob.getInvestigatePos();
		if (pos == null) {
			return;
		}
		// Un nouveau bruit a changé la destination : on repart vers la nouvelle.
		if (!pos.equals(destination)) {
			goToInvestigatePos();
		}
		if (mob.squaredDistanceTo(Vec3d.ofCenter(pos)) < 4.0) {
			// Arrivé : plus rien à voir (il est aveugle), il cherche à tâtons avant de repartir.
			mob.clearInvestigation();
			mob.beginSearch();
		}
	}

	@Override
	public void stop() {
		mob.getNavigation().stop();
		// Arrivé, ou chemin impossible : dans les deux cas on abandonne cette piste.
		mob.clearInvestigation();
		destination = null;
	}

	private void goToInvestigatePos() {
		destination = mob.getInvestigatePos();
		if (destination != null) {
			mob.getNavigation().startMovingTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5, speed);
		}
	}
}
