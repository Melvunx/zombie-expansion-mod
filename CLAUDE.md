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
- La CI GitHub (`.github/workflows/build.yml`) build avec JDK 25, mais le code cible Java 21 (`options.release = 21`).

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
| Animations (optionnel) | GeckoLib, à décider avant de créer les premiers modèles animés |

- **mod_id** : `zombie_expansion` (ne plus le changer)
- **Package racine** : `net.melvunx.zombieexpansion`
- **Auteur** : melvunx · **Licence** : MIT
- **Entrées dans `fabric.mod.json`** : `main` (`ZombieExpansion`) et `fabric-datagen` (`ZombieExpansionDataGenerator`). `ZombieExpansion.id(path)` crée un `Identifier` du mod : l'utiliser plutôt que reconstruire les identifiants à la main.
- **Mixins** : `zombie_expansion.mixins.json` référence encore `ExampleMixin` (reste du template). Le retirer des deux endroits dès qu'il n'est plus utile ; `defaultRequire: 1` fait planter le chargement si une injection ne trouve pas sa cible.
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
  world/                      # spawn par biome (BiomeModifications)
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
5. Tags régionaux, hostilité inter-zombies, puis factions en JSON
6. Zombie général et hordes
7. Zombie marchand pacifiste
8. Raids de factions

## Après le mod principal

Addons prévus pour étendre le même concept (variantes par biome + factions) aux autres mobs hostiles : squelettes, pillagers, etc. Le système de factions doit donc rester générique et ne pas dépendre de `ZombieEntity`.