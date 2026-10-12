package net.melvunx.zombieexpansion.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.melvunx.zombieexpansion.entity.BlindedZombieEntity;
import net.melvunx.zombieexpansion.entity.FrozenZombieEntity;
import net.melvunx.zombieexpansion.faction.Faction;
import net.melvunx.zombieexpansion.faction.FrostVulnerability;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** /zombie_expansion info : fiche de la créature visée (portée 32 blocs). Réservée à un joueur. */
public final class EntityInfoCommand {
	private static final double RANGE = 32.0;

	private EntityInfoCommand() {
	}

	public static LiteralArgumentBuilder<ServerCommandSource> node() {
		return CommandManager.literal("info").executes(EntityInfoCommand::run);
	}

	private static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();

		// Lancer de rayon depuis les yeux, dans la direction du regard, jusqu'à la première créature.
		Vec3d eye = player.getEyePos();
		Vec3d look = player.getRotationVec(1.0f).multiply(RANGE);
		Box searchBox = player.getBoundingBox().stretch(look).expand(1.0);
		EntityHitResult hit = ProjectileUtil.raycast(player, eye, eye.add(look), searchBox,
			entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator(), RANGE * RANGE);

		if (hit == null || !(hit.getEntity() instanceof LivingEntity living)) {
			source.sendFeedback(() -> Text.translatable("command.zombie_expansion.info.none"), false);
			return 0;
		}

		reply(source, Text.translatable("command.zombie_expansion.info.title",
			living.getName(), Registries.ENTITY_TYPE.getId(living.getType()).toString()));

		Faction.of(living.getType()).ifPresentOrElse(
			faction -> reply(source, Text.translatable("command.zombie_expansion.info.faction", faction.getDisplayName())),
			() -> reply(source, Text.translatable("command.zombie_expansion.info.faction.none")));

		reply(source, Text.translatable("command.zombie_expansion.info.stats",
			String.format("%.1f", living.getHealth()), String.format("%.1f", living.getMaxHealth()),
			String.format("%.3f", living.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED))));

		if (living.isBaby()) {
			reply(source, Text.translatable("command.zombie_expansion.info.baby"));
		}
		if (living instanceof MobEntity mob && mob.getTarget() != null) {
			reply(source, Text.translatable("command.zombie_expansion.info.target", mob.getTarget().getName()));
		}

		if (living instanceof BlindedZombieEntity blinded) {
			reply(source, Text.translatable("command.zombie_expansion.info.blinded",
				Text.translatable("command.zombie_expansion.behavior." + blinded.getBehaviorName()),
				blinded.getListeningRange()));
		}
		if (living instanceof FrozenZombieEntity frozen) {
			if (frozen.isConvertingToZombie()) {
				reply(source, Text.translatable("command.zombie_expansion.info.frozen.converting", frozen.getConversionTicks()));
			}
			if (FrostVulnerability.isInHotBiome(frozen.getWorld(), frozen.getBlockPos())) {
				reply(source, Text.translatable("command.zombie_expansion.info.frozen.hot"));
			}
		}
		return 1;
	}

	private static void reply(ServerCommandSource source, Text text) {
		source.sendFeedback(() -> text, false);
	}
}
