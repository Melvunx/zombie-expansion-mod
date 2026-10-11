# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Ce fichier donne le contexte du projet Zombie Expansion à Claude (et à tout contributeur) : vision, spécifications techniques, conventions et feuille de route.

## Commandes

Toutes les commandes passent par le wrapper Gradle (`./gradlew`, ou `gradlew.bat` sous Windows) :

| Commande | Rôle |
|---|---|
| `./gradlew build` | Compile, remappe et produit les jars dans `build/libs/` (c'est ce que lance la CI) |
| `./gradlew runClient` | Lance Minecraft client avec le mod (dossier `run/`) |
| `./gradlew runServer` | Lance un serveur dédié de test |
| `./gradlew runDatagen` | Régénère `src/main/generated/` à partir des providers |

- Il n'y a pas de tests automatisés (pas de `src/test`) : la validation se fait en jeu via `runClient`.
- `gradle.properties` centralise les versions (Minecraft, Yarn, Loader, Fabric API) ; `${version}` est injecté dans `fabric.mod.json` par `processResources`.
- **Gradle doit tourner sur un JDK 25** (Fabric Loom 1.18 l'exige), alors que le code cible Java 21 (`options.release = 21`). Avec un JDK 21 : « Dependency requires at least JVM runtime version 25 ». La CI GitHub utilise déjà JDK 25.
- Depuis WSL il n'y a pas de `java` : lancer Gradle côté Windows, par ex. `cmd.exe /c "set JAVA_HOME=C:\Program Files\Java\jdk-25.0.2&& gradlew.bat runDatagen --console=plain"`.

## Contexte

- **Projet** : Zombie Expansion, mod Minecraft qui ajoute de nombreuses variantes de zombies, propres à chaque biome et à chaque type de caverne, avec de nouvelles capacités, des factions et des interactions entre mobs.
- **Auteur** : moddeur débutant qui apprend le modding. **Expliquer chaque fichier et chaque concept** quand du code est produit (ce que fait la classe, pourquoi elle hérite de telle classe, où elle est enregistrée). Privilégier des étapes courtes et testables en jeu.
- **Langue** : échanges et commentaires de documentation en français ; identifiants de code, noms de classes et clés de registre en anglais.

## Spécifications techniques

| Élément | Valeur |
|---|---|
| Version Minecraft | **1.21.1** |
| Mod loader | **Fabric** (Fabric Loader, dernière version stable compatible 1.21.1) |
| API | **Fabric API** (version `+1.21.1`) |
| Java | **21** |
| Build | Gradle + Fabric Loom (template officiel fabricmc.net/develop) |
| Mappings | Yarn (noms de classes Yarn : `ZombieEntity`, `ActiveTargetGoal`, etc.) |
| IDE | IntelliJ IDEA |
| Modèles / textures | Blockbench |
| Animations | **GeckoLib 4.9.3** (`geckolib-fabric-1.21.1`, dépôt Cloudsmith, déclaré dans `fabric.mod.json` en `depends`) |

- **mod_id** : `zombie_expansion` (ne plus le changer)
- **Package racine** : `net.melvunx.zombieexpansion`
- **Auteur** : melvunx · **Licence** : MIT
- **Entrées dans `fabric.mod.json`** : `main` (`ZombieExpansion`) et `fabric-datagen` (`ZombieExpansionDataGenerator`). `ZombieExpansion.id(path)` crée un `Identifier` du mod : l'utiliser plutôt que reconstruire les identifiants à la main.
- **Mixins** : `zombie_expansion.mixins.json` ; `defaultRequire: 1` fait planter le chargement si une injection ne trouve pas sa cible. Actuel : `SpawnRestrictionMixin` (interdit le zombie vanilla en surface dans les biomes Givre).
- **Template** : généré avec le Fabric Template Mod Generator, option **Data generation** activée, **sans** « Split client and common sources ».
- **Environnements** : tout le code est dans `src/main/java`. Le code client (renderers, modèles, couches de modèle) va dans le sous-package `client/`, n'est jamais appelé depuis le code commun, et ses classes sont annotées `@Environment(EnvType.CLIENT)` si besoin. Un point d'entrée `client` (`ZombieExpansionClient`) sera ajouté à `fabric.mod.json` en même temps que la classe, au premier renderer.

### Data generation

- Point d'entrée : `ZombieExpansionDataGenerator` (`fabric-datagen`), qui enregistre les providers dans `onInitializeDataGenerator`.
- Commande : `./gradlew runDatagen`. Les fichiers sont générés dans `src/main/generated/` : **ne jamais les modifier à la main**, modifier le provider puis relancer la datagen.
- Tout ce qui peut être généré passe par un provider plutôt que par du JSON écrit à la main :
    - traductions : `FabricLanguageProvider` (un pour `en_us`, un pour `fr_fr`)
    - modèles d'items (œufs d'apparition, objets) : `FabricModelProvider`
    - loot tables des zombies : `SimpleFabricLootTableProvider` avec `LootContextTypes.ENTITY`
    - tags d'entités (régions, factions) : `FabricTagProvider.EntityTypeTagProvider`
    - recettes : `FabricRecipeProvider`
- Restent écrits à la main dans `src/main/resources` : textures, sons (`sounds.json`), `fabric.mod.json`, `zombie_expansion.mixins.json`. Les factions JSON pourront être générées plus tard par un `DataProvider` personnalisé.
- Après une modification de provider : relancer `runDatagen` **avant** `runClient`.

### Structure prévue

```
src/main/java/net/melvunx/zombieexpansion/
  ZombieExpansion.java        # ModInitializer : enregistrements
  ZombieExpansionDataGenerator.java  # point d'entrée de la datagen
  datagen/                    # providers (langues, modèles, loot, tags, recettes)
  entity/                     # classes des zombies et projectiles
  entity/ai/                  # goals personnalisés
  faction/                    # système de factions (chargé depuis les données)
  event/                      # hordes, raids de factions (PersistentState)
  registry/                   # ModEntities, ModItems, ModSounds...
  world/                      # spawn par biome (BiomeModifications) : FrostSpawns
  command/                    # commandes (/zombie_expansion biome)
  mixin/                      # SpawnRestrictionMixin
  client/                     # code client uniquement
    ZombieExpansionClient.java  # ClientModInitializer : renderers, modèles
    render/                   # EntityRenderer / EntityModel
src/main/resources/           # écrit à la main
  fabric.mod.json
  zombie_expansion.mixins.json
  assets/zombie_expansion/    # textures, icon.png, sons
src/main/generated/           # produit par runDatagen, ne pas éditer
  assets/zombie_expansion/    # lang (en_us, fr_fr), modèles d'items
  data/zombie_expansion/      # tags, loot tables, recettes
```

## Conventions

- Un zombie = une classe d'entité + un renderer + un modèle + une texture + un œuf d'apparition + une loot table + une règle de spawn.
- Toujours fournir les traductions anglaise **et** française, via les deux `FabricLanguageProvider`.
- Chaque nouvel ajout se termine par la mise à jour des providers concernés et un `runDatagen`.
- Respecter la gamerule `mobGriefing` pour tout mob qui casse ou modifie des blocs.
- Données configurables (factions, relations) en **JSON datapack** plutôt qu'en dur dans le code, pour faciliter les addons futurs.
- États visuels synchronisés client/serveur via `DataTracker` ; données persistantes via NBT (`writeCustomDataToNbt` / `readCustomDataFromNbt`).
- Tester chaque ajout en jeu (`runClient`) avant de passer au suivant.

## Contenu prévu et pistes techniques

| Zombie / système | Biome / lieu | Idée clé | Piste technique | Difficulté |
|---|---|---|---|---|
| Zombie gelé | Biomes enneigés | Coups qui gèlent | `frozenTicks`, attributs | ⭐ |
| Zombie aveugle | Cavernes, Deep Dark | Chasse au bruit, plus rapide | `GameEventListener` (vibrations comme le Warden), ciblage sans ligne de vue ; accroupi = indétectable | ⭐⭐ |
| Zombie caméléon | Jungle | Se fond dans le décor | V1 : texture camouflée + immobilité ; V2 : renderer teinté `BiomeColors` / transparence | ⭐⭐⭐ |
| Zombie lanceur | À définir | Lance ses membres | Entité projectile + `DataTracker` pour masquer le bras sur le modèle | ⭐⭐⭐ |
| Zombie général | Toutes régions | Renforce la horde selon le nombre | Comptage périodique des alliés proches + modificateurs d'attributs ; goal « suivre le leader » ; sa mort fait perdre les bonus | ⭐⭐⭐ |
| Zombie marchand pacifiste | Villages | Vend les items du mod, protège son maître | Interface `Merchant` ; ne pas hériter de `Monster` (pour l'Iron Golem) ; maître stocké par UUID ; goal de défense | ⭐⭐⭐⭐ |
| Hostilité régionale | — | Les zombies attaquent ceux d'autres régions | Tags d'entité par région + `ActiveTargetGoal` avec prédicat | ⭐⭐ |
| Factions | — | Alliances et guerres entre factions | Relations alliés/ennemis en JSON datapack, extensible aux autres mobs | ⭐⭐⭐ |
| Raids de factions | Villages | Raids zombies par vagues | Système maison (pas le `Raid` vanilla) : `PersistentState`, vagues, `ServerBossBar` | ⭐⭐⭐⭐⭐ |


### Faction du froid : « Givre »

**Biomes** : définis par le tag `zombie_expansion:frost_faction_spawns` (généré par `ModBiomeTagProvider`, donc modifiable par datapack). Snowy Plains, Ice Spikes, Snowy Taiga, Snowy Beach, Grove, Snowy Slopes, Jagged Peaks, Frozen Peaks, Windswept Hills, Windswept Gravelly Hills, Taiga, Old Growth Pine/Spruce Taiga, Frozen River, Frozen Ocean, Deep Frozen Ocean, Cold Ocean. **Windswept Forest** est mixte (tag `frost_faction_mixed_spawns`) : 70 % gelé / 30 % vanilla.

**Règles de spawn** :
- Le zombie gelé **remplace** le zombie vanilla dans ces biomes en copiant le poids et la taille de groupe d'origine du biome (95 presque partout, 100 en Old Growth Pine Taiga). Ne pas coder de poids en dur. Les variantes de la faction seront, elles, à poids bas.
- Les zombies villageois, noyés et husks restent vanilla.
- **Surface = gelé, sous terre = vanilla** : le gelé exige le ciel visible (`FrozenZombieEntity.canSpawn`), et `SpawnRestrictionMixin` refuse le zombie vanilla en surface dans le tag. Les biomes sont 3D : une grotte sous des Snowy Plains *est* Snowy Plains, c'est la visibilité du ciel qui distingue.
- La température d'un biome n'est **pas** un critère de spawn : Stony Shore et Windswept Hills valent tous deux 0,2 alors qu'ils ne vont pas dans le même camp. Toujours passer par les tags.
- `/zombie_expansion biome` affiche biome, température, région et poids actifs : l'utiliser pour vérifier un spawn.

C'est la **faction pilote** : elle sert à tester le système de factions sur un seul groupe de biomes avant de l'étendre aux autres régions.

| Zombie | Rôle | Idée clé | Piste technique | Difficulté |
|---|---|---|---|---|
| Zombie gelé | Base | Coups qui gèlent | `frozenTicks`, attributs | ⭐ |
| Lanceur de glace | Distance | Boules de neige tassées avec dégâts et gel | Projectile dérivé de la boule de neige + `ProjectileAttackGoal` | ⭐⭐ |
| Souffleur de blizzard | Distance / contrôle | Cône de vent qui repousse et réduit la visibilité | Zone d'effet devant lui + knockback + particules | ⭐⭐⭐ |
| Zombie stalactite | Embuscade (grottes glacées) | Accroché au plafond, fait tomber des pics de glace | Entité suspendue + projectile tombant, comme la stalactite pointue | ⭐⭐⭐ |
| Harponneur des glaces | Distance (océans gelés) | Harpon qui tire le joueur vers lui | Projectile relié + traction, comme la canne à pêche | ⭐⭐⭐⭐ |
| Cavalier d'ours polaire | Monture | Ours polaire zombie qui charge, cavalier qui frappe ou lance | Système de jockey vanilla ; le cavalier continue à pied si l'ours meurt | ⭐⭐⭐ |
| Cavalier de chèvre | Monture (montagnes) | Bonds sur les pentes, charges qui projettent dans le vide | Jockey + IA de saut de la chèvre | ⭐⭐⭐ |
| Meneur de traîneau | Monture | Traîneau tiré par des loups zombies | Entité traîneau + coordination de plusieurs mobs | ⭐⭐⭐⭐⭐ |
| Chaman du givre | **Chef (pas un boss)** | Soutien : armure de givre (absorption), murs de neige, soins | Pas de barre de boss, vie un peu plus élevée ; goal de fuite face au joueur ; sorts renforcés selon le nombre d'alliés (logique commune avec le zombie général) | ⭐⭐⭐⭐ |
| Zombie enseveli | Piège | Caché sous la neige poudreuse, surgit au passage | Entité invisible/immobile dans la neige poudreuse, détection de proximité | ⭐⭐ |
| Zombie dans la glace | Piège / ambiance (Ice Spikes) | Prisonnier d'un bloc de glace, se libère si la glace casse ou près d'une source de chaleur | Structure ou feature de spawn + détection de blocs de chaleur | ⭐⭐⭐ |

Règle de faction : tous les zombies Givre prennent **plus de dégâts du feu** et sont **ralentis dans les biomes chauds** (désert, badlands, savane).

### Idées supplémentaires (non validées)

- Zombie mineur (grottes) : creuse vers le joueur, lâche du minerai.
- Zombie de géode : armure de cristaux qui renvoie des dégâts, à briser à la pioche.
- Zombie sporifère (Lush Caves, champignons) : nuage de spores à la mort (`AreaEffectCloud`).
- Momie du désert : se reconstitue sauf si tuée par le feu.
- Zombie de marécage : caché sous l'eau, agrippe et ralentit.
- Zombie porte-torche : éteint ou vole les torches.
- Zombie infecteur : transforme les animaux tués en variantes zombies.
- Talisman de faction (vendu par le marchand) : rend une faction neutre envers le joueur.

## Feuille de route

1. Mise en place du projet (template Fabric, IntelliJ, `runClient` fonctionnel)
2. Premier zombie simple (zombie gelé) : entité, attributs, rendu, œuf, spawn par biome
3. Zombie aveugle (IA, goals, GameEvents)
4. Zombie lanceur (projectiles, DataTracker)
5. Tags régionaux, hostilité inter-zombies, puis factions en JSON (tester d'abord avec la faction Givre)
6. Zombie général et hordes
7. Zombie marchand pacifiste
8. Raids de factions

## Après le mod principal

Addons prévus pour étendre le même concept (variantes par biome + factions) aux autres mobs hostiles : squelettes, pillagers, etc. Le système de factions doit donc rester générique et ne pas dépendre de `ZombieEntity`.