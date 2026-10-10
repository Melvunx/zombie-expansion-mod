package net.melvunx.zombieexpansion;

import net.fabricmc.api.ModInitializer;

import net.melvunx.zombieexpansion.registry.ModEntities;
import net.melvunx.zombieexpansion.registry.ModItems;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZombieExpansion implements ModInitializer {
	public static final String MOD_ID = "zombie_expansion";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		// Ordre important : les entités avant les objets (l'œuf référence le type d'entité).
		ModEntities.register();
		ModItems.register();

		LOGGER.info("Zombie Expansion loaded");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
