package net.melvunx.zombieexpansion.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.melvunx.zombieexpansion.registry.ModItems;
import net.minecraft.registry.RegistryWrapper;

/** Génère assets/zombie_expansion/lang/fr_fr.json. */
public class ModFrenchLangProvider extends FabricLanguageProvider {
	public ModFrenchLangProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
		super(dataOutput, "fr_fr", registryLookup);
	}

	@Override
	public void generateTranslations(RegistryWrapper.WrapperLookup registryLookup, TranslationBuilder builder) {
		builder.add(ModEntities.FROZEN_ZOMBIE, "Zombie gelé");
		builder.add(ModEntities.BLINDED_ZOMBIE, "Zombie aveugle");
		builder.add(ModItems.BLINDED_ZOMBIE_SPAWN_EGG, "Œuf d'apparition de zombie aveugle");
		builder.add(ModItems.FROZEN_ZOMBIE_SPAWN_EGG, "Œuf d'apparition de zombie gelé");

		builder.add("command.zombie_expansion.biome.title", "Biome : %s (%s), température %s");
		builder.add("faction.zombie_expansion.frost", "Givre");
		builder.add("faction.zombie_expansion.abyss", "Abysses");
		builder.add("faction.zombie_expansion.vanilla", "Zombies vanilla");
		builder.add("command.zombie_expansion.faction.title", "Faction ici : %s");
		builder.add("command.zombie_expansion.faction.member", "  - %s");
		builder.add("command.zombie_expansion.faction.empty", "  Aucun membre enregistré");
		builder.add("command.zombie_expansion.nearby.title", "Membres de faction dans un rayon de %s blocs :");
		builder.add("command.zombie_expansion.nearby.entry", "  %s : %s");
		builder.add("command.zombie_expansion.nearby.none", "  aucun");
		builder.add("command.zombie_expansion.nearby.blinded_cap", "Zombies aveugles dans un rayon de %s blocs : %s (le spawn naturel s'arrête à %s)");
		builder.add("command.zombie_expansion.info.none", "Aucune créature en vue");
		builder.add("command.zombie_expansion.info.title", "%s (%s)");
		builder.add("command.zombie_expansion.info.faction", "Faction : %s");
		builder.add("command.zombie_expansion.info.faction.none", "Faction : aucune");
		builder.add("command.zombie_expansion.info.stats", "Vie : %s / %s, vitesse : %s");
		builder.add("command.zombie_expansion.info.baby", "Bébé");
		builder.add("command.zombie_expansion.info.target", "Cible : %s");
		builder.add("command.zombie_expansion.info.blinded", "Comportement : %s, portée d'écoute : %s blocs");
		builder.add("command.zombie_expansion.info.frozen.converting", "Redevient un zombie (%s ticks restants)");
		builder.add("command.zombie_expansion.info.frozen.hot", "En biome chaud : ralenti");
		builder.add("command.zombie_expansion.behavior.idle", "au repos");
		builder.add("command.zombie_expansion.behavior.investigating", "enquête sur un bruit");
		builder.add("command.zombie_expansion.behavior.chasing", "poursuit");
		builder.add("command.zombie_expansion.behavior.searching", "cherche");
		builder.add("command.zombie_expansion.biome.heat.hot", "Biome chaud : les zombies de la faction Givre sont ralentis de %s %% et redeviennent des zombies après %s s");
		builder.add("command.zombie_expansion.biome.region.frost", "Région : faction Givre, les zombies vanilla sont remplacés en surface");
		builder.add("command.zombie_expansion.biome.region.mixed", "Région : mixte, %s %% de zombies gelés / %s %% de zombies vanilla");
		builder.add("command.zombie_expansion.biome.region.none", "Région : hors faction Givre (spawns vanilla)");
		builder.add("command.zombie_expansion.biome.sky.visible", "Position : surface (ciel visible)");
		builder.add("command.zombie_expansion.biome.sky.hidden", "Position : sous terre (ciel non visible), les zombies vanilla peuvent apparaître");
		builder.add("command.zombie_expansion.biome.spawn.entry", "Spawn actif : %s, poids %s, groupe %s-%s");
		builder.add("command.zombie_expansion.biome.spawn.none", "Aucune entrée de zombie dans ce biome");
	}
}
