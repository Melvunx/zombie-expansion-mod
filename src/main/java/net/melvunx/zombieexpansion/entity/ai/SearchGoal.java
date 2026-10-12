package net.melvunx.zombieexpansion.entity.ai;

import java.util.EnumSet;

import net.melvunx.zombieexpansion.entity.BlindedZombieEntity;
import net.minecraft.entity.ai.goal.Goal;

/**
 * Le zombie aveugle s'arrête et « cherche » (renifle, tend l'oreille, scrute le sol) après être
 * arrivé à l'endroit d'un bruit. Le décompte est tenu par l'entité ; ce goal ne fait que
 * l'immobiliser, pour que l'animation de recherche ne glisse pas sur le sol.
 */
public class SearchGoal extends Goal {
	private final BlindedZombieEntity mob;

	public SearchGoal(BlindedZombieEntity mob) {
		this.mob = mob;
		setControls(EnumSet.of(Control.MOVE, Control.LOOK));
	}

	@Override
	public boolean canStart() {
		return mob.isSearching() && mob.getTarget() == null;
	}

	@Override
	public void start() {
		mob.getNavigation().stop();
	}

	@Override
	public boolean shouldContinue() {
		return canStart();
	}
}
