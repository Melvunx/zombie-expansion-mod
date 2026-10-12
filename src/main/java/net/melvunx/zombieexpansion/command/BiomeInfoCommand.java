package net.melvunx.zombieexpansion.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.melvunx.zombieexpansion.ZombieExpansion;
import net.melvunx.zombieexpansion.entity.FrozenZombieEntity;
import net.melvunx.zombieexpansion.faction.FrostVulnerability;
import net.melvunx.zombieexpansion.registry.ModBiomeTags;
import net.melvunx.zombieexpansion.world.FrostSpawns;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;

/**
 * /zombie_expansion biome : affiche, à la position de l'exécutant, le biome, sa température,
 * son appartenance à la faction Givre et les poids de spawn de zombie *réellement actifs*
 * (lus sur le biome après application des BiomeModifications).
 *
 * Commande de diagnostic en lecture seule, donc sans niveau de permission requis.
 */
public final class BiomeInfoCommand {
	private BiomeInfoCommand() {
	}

	/** Branche « biome » de /zombie_expansion (assemblée dans ModCommands). */
	public static LiteralArgumentBuilder<ServerCommandSource> node() {
		return CommandManager.literal("biome").executes(BiomeInfoCommand::run);
	}

	private static int run(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		ServerWorld world = source.getWorld();
		BlockPos pos = BlockPos.ofFloored(source.getPosition());
		RegistryEntry<Biome> entry = world.getBiome(pos);
		Biome biome = entry.value();

		Identifier id = entry.getKey().map(key -> key.getValue()).orElse(Identifier.of("unknown"));
		Text biomeName = Text.translatable(Util.createTranslationKey("biome", id));

		reply(source, Text.translatable("command.zombie_expansion.biome.title",
			biomeName, id.toString(), String.format("%.2f", biome.getTemperature())));

		if (entry.isIn(ModBiomeTags.FROST_FACTION_SPAWNS)) {
			reply(source, Text.translatable("command.zombie_expansion.biome.region.frost"));
		} else if (entry.isIn(ModBiomeTags.FROST_FACTION_MIXED_SPAWNS)) {
			reply(source, Text.translatable("command.zombie_expansion.biome.region.mixed",
				Math.round(FrostSpawns.MIXED_FROST_SHARE * 100), Math.round((1 - FrostSpawns.MIXED_FROST_SHARE) * 100)));
		} else {
			reply(source, Text.translatable("command.zombie_expansion.biome.region.none"));
		}

		if (FrostVulnerability.isInHotBiome(world, pos)) {
			reply(source, Text.translatable("command.zombie_expansion.biome.heat.hot",
				Math.round(-FrostVulnerability.HEAT_SLOWDOWN * 100),
				(FrozenZombieEntity.HOT_EXPOSURE_TICKS + FrozenZombieEntity.REVERSION_TICKS) / 20));
		}

		reply(source, Text.translatable(world.isSkyVisible(pos)
			? "command.zombie_expansion.biome.sky.visible"
			: "command.zombie_expansion.biome.sky.hidden"));

		boolean any = false;
		for (SpawnSettings.SpawnEntry spawn : biome.getSpawnSettings().getSpawnEntries(SpawnGroup.MONSTER).getEntries()) {
			if (isZombieFamily(spawn.type)) {
				any = true;
				reply(source, Text.translatable("command.zombie_expansion.biome.spawn.entry",
					Registries.ENTITY_TYPE.getId(spawn.type).toString(),
					spawn.getWeight().getValue(), spawn.minGroupSize, spawn.maxGroupSize));
			}
		}
		if (!any) {
			reply(source, Text.translatable("command.zombie_expansion.biome.spawn.none"));
		}
		return 1;
	}

	/** Zombie vanilla, villageois, noyé, husk, et toutes les entités du mod qui en héritent. */
	private static boolean isZombieFamily(EntityType<?> type) {
		return type == EntityType.ZOMBIE
			|| type == EntityType.ZOMBIE_VILLAGER
			|| type == EntityType.DROWNED
			|| type == EntityType.HUSK
			|| ZombieExpansion.MOD_ID.equals(Registries.ENTITY_TYPE.getId(type).getNamespace());
	}

	private static void reply(ServerCommandSource source, Text text) {
		source.sendFeedback(() -> text, false);
	}
}
