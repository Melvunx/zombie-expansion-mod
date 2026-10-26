package net.melvunx.zombieexpansion.world;

import java.util.List;
import java.util.Optional;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.melvunx.zombieexpansion.ZombieExpansion;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.biome.SpawnSettings;

/**
 * Spawns des membres modés de la faction Les Déterrés.
 *
 * Le zombie vanilla est la référence : chaque membre reçoit un poids égal à une **part** du poids
 * du zombie *dans le biome* (95 presque partout), comme le zombie aveugle. On ne code donc aucun
 * poids absolu, et un biome de mod ou de datapack est respecté.
 *
 * **Plafonnement** : chaque membre a sa part, et la somme des parts est bornée par
 * {@link #TOTAL_SHARE_CAP}. Le zombie vanilla (poids 100 %) reste ainsi très majoritaire : avec un
 * plafond de 35 %, les Déterrés modés représentent au plus ~26 % des zombies d'un biome
 * (0,35 / 1,35). Ajouter un membre = ajouter une ligne à {@link #MEMBERS} ; dépasser le plafond
 * fait échouer le chargement du mod, pour qu'on ne l'oublie pas.
 *
 * Où ils apparaissent (surface et grottes jusqu'à Y -8, sous terre seulement dans les biomes du
 * Givre) n'est pas décidé ici : un BiomeModifications ne connaît ni l'altitude ni le ciel. C'est
 * la règle {@code Faction#allowsUnearthedSpawn}, appelée par le canSpawn de chaque membre.
 * Les biomes sans zombie (Deep Dark, champignons) ne reçoivent rien : les Déterrés n'y ont pas leur place.
 */
public final class UnearthedSpawns {
	/** Somme maximale des parts des membres, relativement au poids du zombie vanilla du biome. */
	public static final float TOTAL_SHARE_CAP = 0.35f;

	/** Un membre et sa part du poids du zombie vanilla (0,08 = 8 %). */
	private record Member(EntityType<?> type, float share, int minGroup, int maxGroup) {
	}

	/** Parts à régler en jeu (cible : 5 à 10 % par membre). */
	private static final List<Member> MEMBERS = List.of(
		new Member(ModEntities.THROWER_ZOMBIE, 0.08f, 1, 2),
		// Rare : un chef n'apparaît qu'en surface (voir HordeChiefEntity#canSpawn) et amène sa troupe.
		new Member(ModEntities.HORDE_CHIEF, 0.05f, 1, 1),
		// Même part que le lanceur en attendant que tous les membres existent, pour régler les pourcentages d'un coup.
		new Member(ModEntities.RUNNER_ZOMBIE, 0.08f, 1, 2)
	);

	private UnearthedSpawns() {
	}

	public static void register() {
		float total = 0.0f;
		for (Member member : MEMBERS) {
			total += member.share();
		}
		if (total > TOTAL_SHARE_CAP + 1.0e-4f) {
			throw new IllegalStateException("Unearthed spawn shares add up to " + total + ", above the cap " + TOTAL_SHARE_CAP);
		}

		BiomeModifications.create(ZombieExpansion.id("unearthed_members"))
			.add(ModificationPhase.ADDITIONS,
				BiomeSelectors.foundInOverworld(),
				(selection, modification) -> {
					Optional<SpawnSettings.SpawnEntry> zombie = FrostSpawns.findZombie(selection);
					if (zombie.isEmpty()) {
						return;
					}
					int zombieWeight = zombie.get().getWeight().getValue();
					for (Member member : MEMBERS) {
						int weight = Math.max(1, Math.round(zombieWeight * member.share()));
						modification.getSpawnSettings().addSpawn(SpawnGroup.MONSTER,
							new SpawnSettings.SpawnEntry(member.type(), weight, member.minGroup(), member.maxGroup()));
					}
				});
	}
}
