package net.melvunx.zombieexpansion.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/** Point d'enregistrement de toutes les commandes du mod (racine : /zombie_expansion). */
public final class ModCommands {
	private ModCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			BiomeInfoCommand.register(dispatcher));
	}
}
