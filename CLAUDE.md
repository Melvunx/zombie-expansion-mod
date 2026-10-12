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

### Structure prévue

```
src/main/java/net/melvunx/zombieexpansion/
  ZombieExpansion.java        # ModInitializer : enregistrements
  ZombieExpansionDataGenerator.java  # point d'entrée de la datagen
  datagen/                    # providers (langues, modèles, loot, tags, recettes)
  entity/                     # classes des zombies et projectiles
  entity/ai/                  # goals personnalisés (InvestigateVibrationGoal, SearchGoal)
  faction/                    # système de factions (chargé depuis les données)
  event/                      # hordes, raids de factions (PersistentState)
  registry/                   # ModEntities, ModItems, ModSounds...
  world/                      # spawn par biome (BiomeModifications) : FrostSpawns, AbyssSpawns
  command/                    # commandes (/zombie_expansion biome)
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
| Mineur des profondeurs | Voleur | Variante des profondeurs du **mineur zombie** (faction vanilla), **plus rapide** ; vole les minerais de l'inventaire et fuit, les rend à sa mort | Goal de fuite + inventaire interne sauvegardé en NBT | ⭐⭐⭐ |
| Éteigneur | Contrôle | Variante des profondeurs du **voleur de torche** (faction vanilla) : éteint les torches autour de lui | Recherche de blocs de lumière proches + suppression (respecter `mobGriefing`) | ⭐⭐ |
| Prophète du Sculk | **Chef (pas un boss)** | Voir détail ci-dessous | Logique de chef commune (général, chaman) | ⭐⭐⭐⭐ |

**Zombie aveugle** (implémenté, `BlindedZombieEntity`, id `blinded_zombie`, nom EN « The Blinded », assets `the_blinded.*`) :
- **Spawn** : origine Deep Dark (poids 20), mais présent **dans tout l'Overworld sous Y -8** (poids 4, rare), comme l'Enderman. Groupes de 2 à 3. Seuls les biomes ayant déjà des monstres reçoivent l'entrée (biome champignon exclu, biomes de mods respectés). Aucun bébé (`setBaby` neutralisé).
- **Plafond de densité** : pas de spawn naturel s'il y a déjà 5 aveugles à moins de 24 blocs (`MAX_NEARBY`, `DENSITY_RADIUS`). Le plafond de monstres vanilla est partagé entre espèces et ne limiterait rien dans le Deep Dark, où ils sont seuls.
- **Stats** : 14 PV, vitesse ×1,15 (0,23 → 0,2645), portée d'écoute **12 blocs** (`getListeningRange()`, point d'accroche de l'effet Ouïe aiguisée).
- **Écoute** : aucun `ActiveTargetGoal`. Mêmes événements que le Warden (tag `warden_can_listen`) via `Vibrations`, mais **seul le joueur** est entendu : ses pas, sa monture, ses projectiles et les objets qu'il lâche (même logique que le Sculk Shrieker). Un projectile ou un objet désigne un lieu, pas une cible : il attire vers l'impact sans cibler le lanceur (`InvestigateVibrationGoal`), puis il cherche sur place (`SearchGoal`). Accroupi = pas de vibration de pas = indétectable.
- **Alliés** : ne cible **jamais** un `ZombieEntity` (sa faction, zombies vanilla, husks, noyés) via `canTarget`, sauf cible portant l'effet Provocation (`isProvoked`, à brancher quand l'effet existera).
- **Mort discrète** : un coup d'un joueur accroupi supprime les événements `ENTITY_DAMAGE` / `ENTITY_DIE`, donc les autres aveugles n'entendent rien.
- **Les shriekers** : il ne les active pas (vanilla : un shrieker n'accepte que les vibrations d'un joueur).
- **Darkness** : 10 % par coup, 100 ticks (durée à valider).
- **Animations** (état synchronisé par `DataTracker` `BEHAVIOR`) : `idle`, `walk` (errance), `walk_grope` (en route vers un bruit), `run` (poursuite) / `run_frenzy` (poursuite avec ≤ 50 % de vie), `attack_slam` / `attack_grab_bite` / `attack_swipe` (au hasard à chaque attaque), `search_sniff` / `search_listen` / `search_ground` (au hasard, 3 à 5 s, à l'arrivée sur un bruit). Les trois animations de recherche sont bouclées : ce sont des états.
- **Texture** : `textures/entity/the_blinded.png` à fournir.

**Prophète du Sculk** : zombie couvert de sculk avec un catalyseur dans la poitrine. Pas de barre de boss, reste en retrait et fuit le joueur.
- Donne aux **zombies aveugles** proches un effet personnalisé **Ouïe aiguisée** qui augmente la portée de leur écoute.
- Augmente la **vie** et la **vitesse** de tous les zombies Abysses aux alentours.
- Lance périodiquement (**pas en continu**) une salve de debuffs sur les joueurs et ennemis proches : **Faiblesse 10 s**, **Lenteur 10 s**, **Darkness 5 s**.
- Répand du sculk quand un zombie meurt près de lui, comme un catalyseur.
  **Spawn** : le Deep Dark n'a aucun spawn de monstres en vanilla. Ajouter les spawns avec `BiomeModifications.addSpawn` et une restriction personnalisée (`SpawnRestriction`) qui vérifie `pos.getY() <= -8`. Surveiller la fréquence pour ne pas rendre la zone invivable.

**Loot** :
- Zombies du Deep Dark : chair putréfiée + **fragments d'écho**.
- Lanceur d'améthyste : chair putréfiée + **éclats d'améthyste** + faible chance de lâcher son **arbalète d'améthyste**.
- Mineur des profondeurs : chair putréfiée + **1 % de chance pour chacun** des 5 localisateurs de minerai.
  **À définir** : durée exacte du Darkness du zombie aveugle ; temps de recharge des salves du Prophète et du faisceau de redstone ; taux de drop des fragments d'écho et de l'arbalète ; si les tirs du lanceur d'améthyste appliquent Surbrillance et Provocation au joueur ; « pression des profondeurs » (bonus de stats selon le Y) à valider.

### Faction du zombie vanilla (nom à définir)

Faction des zombies « classiques » de la surface. Membres prévus :
- Zombie vanilla
- **Mineur zombie** classique : creuse vers le joueur et lâche du minerai (sa variante des profondeurs, plus rapide, est dans les Abysses). Respecter `mobGriefing`.
- **Voleur de torche** (alias zombie porte-torche) : éteint ou vole les torches (sa variante des profondeurs est l'Éteigneur des Abysses)
- **Zombie de géode** : armure de cristaux qui renvoie des dégâts, à briser à la pioche
- **Zombie infecteur** : transforme les animaux tués en variantes zombies

## Items prévus

| Item | Obtention | Effet | Piste technique |
|---|---|---|---|
| **Arbalète d'améthyste** | Craft ; rare sur le lanceur d'améthyste | Munitions : **éclats d'améthyste**. La cible touchée reçoit **Surbrillance** (Glowing) et **Provocation** | Classe dérivée de `CrossbowItem` qui accepte les éclats d'améthyste comme munitions ; projectile `AmethystShardEntity` |
| **Effet Provocation** (Taunt) | Via l'arbalète d'améthyste | Pendant **10 s**, les **mobs hostiles** proches attaquent la cible. Sert à détourner l'attention des mobs | `StatusEffect` personnalisé : toutes les X ticks, les `HostileEntity` dans un rayon (à définir) prennent l'entité affectée comme cible (`setTarget`) |
| **Effet Ouïe aiguisée** | Donné par le Prophète du Sculk | Augmente la portée d'écoute des zombies aveugles | `StatusEffect` personnalisé lu par le `GameEventListener` du zombie aveugle |
| **Localisateurs de minerai** ×5 : diamant, fer, émeraude, redstone, lapis | Mineur des profondeurs, **1 % chacun** | Localisent le minerai correspondant le plus proche | À l'utilisation, scan limité autour du joueur (rayon et recharge à définir pour les performances) avec les tags de minerais (`BlockTags.DIAMOND_ORES`, etc.), indication de direction par particules ou message |
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
4. Zombie lanceur (projectiles, DataTracker)
5. Tags régionaux, hostilité inter-zombies, puis factions en JSON (tester d'abord avec la faction Givre)
6. Zombie général et hordes
7. Faction Abysses et ses items (arbalète d'améthyste, effets Provocation et Ouïe aiguisée, localisateurs, armure d'améthyste)
8. Zombie marchand pacifiste
9. Raids de factions

Idées gardées pour une faction future : le zombie **rampant** (circule dans les tunnels d'un bloc de haut).

## Après le mod principal

Addons prévus pour étendre le même concept (variantes par biome + factions) aux autres mobs hostiles : squelettes, pillagers, etc. Le système de factions doit donc rester générique et ne pas dépendre de `ZombieEntity`.