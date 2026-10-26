package net.melvunx.zombieexpansion.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.melvunx.zombieexpansion.registry.ModItems;
import net.melvunx.zombieexpansion.registry.ModSounds;
import net.minecraft.registry.RegistryWrapper;

/** Génère assets/zombie_expansion/lang/en_us.json. À garder strictement parallèle à {@link ModFrenchLangProvider}. */
public class ModEnglishLangProvider extends FabricLanguageProvider {
	public ModEnglishLangProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
		super(dataOutput, "en_us", registryLookup);
	}

	@Override
	public void generateTranslations(RegistryWrapper.WrapperLookup registryLookup, TranslationBuilder builder) {
		for (ModSounds.Entry sound : ModSounds.ENTRIES) {
			builder.add(sound.subtitleKey(), sound.en());
		}
		builder.add(ModEntities.FROZEN_ZOMBIE, "Frozen Zombie");
		builder.add(ModEntities.BLINDED_ZOMBIE, "The Blinded");
		builder.add(ModItems.BLINDED_ZOMBIE_SPAWN_EGG, "The Blinded Spawn Egg");
		builder.add("effect.zombie_expansion.glued", "Glued");
		builder.add(ModEntities.THROWER_ZOMBIE, "Thrower Zombie");
		builder.add(ModEntities.THROWN_ARM, "Thrown Arm");
		builder.add(ModEntities.HORDE_CHIEF, "Horde Chief");
		builder.add(ModEntities.HORDE_MINION, "Horde Minion");
		builder.add(ModItems.HORDE_MINION_SPAWN_EGG, "Horde Minion Spawn Egg");
		builder.add(ModItems.HORDE_CHIEF_SPAWN_EGG, "Horde Chief Spawn Egg");
		builder.add("death.attack.zombie_expansion.horde_attack", "%1$s was slain by %2$s");
		builder.add(ModItems.THROWER_ZOMBIE_SPAWN_EGG, "Thrower Zombie Spawn Egg");
		builder.add(ModItems.FROZEN_ZOMBIE_SPAWN_EGG, "Frozen Zombie Spawn Egg");

		builder.add("command.zombie_expansion.biome.title", "Biome: %s (%s), temperature %s");
		builder.add("faction.zombie_expansion.frost", "Frost");
		builder.add("faction.zombie_expansion.abyss", "Abyss");
		builder.add("faction.zombie_expansion.unearthed", "The Unearthed");
		builder.add("command.zombie_expansion.faction.title", "Faction here: %s");
		builder.add("command.zombie_expansion.faction.member", "  - %s");
		builder.add("command.zombie_expansion.faction.empty", "  No member registered");
		builder.add("command.zombie_expansion.nearby.title", "Faction members within %s blocks:");
		builder.add("command.zombie_expansion.nearby.entry", "  %s: %s");
		builder.add("command.zombie_expansion.nearby.none", "  none");
		builder.add("command.zombie_expansion.nearby.blinded_cap", "Blinded zombies within %s blocks: %s (natural spawn stops at %s)");
		builder.add("command.zombie_expansion.info.none", "No creature in sight");
		builder.add("command.zombie_expansion.info.title", "%s (%s)");
		builder.add("command.zombie_expansion.info.faction", "Faction: %s");
		builder.add("command.zombie_expansion.info.faction.none", "Faction: none");
		builder.add("command.zombie_expansion.info.stats", "Health: %s / %s, speed: %s");
		builder.add("command.zombie_expansion.info.baby", "Baby");
		builder.add("command.zombie_expansion.info.target", "Target: %s");
		builder.add("command.zombie_expansion.info.blinded", "Behavior: %s, listening range: %s blocks");
		builder.add("command.zombie_expansion.info.frozen.converting", "Turning back into a zombie (%s ticks left)");
		builder.add("command.zombie_expansion.info.frozen.hot", "In a hot biome: slowed down");
		builder.add("command.zombie_expansion.behavior.idle", "idle");
		builder.add("command.zombie_expansion.behavior.investigating", "investigating a noise");
		builder.add("command.zombie_expansion.behavior.chasing", "chasing");
		builder.add("command.zombie_expansion.behavior.searching", "searching");
		builder.add("command.zombie_expansion.biome.heat.hot", "Hot biome: Frost faction zombies are slowed by %s%% and turn back into zombies after %s s");
		builder.add("command.zombie_expansion.biome.region.frost", "Region: Frost faction, vanilla zombies are replaced on the surface");
		builder.add("command.zombie_expansion.biome.region.mixed", "Region: mixed, %s%% frozen zombies / %s%% vanilla zombies");
		builder.add("command.zombie_expansion.biome.region.none", "Region: not part of the Frost faction (vanilla spawns)");
		builder.add("command.zombie_expansion.biome.sky.visible", "Position: surface (sky visible)");
		builder.add("command.zombie_expansion.biome.sky.hidden", "Position: underground (no sky visible), vanilla zombies can spawn");
		builder.add("command.zombie_expansion.biome.spawn.entry", "Active spawn: %s, weight %s, group %s-%s");
		builder.add("command.zombie_expansion.biome.spawn.none", "No zombie spawn entry in this biome");
	}
}
