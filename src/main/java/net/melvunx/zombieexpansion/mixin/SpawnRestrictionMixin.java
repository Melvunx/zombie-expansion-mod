package net.melvunx.zombieexpansion.mixin;

import net.melvunx.zombieexpansion.registry.ModBiomeTags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Interdit au zombie vanilla d'apparaître **en surface** dans les biomes de la faction
 * Givre, tout en le laissant apparaître sous terre (grottes).
 *
 * Pourquoi un Mixin : la règle de spawn d'un type d'entité est enregistrée une seule
 * fois, globalement, et ne connaît pas le biome. Ni BiomeModifications ni l'API Fabric
 * ne permettent d'y greffer une condition par biome + position.
 *
 * {@link SpawnRestriction#canSpawn} est le point de passage de tous les spawns naturels :
 * on y refuse le zombie avant que le jeu ne continue.
 */
@Mixin(SpawnRestriction.class)
public class SpawnRestrictionMixin {
	@Inject(method = "canSpawn", at = @At("HEAD"), cancellable = true)
	private static <T extends Entity> void zombieexpansion$noVanillaZombieOnFrostSurface(
		EntityType<T> type, ServerWorldAccess world, SpawnReason reason, BlockPos pos, Random random,
		CallbackInfoReturnable<Boolean> cir
	) {
		// Renforts, œufs, spawners, etc. ne sont pas concernés : seulement les spawns naturels.
		boolean natural = reason == SpawnReason.NATURAL || reason == SpawnReason.CHUNK_GENERATION;

		if (natural
			&& type == EntityType.ZOMBIE
			&& world.isSkyVisible(pos)
			&& world.getBiome(pos).isIn(ModBiomeTags.FROST_FACTION_SPAWNS)) {
			cir.setReturnValue(false);
		}
	}
}
