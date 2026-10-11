package net.melvunx.zombieexpansion.mixin;

import net.melvunx.zombieexpansion.entity.FrozenZombieEntity;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Un zombie (adulte ou bébé) qui reste dans la neige poudreuse devient un zombie gelé.
 *
 * Même principe que le squelette qui devient un Stray (voir SkeletonEntity#tick), mais
 * une classe vanilla n'a pas de hook pour ça : on s'insère dans ZombieEntity#tick.
 * La conversion passe par MobEntity#convertTo, qui recopie le flag bébé, l'équipement,
 * le nom, etc. : un bébé zombie donne donc un bébé zombie gelé sans code supplémentaire.
 */
@Mixin(ZombieEntity.class)
public abstract class ZombieEntityMixin {
	/** Durée passée dans la neige poudreuse avant la transformation (100 ticks = 5 s). */
	@Unique
	private static final int POWDER_SNOW_CONVERSION_TICKS = 100;

	/** Non persisté : si le monde est rechargé, le décompte repart de zéro, sans conséquence. */
	@Unique
	private int zombieexpansion$powderSnowTicks;

	@Inject(method = "tick", at = @At("HEAD"))
	private void zombieexpansion$convertInPowderSnow(CallbackInfo ci) {
		ZombieEntity self = (ZombieEntity) (Object) this;

		// getType() == ZOMBIE exclut le husk, le noyé et le zombie gelé lui-même, qui héritent tous de ZombieEntity.
		if (self.getWorld().isClient || !self.isAlive() || self.isAiDisabled() || self.getType() != EntityType.ZOMBIE) {
			return;
		}

		// On lit le bloc à la position du zombie plutôt que Entity#inPowderSnow : un @Shadow ne
		// retrouve pas un champ hérité d'Entity depuis un mixin sur ZombieEntity.
		if (!self.getBlockStateAtPos().isOf(Blocks.POWDER_SNOW)) {
			this.zombieexpansion$powderSnowTicks = 0;
			return;
		}

		if (++this.zombieexpansion$powderSnowTicks < POWDER_SNOW_CONVERSION_TICKS) {
			return;
		}

		FrozenZombieEntity frozen = self.convertTo(ModEntities.FROZEN_ZOMBIE, true);
		if (frozen != null && self.getWorld() instanceof ServerWorld serverWorld) {
			serverWorld.spawnParticles(ParticleTypes.SNOWFLAKE,
				frozen.getX(), frozen.getBodyY(0.5), frozen.getZ(), 30, 0.3, 0.5, 0.3, 0.05);
		}
	}
}
