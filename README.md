# 🧟 Zombie Expansion

> Un mod Fabric pour Minecraft **1.21.1** qui transforme les zombies en une menace aussi variée que le monde lui-même.

Dans Minecraft, un zombie reste un zombie, que vous soyez dans la jungle, le désert ou au fond d'une caverne. **Zombie Expansion** change ça : chaque biome et chaque type de caverne abrite ses propres zombies, avec leurs capacités, leurs comportements et même leurs alliances. Les zombies forment des factions, se font la guerre entre régions, s'organisent en hordes… et certains n'ont même pas envie de vous manger.

## ✨ Contenu prévu

### Nouveaux zombies

- **Zombie aveugle** : il ne vous voit pas, mais il vous entend. Le moindre bruit l'attire, et il est plus rapide qu'un zombie normal. Avancez accroupi…
- **Zombie caméléon** : caché dans la végétation de la jungle, il se fond dans le décor jusqu'à ce qu'il soit trop tard.
- **Zombie lanceur** : il arrache ses propres membres pour vous les lancer dessus.
- **Zombie général** : il mène une horde, et plus elle est grande, plus elle est puissante. Abattez-le pour la désorganiser.
- **Zombie gelé** : dans les terres enneigées, ses coups vous glacent sur place.
- **Zombie marchand** : éduqué par un mystérieux maître, il est pacifique, vend des objets uniques du mod et défend son maître si le village est attaqué.

### Factions et interactions

- Les zombies d'une région attaquent ceux venus d'ailleurs.
- Des **factions** zombies s'allient ou se font la guerre.
- De **nouveaux raids** : des factions entières de zombies attaquent les villages par vagues.

## 📦 Installation

> Le mod est en cours de développement, aucune version n'est encore publiée.

1. Installer [Fabric Loader](https://fabricmc.net/use/) pour Minecraft 1.21.1
2. Télécharger [Fabric API](https://modrinth.com/mod/fabric-api)
3. Placer `zombie_expansion-x.x.x.jar` et Fabric API dans le dossier `mods`

**Prérequis** : Minecraft 1.21.1 · Fabric Loader · Fabric API · Java 21

## 🛠️ Développement

```bash
git clone <url-du-depot>
cd zombie-expansion
./gradlew runClient   # lance Minecraft avec le mod
./gradlew build       # génère le .jar dans build/libs/
```

Outils utilisés : IntelliJ IDEA, Fabric Loom, Blockbench.

## 🗺️ Feuille de route

- [ ] Mise en place du projet
- [ ] Zombie gelé
- [ ] Zombie aveugle
- [ ] Zombie lanceur
- [ ] Hostilité entre régions et factions
- [ ] Zombie général et hordes
- [ ] Zombie marchand
- [ ] Raids de factions

## 🔮 Ajouts futurs

**Nouveaux zombies envisagés**
- Zombie mineur qui creuse jusqu'à vous à travers la roche
- Zombie de géode couvert de cristaux d'améthyste
- Zombie sporifère des cavernes luxuriantes
- Momie du désert qui se reconstitue si elle n'est pas brûlée
- Zombie des marais qui vous attrape sous l'eau
- Zombie porte-torche qui plonge vos bases dans le noir
- Zombie infecteur qui transforme les animaux en zombies

**Objets**
- Talisman de faction pour devenir neutre auprès d'une faction
- Armures et équipements vendus par le zombie marchand

**Addons**
- Extension du concept aux autres mobs hostiles : **squelettes**, **pillagers** et d'autres, avec leurs propres variantes et factions.

## 📄 Licence

Ce projet est distribué sous licence **MIT**. Voir le fichier [LICENSE](LICENSE) pour plus de détails.