package net.melvunx.zombieexpansion.command;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.melvunx.zombieexpansion.entity.BlindedZombieEntity;
import net.melvunx.zombieexpansion.faction.Faction;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * /zombie_expansion nearby [rayon] : compte les membres de chaque faction autour de l'exécutant.
 * Sert surtout à régler la densité de spawn en jeu (rayon par défaut 32, maximum 128).
 */
public final class NearbyCommand {
	private static final int DEFAULT_RADIUS = 32;

	private NearbyCommand() {
	}

	public static LiteralArgumentBuilder<ServerCommandSource> node() {
		return CommandManager.literal("nearby")
			.executes(context -> run(context, DEFAULT_RADIUS))
			.then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 128))
				.executes(context -> run(context, IntegerArgumentType.getInteger(context, "radius"))));
	}

	private static int run(CommandContext<ServerCommandSource> context, int radius) {
		ServerCommandSource source = context.getSource();
		ServerWorld world = source.getWorld();
		BlockPos pos = BlockPos.ofFloored(source.getPosition());

		List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, new Box(pos).expand(radius), entity -> true);

		Map<Faction, Integer> counts = new EnumMap<>(Faction.class);
		for (LivingEntity entity : entities) {
			Faction.of(entity.getType()).ifPresent(faction -> counts.merge(faction, 1, Integer::sum));
		}

		source.sendFeedback(() -> Text.translatable("command.zombie_expansion.nearby.title", radius), false);
		if (counts.isEmpty()) {
			source.sendFeedback(() -> Text.translatable("command.zombie_expansion.nearby.none"), false);
		}
		counts.forEach((faction, count) -> source.sendFeedback(
			() -> Text.translatable("command.zombie_expansion.nearby.entry", faction.getDisplayName(), count), false));

		// Le plafond de spawn des aveugles porte sur un rayon fixe : on le montre pour régler MAX_NEARBY.
		long blindedInCap = entities.stream()
			.filter(entity -> entity instanceof BlindedZombieEntity)
			.filter(entity -> entity.squaredDistanceTo(source.getPosition()) <= (double) BlindedZombieEntity.DENSITY_RADIUS * BlindedZombieEntity.DENSITY_RADIUS)
			.count();
		source.sendFeedback(() -> Text.translatable("command.zombie_expansion.nearby.blinded_cap",
			BlindedZombieEntity.DENSITY_RADIUS, blindedInCap, BlindedZombieEntity.MAX_NEARBY), false);

		return entities.size();
	}
}
