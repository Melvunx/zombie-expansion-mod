# CLAUDE.md — Zombie Expansion

Ce fichier donne le contexte du projet à Claude (et à tout contributeur) : vision, spécifications techniques, conventions et feuille de route.

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
- **Environnements** : logique commune côté `main` ; rendu, modèles et renderers côté `client` (split sources Loom).

### Structure prévue

```
src/main/java/net/melvunx/zombieexpansion/
  ZombieExpansion.java        # ModInitializer : enregistrements
  entity/                     # classes des zombies et projectiles
  entity/ai/                  # goals personnalisés
  faction/                    # système de factions (chargé depuis les données)
  event/                      # hordes, raids de factions (PersistentState)
  registry/                   # ModEntities, ModItems, ModSounds...
  world/                      # spawn par biome (BiomeModifications)
src/client/java/net/melvunx/zombieexpansion/
  ZombieExpansionClient.java  # ClientModInitializer : renderers, modèles
  render/                     # EntityRenderer / EntityModel
src/main/resources/
  fabric.mod.json
  assets/zombie_expansion/    # textures, lang (fr_fr, en_us), sons
  data/zombie_expansion/      # tags, loot tables, factions JSON
```

## Conventions

- Un zombie = une classe d'entité + un renderer + un modèle + une texture + un œuf d'apparition + une loot table + une règle de spawn.
- Toujours fournir les traductions `en_us.json` **et** `fr_fr.json`.
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
