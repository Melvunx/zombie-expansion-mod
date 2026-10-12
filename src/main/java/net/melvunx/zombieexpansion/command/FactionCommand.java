package net.melvunx.zombieexpansion.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.melvunx.zombieexpansion.faction.Faction;
import net.minecraft.entity.EntityType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** /zombie_expansion faction : la faction dominante ici et la liste de ses membres (lue dans le tag). */
public final class FactionCommand {
	private FactionCommand() {
	}

	public static LiteralArgumentBuilder<ServerCommandSource> node() {
		return CommandManager.literal("faction").executes(FactionCommand::run);
	}

	private static int run(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		BlockPos pos = BlockPos.ofFloored(source.getPosition());
		Faction faction = Faction.at(source.getWorld(), pos);

		source.sendFeedback(() -> Text.translatable("command.zombie_expansion.faction.title", faction.getDisplayName()), false);

		var members = faction.getMembers();
		if (members.isEmpty()) {
			source.sendFeedback(() -> Text.translatable("command.zombie_expansion.faction.empty"), false);
		}
		for (EntityType<?> member : members) {
			source.sendFeedback(() -> Text.translatable("command.zombie_expansion.faction.member", member.getName()), false);
		}
		return members.size();
	}
}
