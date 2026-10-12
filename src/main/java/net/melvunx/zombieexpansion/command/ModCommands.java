package net.melvunx.zombieexpansion.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.melvunx.zombieexpansion.ZombieExpansion;
import net.minecraft.server.command.CommandManager;

/**
 * Point d'enregistrement de toutes les commandes du mod.
 *
 * Il n'y a qu'une seule racine, /zombie_expansion, assemblée ici : chaque classe de commande
 * fournit sa branche via {@code node()}.
 */
public final class ModCommands {
	private ModCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(CommandManager.literal(ZombieExpansion.MOD_ID)
				.then(BiomeInfoCommand.node())
				.then(FactionCommand.node())
				.then(NearbyCommand.node())
				.then(EntityInfoCommand.node())));
	}
}
