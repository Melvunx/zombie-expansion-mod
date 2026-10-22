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
- **Mixins** : `zombie_expansion.mixins.json` ; `defaultRequire: 1` fait planter le chargement si une injection ne trouve pas sa cible. Actuels : `SpawnRestrictionMixin` (interdit le zombie vanilla en surface dans les biomes Givre) et `ZombieEntityMixin` (zombie en neige poudreuse → zombie gelé). **Piège** : un `@Shadow` ne retrouve pas un champ hérité (ex. `Entity#inPowderSnow`) depuis un mixin sur une sous-classe ; l'erreur n'apparaît qu'au runtime (la datagen la révèle, pas `compileJava`).
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
- Référencer un tag **vanilla** depuis un `FabricTagProvider` demande `forceAddTag` (pas `addTag`), sinon la datagen échoue avec « missing following references ».
- Restent écrits à la main dans `src/main/resources` : textures, sons (`sounds.json`), `fabric.mod.json`, `zombie_expansion.mixins.json`. Les factions JSON pourront être générées plus tard par un `DataProvider` personnalisé.
- Après une modification de provider : relancer `runDatagen` **avant** `runClient`.
- **Piège Blockbench / GeckoLib (animations)** : Blockbench exporte les temps entiers (`"0"`, `"1"`, `"3"`) **avant** les temps décimaux (`"0.25"`), et GeckoLib lit les keyframes dans l'ordre du fichier sans les trier. Résultat : une animation qui ne joue pas ou saute (c'est ce qui a cassé `walk` et `idle` du zombie lanceur). Après chaque export, lancer `python3 tools/sort_keyframes.py src/main/resources/assets/zombie_expansion/animations/entity/*.json` (ajouter `--check` pour seulement vérifier).

### Structure prévue

```
src/main/java/net/melvunx/zombieexpansion/
  ZombieExpansion.java        # ModInitializer : enregistrements
  ZombieExpansionDataGenerator.java  # point d'entrée de la datagen
  datagen/                    # providers (langues, modèles, loot, tags, recettes)
  entity/                     # classes des zombies et projectiles
  entity/ai/                  # goals personnalisés (InvestigateVibrationGoal, SearchGoal, ThrowArmGoal)
  effect/                     # effets de statut (GluedStatusEffect), enregistrés dans registry/ModStatusEffects
  faction/                    # Faction (enum liée à un tag d'entités), FrostVulnerability ; relations à venir en JSON
  event/                      # hordes, raids de factions (PersistentState)
  registry/                   # ModEntities, ModItems, ModSounds...
  world/                      # spawn par biome (BiomeModifications) : FrostSpawns, AbyssSpawns, UnearthedSpawns
  command/                    # /zombie_expansion biome | faction | nearby [rayon] | info (racine unique dans ModCommands)
  mixin/                      # SpawnRestrictionMixin
  client/                     # code client uniquement
    ZombieExpansionClient.java  # ClientModInitializer : renderers, modèles
    render/                   # EntityRenderer
    model/                    # GeoModel (FrozenZombieModel : choisit l'animation bébé)
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
| Zombie aveugle (*The Blinded*) | Deep Dark (cœur), puis tout l'Overworld sous Y -8 (comme l'Enderman) | Chasse au bruit, plus rapide | `GameEventListener` (vibrations comme le Warden), ciblage sans ligne de vue ; accroupi = indétectable | ⭐⭐ |
| Zombie caméléon | Jungle | Se fond dans le décor | V1 : texture camouflée + immobilité ; V2 : renderer teinté `BiomeColors` / transparence | ⭐⭐⭐ |
| Zombie lanceur | Faction Les Déterrés (partie « facile » du mod) : surface et grottes | Lance ses membres, effet Englué (Lenteur 3 s + bave verte) | Entité projectile + `DataTracker` pour masquer le bras sur le modèle | ⭐⭐⭐ |
| Chef de horde (le « général » des Déterrés) | Surface uniquement, hors biomes du Givre | Mène des sous-fifres et donne de légers bonus aux autres Déterrés proches, par impulsions avec temps de recharge | Comptage périodique des alliés proches + modificateurs d'attributs temporaires ; goal « suivre le leader » ; sa mort fait perdre les bonus ; ses sous-fifres sont exclus des bonus. Chaque faction aura son propre chef (chaman, prophète) | ⭐⭐⭐ |
| Zombie marchand pacifiste | Villages | Vend les items du mod, protège son maître | Interface `Merchant` ; ne pas hériter de `Monster` (pour l'Iron Golem) ; maître stocké par UUID ; goal de défense | ⭐⭐⭐⭐ |
| Hostilité régionale | — | Les zombies attaquent ceux d'autres régions | Tags d'entité par région + `ActiveTargetGoal` avec prédicat | ⭐⭐ |
| Factions | — | Alliances et guerres entre factions | Relations alliés/ennemis en JSON datapack, extensible aux autres mobs | ⭐⭐⭐ |
| Raids de factions | Villages | Raids zombies par vagues | Système maison (pas le `Raid` vanilla) : `PersistentState`, vagues, `ServerBossBar` | ⭐⭐⭐⭐⭐ |


### Factions : tags et commandes

- **Une faction = un tag d'entités** (`ModEntityTypeTags`, générés par `ModEntityTypeTagProvider`) : `zombie_expansion:frost_faction`, `abyss_faction`, `unearthed_faction` (Les Déterrés). Ajouter un membre = l'ajouter au tag, jamais un `instanceof`. L'enum `faction/Faction` lit ces tags.
- **Position → faction dominante** (`Faction.at`) : les Abysses sous Y -8 ou dans le tag de biomes `abyss_faction_spawns` (Deep Dark), puis le Givre dans ses biomes, sinon les Déterrés.
- **Commandes** (racine unique `/zombie_expansion`, branches assemblées dans `ModCommands`) : `biome` (biome, température, région, poids actifs), `faction` (faction ici et ses membres), `nearby [rayon]` (membres de chaque faction autour de soi, plus le plafond de densité des aveugles), `info` (fiche de la créature visée : faction, vie, vitesse, cible, état). Sans permission requise, `info` est réservée à un joueur.
- **À faire** : les relations alliés/ennemis entre factions, en JSON de datapack.

### Faction du froid : « Givre »

**Biomes** : définis par le tag `zombie_expansion:frost_faction_spawns` (généré par `ModBiomeTagProvider`, donc modifiable par datapack). Snowy Plains, Ice Spikes, Snowy Taiga, Snowy Beach, Grove, Snowy Slopes, Jagged Peaks, Frozen Peaks, Windswept Hills, Windswept Gravelly Hills, Taiga, Old Growth Pine/Spruce Taiga, Frozen River, Frozen Ocean, Deep Frozen Ocean, Cold Ocean. **Windswept Forest** est mixte (tag `frost_faction_mixed_spawns`) : 70 % gelé / 30 % vanilla.

**Règles de spawn** :
- Le zombie gelé est **ajouté** avec le poids et la taille de groupe d'origine du zombie du biome (95 presque partout, 100 en Old Growth Pine Taiga). Ne pas coder de poids en dur. Le zombie vanilla **reste dans la liste** (c'est lui qui apparaît sous terre) ; il est refusé en surface par le mixin. Seul le biome mixte réduit le vanilla à 30 %. Les variantes de la faction seront, elles, à poids bas.
- Les zombies villageois, noyés et husks restent vanilla.
- **Surface = gelé, sous terre = vanilla** : le gelé exige le ciel visible (`FrozenZombieEntity.canSpawn`), et `SpawnRestrictionMixin` refuse le zombie vanilla en surface dans le tag. Chaque position n'accepte donc qu'un des deux, l'autre est une tentative perdue. Les biomes sont 3D : une grotte sous des Snowy Plains *est* Snowy Plains, c'est la visibilité du ciel qui distingue.
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

Règles de faction :
- **Faiblesse au feu** : dégâts de feu ×2, boules de feu (ghast, blaze, charge de feu) ×3, non cumulés. Centralisé dans `faction/FrostVulnerability.amplify()` ; chaque membre l'appelle depuis son `damage()`.
- **Insensibles au gel** : tag vanilla `freeze_immune_entity_types` (généré par `ModEntityTypeTagProvider`), sinon un zombie transformé en neige poudreuse y subirait aussitôt du gel.
- **Ralentis dans les biomes chauds** : -30 % de vitesse (`FrostVulnerability.updateHeatSlowdown`, modificateur temporaire). « Chaud » = tag `zombie_expansion:hot_biomes` (désert, `#is_badlands`, `#is_savanna`, `#is_nether`), pas un seuil de température.
- **Retour en zombie** : un zombie gelé resté **140 ticks** en biome chaud puis **300 ticks** de conversion visible (tremblement via `isShaking`, flag synchronisé par `DataTracker`) redevient un zombie vanilla. Timings calqués sur `SkeletonEntity` (squelette → Stray), annulé dès qu'il quitte le biome chaud, persisté en NBT (`ZombieConversionTime`). La conversion inverse (zombie → gelé en neige poudreuse) reste, elle, à 100 ticks.
- **Transformation** : un zombie vanilla (adulte ou bébé) resté 100 ticks dans la neige poudreuse devient un zombie gelé (même flag bébé, équipement et nom conservés). Seul `EntityType.ZOMBIE` est concerné (pas husk, noyé).
- **Bébé** : même mécanique que le zombie vanilla (`isBaby()`, 5 % au spawn). Le renderer réduit le modèle à 50 % (`scaleModelForRender`) ; GeckoLib ne le fait pas seul.

### Faction des profondeurs : « Abysses »

Zone : **entre Y -8 et Y -64** (couche de deepslate), cœur de faction dans le **Deep Dark**. Thème : ténèbres, sculk, minerais des profondeurs, lien avec le Warden.
Objectif de design : c'est la faction **la plus puissante**. L'exploration des profondeurs doit être nettement plus difficile qu'en vanilla, tout en restant juste (contre-mesures possibles : marcher accroupi, laine, armure d'améthyste).

**Relations** : ennemie des futures factions **Lush Caves** et **Dripstone**, qui sont alliées entre elles. Combats attendus aux frontières des zones.

| Zombie | Rôle | Capacités validées | Piste technique | Difficulté |
|---|---|---|---|---|
| Zombie aveugle | Base | Chasse au son, plus rapide qu'un zombie normal ; **10 % de chance** d'infliger un court effet **Darkness** à chaque coup | `GameEventListener`, ciblage sans ligne de vue ; `StatusEffects.DARKNESS` sur `tryAttack` | ⭐⭐ |
| Lanceur d'améthyste | Distance | Zombie couvert d'améthyste, tire à l'**arbalète d'améthyste** avec des éclats d'améthyste | `CrossbowAttackGoal` / `CrossbowUser` + projectile `AmethystShardEntity` (partagé avec l'item joueur) | ⭐⭐⭐ |
| Zombie incrusté | Variantes de minerai | Une entité, plusieurs variantes : **diamant** (très résistant), **redstone** (faisceau électrique), **lapis** (vole de l'XP à chaque coup), **or** (rapide, vole et fuit) | Variante stockée en `DataTracker` + NBT ; le renderer choisit la texture selon la variante | ⭐⭐⭐ |
| ↳ variante redstone | Distance courte | **Faisceau de redstone** façon « Kamé Hamé Ha » : portée **~4 blocs**, dure **2 s**, **dégâts en continu** | Raycast chaque tick pendant 40 ticks, dégâts par tick aux entités touchées, particules de poussière de redstone ; temps de recharge à définir | ⭐⭐⭐ |
| Mineur des profondeurs | Voleur | Variante des profondeurs du **mineur zombie** (faction des Déterrés), **plus rapide** ; vole les minerais de l'inventaire et fuit, les rend à sa mort | Goal de fuite + inventaire interne sauvegardé en NBT | ⭐⭐⭐ |
| Éteigneur | Contrôle | Variante des profondeurs du **voleur de torche** (faction des Déterrés) : éteint les torches autour de lui | Recherche de blocs de lumière proches + suppression (respecter `mobGriefing`) | ⭐⭐ |
| Prophète du Sculk | **Chef (pas un boss)** | Voir détail ci-dessous | Logique de chef commune (général, chaman) | ⭐⭐⭐⭐ |

**Zombie aveugle** (implémenté, `BlindedZombieEntity`, id `blinded_zombie`, nom EN « The Blinded », assets `the_blinded.*`) :
- **Spawn** : origine Deep Dark (poids 20), mais présent **dans tout l'Overworld sous Y -8**, comme l'Enderman. Groupes de 2 à 3. Sous terre il représente **20 % des zombies** (80 % zombie vanilla) : son poids vaut 25 % du poids du zombie du biome (`AbyssSpawns.blindedWeight`, donc 24 pour 95, et réduit en biome mixte). Seuls les biomes ayant déjà des monstres reçoivent l'entrée (biome champignon exclu, biomes de mods respectés). Aucun bébé (`setBaby` neutralisé).
- **Ancient City** : la structure vanilla interdit tout spawn de monstre sur sa boîte englobante (`spawn_overrides.monster = []`), donc rien n'y apparaît, même dans le biome Deep Dark. Choix de design vanilla, non contourné pour l'instant.
- **Tester** : le mode Spectateur désactive le spawn autour du joueur ; utiliser Survie ou Créatif.
- **Plafond de densité** : pas de spawn naturel s'il y a déjà 5 aveugles à moins de 24 blocs (`MAX_NEARBY`, `DENSITY_RADIUS`). Le plafond de monstres vanilla est partagé entre espèces et ne limiterait rien dans le Deep Dark, où ils sont seuls.
- **Stats** : 20 PV (comme le zombie vanilla), vitesse ×1,15 (0,23 → 0,2645), portée d'écoute **12 blocs** (`getListeningRange()`, point d'accroche de l'effet Ouïe aiguisée).
- **Écoute** : aucun `ActiveTargetGoal`. Mêmes événements que le Warden (tag `warden_can_listen`) via `Vibrations`, mais **seul le joueur** est entendu : ses pas, sa monture, ses projectiles et les objets qu'il lâche (même logique que le Sculk Shrieker). Un projectile ou un objet désigne un lieu, pas une cible : il attire vers l'impact sans cibler le lanceur (`InvestigateVibrationGoal`), puis il cherche sur place (`SearchGoal`). Accroupi = pas de vibration de pas = indétectable.
- **Alliés** : ne cible jamais un membre de `abyss_faction` ni de `unearthed_faction` (Déterrés, zombies vanilla compris ; `canTarget`, par tags), sauf cible portant l'effet Provocation (`isProvoked`, à brancher quand l'effet existera).
- **Mort discrète** : un coup d'un joueur accroupi supprime les événements `ENTITY_DAMAGE` / `ENTITY_DIE`, donc les autres aveugles n'entendent rien.
- **Les shriekers** : il ne les active pas (vanilla : un shrieker n'accepte que les vibrations d'un joueur).
- **Darkness** : 10 % par coup, 100 ticks (durée à valider).
- **Loot** : chair putréfiée 0-2 (toujours) et **fragments d'écho 0-2 uniquement si le joueur porte le coup mortel** (`killed_by_player`, moyenne 1 par mort). C'est une récompense, pas un farm à la lave.
- **Sons** : à ajouter (entend un bruit, recherche, attaque, mort). Pas encore de `sounds.json`.
- **Vitesse d'animation** : les animations sont chronométrées, pas liées à la distance parcourue. `walk` et `walk_grope` sont lues ×1,15 (`SPEED_BONUS`, le même facteur que le déplacement, via `setAnimationSpeedHandler`) ; run, idle et recherches restent à vitesse normale. Valeur à ajuster en jeu si besoin.
- **Animations** (état synchronisé par `DataTracker` `BEHAVIOR`) : `idle`, `walk` (errance), `walk_grope` (en route vers un bruit), `run` / `run_frenzy` (poursuite, variante tirée au hasard à chaque nouvelle poursuite, 50 %), `attack_slam` / `attack_grab_bite` / `attack_swipe` (au hasard à chaque attaque), `search_sniff` / `search_listen` / `search_ground` (au hasard, 3 à 5 s, à l'arrivée sur un bruit). Les trois animations de recherche sont bouclées : ce sont des états.
- **Texture** : `textures/entity/the_blinded.png` (64×64).

**Prophète du Sculk** : zombie couvert de sculk avec un catalyseur dans la poitrine. Pas de barre de boss, reste en retrait et fuit le joueur.
- Donne aux **zombies aveugles** proches un effet personnalisé **Ouïe aiguisée** qui augmente la portée de leur écoute.
- Augmente la **vie** et la **vitesse** de tous les zombies Abysses aux alentours.
- Lance périodiquement (**pas en continu**) une salve de debuffs sur les joueurs et ennemis proches : **Faiblesse 10 s**, **Lenteur 10 s**, **Darkness 5 s**.
- Répand du sculk quand un zombie meurt près de lui, comme un catalyseur.
  **Spawn** : le Deep Dark n'a aucun spawn de monstres en vanilla. Ajouter les spawns avec `BiomeModifications.addSpawn` et une restriction personnalisée (`SpawnRestriction`) qui vérifie `pos.getY() <= -8`. Surveiller la fréquence pour ne pas rendre la zone invivable.

**Loot** :
- Zombies du Deep Dark : chair putréfiée + **0 à 2 fragments d'écho** (seulement sur un kill par le joueur).
- Lanceur d'améthyste : chair putréfiée + **éclats d'améthyste** + faible chance de lâcher son **arbalète d'améthyste**.
- Mineur des profondeurs : chair putréfiée + **1 % de chance pour chacun** des 5 localisateurs de minerai.
  **À définir** : durée exacte du Darkness du zombie aveugle ; temps de recharge des salves du Prophète et du faisceau de redstone ; taux de drop de l'arbalète ; si les tirs du lanceur d'améthyste appliquent Surbrillance et Provocation au joueur ; « pression des profondeurs » (bonus de stats selon le Y) à valider.

### Faction « Les Déterrés » (*The Unearthed*)

Faction des zombies « classiques », **la première que le joueur découvre**. Tag : `zombie_expansion:unearthed_faction`, `Faction.UNEARTHED`.

**Charte de la faction** (ce qui doit rester vrai pour tout futur membre) :
- **Early game** : pas trop fort, mais différent du zombie vanilla. Les Déterrés sont l'apprentissage du mod : pas de mécanique qui demande déjà une contre-mesure spécialisée (pas d'effet lourd, pas de gimmick de la partie « difficile »).
- **Budget de puissance** : au plus 20 PV et des dégâts de mêlée au plus égaux à ceux du zombie vanilla. Un membre peut être plus faible ou plus gênant, pas plus fort.
- **Exceptions validées** au budget (les seules), voir « Stats des exceptions » ci-dessous : le **Gonflé** (stats, dégâts et Nausée), le **Chef de horde** (stats et dégâts), le **Zombie bûcheron** (dégâts, à cause de la hache) et le **Sous-fifre sauvage** (dégâts, à cause de l'épée). Toute nouvelle exception doit être validée explicitement.
- **Spawn** : **à la surface et dans les grottes jusqu'à Y -8**, sans priorité entre les deux (une seule exception : le Chef de horde, surface uniquement). En dessous de Y -8, ce sont les Abysses (et le zombie aveugle, 20 % des zombies).
- **Rareté** : poids bas par rapport au zombie vanilla du biome, comme les variantes du Givre : le zombie vanilla reste la référence.
- **Alliés** : le zombie aveugle ne les cible jamais (sauf Provocation).

**Frontière avec les Abysses** : la limite est **Y -8**, sans zone neutre. Au-dessus de Y -8 (surface et grottes) : Déterrés ; à Y -8 et en dessous : Abysses. Les combats entre factions se feront donc autour de cette couche.

**Règles de spawn des Déterrés** :
- **Surface et grottes** jusqu'à Y -8 pour tous les membres, **sauf le Chef de horde : surface uniquement**.
- **Biomes du Givre** : la surface appartient au zombie gelé (voir Givre). Les Déterrés n'y apparaissent que **dans les grottes sous ces biomes**. Conséquence : le Chef de horde, qui ne spawn qu'en surface, n'apparaît dans aucun biome du Givre.
- **Plafonnement** (`UnearthedSpawns`) : chaque membre a une **part du poids du zombie vanilla du biome** (cible 5 à 10 %, lanceur : 8 % soit un poids de 8 pour 95), et la **somme des parts est plafonnée à 35 %** (`TOTAL_SHARE_CAP`, vérifiée au chargement : le mod refuse de démarrer si elle est dépassée). Les Déterrés modés font donc au plus ~26 % des zombies d'un biome. Seuls les biomes ayant un zombie reçoivent l'entrée. Chiffres à régler en jeu.
- **Nouvelles factions** : à chaque ajout d'une faction, **demander** à Melvin si les Déterrés doivent aussi apparaître dans les grottes sous ses biomes de surface. Ne pas le décider seul.

**Principe de design** : chaque membre apprend au joueur **un réflexe de base** de Minecraft (contourner, reculer, tuer de loin, viser le chef, protéger sa base…). Il doit y avoir plusieurs manières de les affronter.

Membres actuels : zombie vanilla et zombie villageois (tag `unearthed_faction`, tels que le jeu les fournit). Le husk et le noyé n'en font **pas** partie.

**Membres validés** :

| Zombie | Rôle | Idée clé | Piste technique | Difficulté |
|---|---|---|---|---|
| Zombie lanceur | Distance | Lance ses bras ; le joueur touché reçoit l'effet **Englué** (Lenteur **3 s** + bave verte à l'écran) | Projectile (bras) + `DataTracker` pour masquer le bras lancé sur le modèle ; effet Englué (voir Items prévus) | ⭐⭐⭐ |
| Zombie archer / arbalétrier | Distance | Une seule entité : spawne **avec un arc ou une arbalète** (au hasard). IA proche du squelette, **fuit si le joueur s'approche** | Arme donnée dans `initialize` (`equipStack`) ; `BowAttackGoal` ou `CrossbowAttackGoal` + `CrossbowUser` ; `FleeEntityGoal` sous ~5 blocs ; moins précis que le squelette (early game) ; pose de bras de tir dans le renderer | ⭐⭐ |
| Chef de horde | **Chef (pas un boss)** | Le « général » des Déterrés : mène des sous-fifres et donne de légers bonus aux autres Déterrés proches, par impulsions avec temps de recharge. **Surface uniquement** | **Exception au budget** : 30 PV, 3 points d'armure, +0,5 cœur de dégâts par rapport au zombie vanilla. Spawne avec 2 à 4 sous-fifres ; goal « suivre le leader » ; bonus **plus légers que ceux d'un vrai général**, appliqués par impulsions (durée et temps de recharge à régler en jeu), perdus à sa mort ; **les sous-fifres (sauvages ou du joueur) ne reçoivent jamais ces bonus**. Logique de chef commune (chaman, prophète) | ⭐⭐⭐ |
| Sous-fifre | Mêlée faible | Accompagne le chef : **14 PV**, spawne avec une **épée en bois** (exception sur les dégâts). Sauvage : brûle au soleil ; celui du joueur : non | Voir section « Sous-fifre du joueur » : même entité, hostile tant qu'elle n'a pas de propriétaire | ⭐⭐ (sauvage) |
| Zombie au bouclier | Mêlée | Bloque les attaques de face. Contre : le contourner, ou une **hache** qui désactive le bouclier (mécanique vanilla) | Bouclier en main secondaire ; blocage géré par la mécanique vanilla `isBlocking` / désactivation par la hache | ⭐⭐ |
| Coureur | Mêlée | Maigre et rapide, **12 PV**. Oblige à se battre en reculant | Attributs (vitesse plus haute, vie plus basse) | ⭐ |
| Gonflé | Mêlée / zone | Lent, résistant au recul ; à sa mort, éclate en un nuage de **Nausée I pendant 5 s** pour le joueur touché. Contre : le tuer de loin ou s'écarter | **Exception au budget** : 35 PV, 4 points d'armure, dégâts selon la difficulté (voir ci-dessous) ; `AreaEffectCloud` à la mort (`onDeath`) avec `StatusEffectInstance(NAUSEA, 100, 0)` | ⭐⭐ |
| Zombie fouisseur | Embuscade | **Sort de terre** (herbe, terre, gravier, pierre de grotte) près du joueur, la nuit et dans les grottes. Zombie emblème des Déterrés | Animation d'émergence GeckoLib (comme le Warden) ; invulnérable et immobile pendant l'émergence ; particules du bloc en dessous | ⭐⭐⭐ |
| Zombie bûcheron | Siège | Armé d'une hache, **casse les portes en bois quelle que soit la difficulté** (vanilla : Difficile seulement) | `BreakDoorGoal` avec prédicat de difficulté toujours vrai ; respecter `mobGriefing`. **Exception au budget** sur les dégâts (la hache dépasse l'épée) : dégâts fixés par `DifficultyDamage` | ⭐⭐ |
| Mineur zombie | Creuseur | **Casse des blocs uniquement pour atteindre le joueur**. Il **ne vole pas** : le vol est réservé au mineur des profondeurs (Abysses, plus rapide) | Respecter `mobGriefing` ; loot éventuel à définir | ⭐⭐⭐ |
| Voleur de torche | Contrôle | Éteint ou vole les torches (sa variante des profondeurs est l'Éteigneur des Abysses) | Recherche de blocs de lumière proches ; respecter `mobGriefing` | ⭐⭐ |
| Zombie de géode | Mêlée | Armure de cristaux qui renvoie des dégâts, à briser à la pioche | — | ⭐⭐⭐ |
| Zombie infecteur | Soutien | Transforme les animaux tués en variantes zombies | — | ⭐⭐⭐ |

**Zombie lanceur** (implémenté, `ThrowerZombieEntity`, id `thrower_zombie`, nom EN « Thrower Zombie », FR « Zombie lanceur ») :
- **Stats** : celles du zombie vanilla (`createZombieAttributes`), aucun bébé. Il garde la mêlée vanilla ; seul un goal de tir s'ajoute.
- **Tir** (`ThrowArmGoal`, priorité 1) : cible visible entre **4 et 12 blocs** et bras présent (ni lancé, ni en repousse) → il s'immobilise **0,75 s** (visée, le joueur peut réagir), puis lance son bras. **À moins de 4 blocs** (`MELEE_RANGE`, abaissé de 5 : la zone morte était trop grande) il se bat au corps à corps, et si le joueur se rapproche pendant la visée le tir est annulé ; au-delà de 4 blocs il retire de loin. Imprécision de 6° (early game). Piste si trop facile à coller : lui faire garder ses distances quand il est armé.
- **Bras** : une fois lancé, le bras droit disparaît du modèle (`DataTracker` `ARM_THROWN`, lu par `ThrowerZombieModel`) et repousse ainsi : un décompte de **4 s** (`ARM_REGROW_DELAY_TICKS`, persisté en NBT `ArmRegrowTicks`) qui **ne tourne que si la cible n'est pas à moins de 4 blocs** (collé au joueur, il n'a pas le temps de repousser), puis l'animation de repousse (1,2 s ou 0,7 s, au hasard) pendant laquelle le bras est de nouveau visible mais inutilisable ; ensuite il peut tirer. Pas d'item à ramasser.
- **Mêlée** : chaque coup qui touche a **30 %** de chance d'appliquer Englué 3 s (`MELEE_GLUED_CHANCE`), pour que coller le lanceur ne soit pas toujours la bonne réponse.
- **Son** : le lancer joue le son vanilla du lancer d'œuf (`ENTITY_EGG_THROW`), provisoire, en attendant un son dédié (`sounds.json`).
- **Projectile** (`ThrownArmEntity`, dérive de `ThrownItemEntity`) : **2 PV** d'impact + effet **Englué 3 s**, sur **toute créature** touchée (pas seulement le joueur), avec les mêmes valeurs. Il **traverse les membres de la faction du lanceur** (lue par tag sur le type du lanceur, repli sur Les Déterrés si le lanceur a disparu) : ni dégâts ni effet. **Enderman** : comme avec une flèche, il se téléporte (son `damage` renvoie vrai sans dégâts, donc exclu explicitement de l'effet). Traînée de particules de potion vertes en vol (client, `ENTITY_EFFECT` de la couleur de l'effet) et éclaboussure de bave à l'impact.
- **Effet Englué** (`GluedStatusEffect`, `ModStatusEffects.GLUED`) : **-30 % de vitesse** (Lenteur I vanilla : -15 %), retiré par le lait. Overlay de bave verte côté client (`client/GluedOverlay`, `HudRenderCallback`, texture `textures/misc/glued_overlay.png` générée par script), qui s'estompe sur la dernière seconde.
- **Rendu GeckoLib** (modèles, animations et textures faits dans Blockbench, version simple sans moignon) :
  - `geo/entity/thrower_zombie.geo.json` : os `head`, `headwear`, `body`, `left_arm`, `right_arm`, `left_leg`, `right_leg` (racine `thrower zombie`). Le renderer (`ThrowerZombieRenderer`) masque l'os `right_arm` quand `ARM_THROWN` est vrai (`preRender`, `model.getBone(...).setHidden`). Le modèle étant partagé entre instances, la visibilité est réglée avant chaque rendu.
  - `animations/entity/thrower_zombie.animation.json` : `idle`, `walk` (bouclées), `aim` (0,75 s, `hold_on_last_frame`, = le temps de visée du goal, joué tant que le `DataTracker` `AIMING` est vrai ; `LoopType.DEFAULT` pour reprendre le hold du JSON) et `throw` (0,4 s, déclenchée par `triggerAnim`, lâcher à 0,12 s).
  - **Lâcher synchronisé** : `throwArm` lance l'animation, mais le projectile part et le bras disparaît **2 ticks plus tard** (`THROW_RELEASE_TICKS`, ≈ 0,12 s), au moment où la main le lâche. Le goal ne relance rien pendant ce délai (`isThrowing`).
  - `textures/entity/thrower_zombie.png` (64×64, disposition « legacy » : jambe et bras gauches = UV droits en miroir, donc ils partagent la même bave).
  - Projectile : `ThrownArmEntity` est un `GeoEntity` (base `ThrownItemEntity` conservée pour la physique et le réseau), rendu par `ThrownArmRenderer` avec `geo/entity/thrown_arm.geo.json`, `animations/entity/thrown_arm.animation.json` (`spin`, 0,4 s bouclée) et `textures/entity/thrown_arm.png` (16×16). GeckoLib n'oriente que les créatures : le renderer lui donne le lacet interpolé du projectile (le tangage est inutile, le spin est autour de X).
  - **Limites** : pas de calque d'armure ni d'objet tenu (réglé en interdisant au lanceur de ramasser des objets), sons à faire.
  - **Animations de mêlée et de repousse** (branchées ; chacune jouée une fois par `triggerAnim` sur son contrôleur, la variante est tirée au hasard) :
    - Mêlée à **deux bras** (bras droit présent) : `attack_both` (0,6 s, coup à 0,3 s) ou `attack_both_alt` (0,7 s, tenaille, coup à 0,4 s).
    - Mêlée à **un bras** (bras lancé ou en repousse) : `attack_single` (0,5 s, coup à 0,25 s) ou `attack_single_alt` (0,6 s, revers latéral, coup à 0,3 s).
    - **Repousse** : `regrow` (1,2 s) ou `regrow_burst` (0,7 s, « super régénération »). Elles animent l'échelle de `right_arm` de 0,01 à 1 : `ARM_THROWN` repasse donc à faux **au début** de l'animation. Le goal ne tire pas avant sa fin (`isRegrowing`, 24 ou 14 ticks).
    - L'animation d'attaque démarre quand `tryAttack` est appelé, alors que les dégâts sont infligés tout de suite : le « coup » visuel (0,25 à 0,4 s) arrive après les dégâts. À ajuster si ça gêne.
  - **Plus tard** : version avec moignon (couper `right_arm` en deux os, `right_arm_lower` masqué seul) et animation `regrow`.
- **Spawn** : règle commune `Faction.allowsUnearthedSpawn` (surface et grottes, Y > -8, sous terre uniquement dans les biomes du Givre, jamais dans le Deep Dark). Entrée de spawn ajoutée par `UnearthedSpawns` (part de 8 %, groupes de 1 à 2).
- **Loot** : chair putréfiée 0-2.
- **Pas d'objets ni d'équipement** : `canGather` renvoie faux, pas d'équipement initial, citrouille d'Halloween retirée. Le modèle n'affiche ni armure ni objet tenu ; cela règle la limite correspondante.

#### Stats des exceptions

1 cœur = 2 PV. Le zombie vanilla sert de référence : 20 PV, 2 points d'armure, dégâts finaux contre le joueur de 2,5 / 3 / 4,5 PV (Facile / Normal / Difficile).

| Zombie | PV | Armure | Dégâts Facile | Dégâts Normal | Dégâts Difficile |
|---|---|---|---|---|---|
| Zombie vanilla (référence) | 20 | 2 | 2,5 PV (1,25 ❤) | 3 PV (1,5 ❤) | 4,5 PV (2,25 ❤) |
| **Gonflé** | **35** | **4** | **3 à 4 PV** (1,5 à 2 ❤) | **4 à 5 PV** (2 à 2,5 ❤) | **6 à 8 PV** (3 à 4 ❤) |
| **Chef de horde** | **30** | **3** | **3,5 PV** (vanilla + 0,5 ❤) | **4 PV** (vanilla + 0,5 ❤) | **5,5 PV** (vanilla + 0,5 ❤) |

**Autres exceptions** (dégâts uniquement, valeurs **à fixer**) :

| Zombie | Pourquoi | Dégâts |
|---|---|---|
| **Zombie bûcheron** | La hache fait plus de dégâts qu'une épée | À fixer (proposition : vanilla + 0,5 ❤, comme le chef) |
| **Sous-fifre sauvage** (14 PV, épée en bois) | L'épée en bois ajoute des dégâts à l'attribut de base | À fixer (proposition : dégâts du zombie vanilla) |

**Nausée du Gonflé** : exception à la règle « pas d'effet lourd », limitée à **Nausée niveau I (amplificateur 0) pendant 5 s**, uniquement pour le joueur touché par le nuage.

- **PV et armure** : attributs `GENERIC_MAX_HEALTH` et `GENERIC_ARMOR` dans `createAttributes()`.
- **Dégâts** : ce sont les **dégâts finaux reçus par le joueur**, déjà par difficulté. Ils ne peuvent pas venir du seul attribut `GENERIC_ATTACK_DAMAGE`, car le jeu le rééchelonne contre un joueur (Facile : `min(x/2 + 1, x)`, Difficile : `x × 1,5`), ce qui ne donne ni les fourchettes du Gonflé ni un +0,5 cœur constant pour le chef.
  - Piste : surcharger `tryAttack`, calculer le montant selon `world.getDifficulty()` (tirage aléatoire dans la fourchette pour le Gonflé, vanilla + 1 PV pour le chef), et l'infliger avec un **type de dégâts du mod sans mise à l'échelle** (`scaling: never`, déclaré via un `FabricDynamicRegistryProvider` en datagen, avec un message de mort traduit). Centraliser le calcul dans une petite classe commune (ex. `DifficultyDamage`).
  - En **Paisible**, pas de monstres, donc rien à définir.
  - Contre une **autre créature** (combats entre factions), le jeu ne met pas les dégâts à l'échelle : utiliser la valeur **Normal**.
- Le **recul** et les **bonus de chef** restent à part (recul : `GENERIC_KNOCKBACK_RESISTANCE` élevé pour le Gonflé).

**Loot du chef de horde** : chair putréfiée + **10 % de chance** de lâcher l'**item d'invocation du sous-fifre**, uniquement si le joueur porte le coup mortel (`killed_by_player`). Les sous-fifres ne le lâchent pas : il faut tuer le chef.

#### Sous-fifre du joueur

Obtenu avec l'item d'invocation lâché par le chef de horde. L'item fonctionne **comme une Poké Ball** : il contient un sous-fifre, qu'on invoque et qu'on rappelle.

**Version 1 (à implémenter)** :
- **Une seule entité** pour le sous-fifre sauvage et celui du joueur : un champ **propriétaire** (UUID, NBT) optionnel. Sans propriétaire : hostile, membre de la horde. Avec propriétaire : il suit le joueur et le défend.
- **Invocation / rappel** : clic droit sur un bloc avec l'item → le sous-fifre apparaît avec le joueur comme propriétaire. Clic droit sur son sous-fifre avec l'item → il retourne dedans. L'item n'est pas empilable (1 item = 1 sous-fifre). Les données du sous-fifre (vie, équipement, inventaire, améliorations) sont stockées **dans l'item** via un composant de données (1.21 : `DataComponentTypes.CUSTOM_DATA` ou un `ComponentType` du mod), pour qu'il garde tout entre deux invocations.
- **Soleil** : le sous-fifre **sauvage brûle** comme un zombie normal ; celui **du joueur ne brûle pas** (`burnsInDaylight()` renvoie `false` seulement s'il a un propriétaire).
- **Équipement** : armure (4 emplacements) et une arme en main.
- **Combat au choix du joueur** : en mêlée avec une épée ou une hache, ou **à distance avec un arc ou une arbalète**. À distance, il **consomme des munitions** rangées dans son **petit inventaire** ; sans munitions, il repasse en mêlée.
- **Interface** : accroupi + clic droit sur son sous-fifre ouvre son inventaire (emplacements d'armure, arme, munitions), comme l'inventaire du cheval ou du lama. `SimpleInventory` sauvegardé en NBT + `ScreenHandler` ; `ExtendedScreenHandlerFactory` (Fabric API) pour envoyer l'id de l'entité au client.
- **IA** : `ZombieEntity` n'est pas une `TameableEntity`, donc les goals du loup (`FollowOwnerGoal`, `TrackOwnerAttackerGoal`, `AttackWithOwnerGoal`) sont à **réécrire** dans `entity/ai/` en s'en inspirant.
- **Pièges à prévoir** : l'**Iron Golem** cible les monstres et attaquera le sous-fifre (exception à gérer, mixin ou prédicat sur son ciblage) ; les autres zombies (aveugle compris) et le joueur ne doivent pas le traiter comme un ennemi par erreur, et lui ne doit pas attaquer son propriétaire.
- Difficulté : ⭐⭐⭐⭐ (inventaire, interface et IA de compagnon).

**Plus tard** :
- **Modes de comportement** (stockés en NBT, changés depuis l'interface) :
  - **Agressif** : attaque toute créature hostile au joueur, avec la portée de vision d'un zombie vanilla.
  - **Neutre** (comme le loup) : défend le joueur quand une entité l'attaque.
  - **Passif** : n'attaque personne, même si le joueur est attaqué.
  - **Sentinelle** : défend une **zone définie par le joueur** (position + rayon stockés en NBT) contre toute créature hostile.
- **Améliorations** :
  - augmenter le **nombre de sous-fifres invocables** en même temps (compteur par joueur, par exemple via l'Attachment API de Fabric) ;
  - améliorer le sous-fifre lui-même : plus de PV, plus de dégâts, etc. (modificateurs d'attributs stockés dans l'item).

**À définir** : ce qui arrive à l'item si le sous-fifre meurt (item vide, item détruit, ou temps de recharge avant de pouvoir le réinvoquer) ; nombre d'emplacements de l'inventaire ; nom de l'item d'invocation ; valeurs des dégâts du bûcheron et du sous-fifre sauvage ; bonus exacts du chef de horde (légers, par impulsions avec temps de recharge, sans les sous-fifres) : durée, temps de recharge, rayon (ses propres stats sont fixées dans « Stats des exceptions »).

## Items prévus

| Item | Obtention | Effet | Piste technique |
|---|---|---|---|
| **Arbalète d'améthyste** | Craft ; rare sur le lanceur d'améthyste | Munitions : **éclats d'améthyste**. La cible touchée reçoit **Surbrillance** (Glowing) et **Provocation** | Classe dérivée de `CrossbowItem` qui accepte les éclats d'améthyste comme munitions ; projectile `AmethystShardEntity` |
| **Effet Provocation** (Taunt) | Via l'arbalète d'améthyste | Pendant **10 s**, les **mobs hostiles** proches attaquent la cible. Sert à détourner l'attention des mobs | `StatusEffect` personnalisé : toutes les X ticks, les `HostileEntity` dans un rayon (à définir) prennent l'entité affectée comme cible (`setTarget`) |
| **Effet Ouïe aiguisée** | Donné par le Prophète du Sculk | Augmente la portée d'écoute des zombies aveugles | `StatusEffect` personnalisé lu par le `GameEventListener` du zombie aveugle |
| **Localisateurs de minerai** ×5 : diamant, fer, émeraude, redstone, lapis | Mineur des profondeurs, **1 % chacun** | Localisent le minerai correspondant le plus proche | À l'utilisation, scan limité autour du joueur (rayon et recharge à définir pour les performances) avec les tags de minerais (`BlockTags.DIAMOND_ORES`, etc.), indication de direction par particules ou message |
| **Item d'invocation du sous-fifre** (nom à définir) | Chef de horde (Déterrés), **10 %**, kill par le joueur | Contient un sous-fifre, l'invoque et le rappelle, comme une Poké Ball | Item non empilable ; données du sous-fifre dans un composant de données (voir « Sous-fifre du joueur ») |
| **Effet Englué** | Bras du zombie lanceur (Déterrés) | **Lenteur pendant 3 s** + **bave verte à l'écran** ; retiré par le lait | `StatusEffect` personnalisé avec modificateur de vitesse ; particules de potion vertes sur la créature affectée (couleur `GluedStatusEffect.COLOR`, dessinées par le jeu). **À faire plus tard** : icône d'effet (`textures/mob_effect/glued.png`) et description ; overlay côté client avec `HudRenderCallback` tant que le joueur a l'effet (les effets sont déjà synchronisés au client) |
| **Armure d'améthyste** | Craft : améthyste, laine, fragments d'écho | **Réduit les bruits** du joueur (moins de vibrations détectées) | `ArmorMaterial` enregistré (registre en 1.21) ; réduction du bruit via un mixin sur l'émission des GameEvents du joueur, ou vérifiée par les listeners du mod. Pièce par pièce ou set complet : à définir |

### Idées supplémentaires (non validées)

- Zombie sporifère (Lush Caves, champignons) : nuage de spores à la mort (`AreaEffectCloud`).
- Momie du désert : se reconstitue sauf si tuée par le feu.
- Zombie de marécage : caché sous l'eau, agrippe et ralentit.
- Talisman de faction (vendu par le marchand) : rend une faction neutre envers le joueur.
- Succès « Tueur de faction » : avoir éliminé un zombie de chaque faction. Piste : un tag d'entités par faction (`zombie_expansion:frost_faction`...) + un advancement généré par `FabricAdvancementProvider` avec un critère `player_killed_entity` par tag, tous requis.

## Feuille de route

1. Mise en place du projet (template Fabric, IntelliJ, `runClient` fonctionnel)
2. Premier zombie simple (zombie gelé) : entité, attributs, rendu, œuf, spawn par biome
3. Zombie aveugle (IA, goals, GameEvents)
4. Zombie lanceur (projectiles, DataTracker) : première pierre de la faction Les Déterrés (chaque faction démarre par **un zombie fondateur** : Givre = gelé, Abysses = aveugle, Déterrés = lanceur)
5. Tags régionaux, hostilité inter-zombies, puis factions en JSON (tester d'abord avec la faction Givre)
6. Spawn des Déterrés (surface + grottes jusqu'à Y -8, plafonnement)
7. Faction Les Déterrés : chef de horde (le « général »), sous-fifres, puis le sous-fifre du joueur (item d'invocation, équipement, inventaire), puis les autres membres
8. Faction Abysses et ses items (arbalète d'améthyste, effets Provocation et Ouïe aiguisée, localisateurs, armure d'améthyste)
9. Zombie marchand pacifiste
10. Raids de factions

Idées gardées pour une faction future : le zombie **rampant** (circule dans les tunnels d'un bloc de haut).

## Après le mod principal

Addons prévus pour étendre le même concept (variantes par biome + factions) aux autres mobs hostiles : squelettes, pillagers, etc. Le système de factions doit donc rester générique et ne pas dépendre de `ZombieEntity`.