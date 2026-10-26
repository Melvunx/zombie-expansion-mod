package net.melvunx.zombieexpansion.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.melvunx.zombieexpansion.ZombieExpansion;
import net.melvunx.zombieexpansion.entity.HordeMinionVariant;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;

/**
 * Registre des sons du mod. Un {@link SoundEvent} n'est qu'un identifiant : les fichiers .ogg qu'il joue
 * sont déclarés à la main dans assets/zombie_expansion/sounds.json (ce qui fait partie des ressources écrites
 * à la main, voir CLAUDE.md). Les sous-titres (affichés si le joueur les active) sont générés par les
 * providers de langue à partir de {@link #ENTRIES}, pour que l'anglais et le français restent parallèles.
 */
public final class ModSounds {
	/** Un son et ses deux sous-titres. */
	public record Entry(SoundEvent event, String en, String fr) {
		/** Clé de traduction référencée par sounds.json (« subtitle »). */
		public String subtitleKey() {
			return "subtitles." + event.getId().getNamespace() + "." + event.getId().getPath();
		}
	}

	public static final List<Entry> ENTRIES = new ArrayList<>();
	private static final Map<String, SoundEvent> MINION_SOUNDS = new HashMap<>();

	public static final SoundEvent HORDE_CHIEF_AMBIENT = register("entity.horde_chief.ambient", "Horde Chief growls", "Le chef de horde gronde");
	public static final SoundEvent HORDE_CHIEF_ATTACK = register("entity.horde_chief.attack", "Horde Chief roars", "Le chef de horde rugit");
	/** Le cri de ralliement : joué quand le chef donne son bonus aux autres Déterrés. */
	public static final SoundEvent HORDE_CHIEF_BOOST = register("entity.horde_chief.boost", "Horde Chief rallies the horde", "Le chef de horde rallie la horde");

	static {
		for (HordeMinionVariant variant : HordeMinionVariant.values()) {
			String[] subtitles = switch (variant) {
				case DOPEY -> new String[]{"Horde Minion groans", "Horde Minion grunts", "Horde Minion mumbles",
					"Le sous-fifre gémit", "Le sous-fifre grogne", "Le sous-fifre marmonne"};
				case CROSS_EYED -> new String[]{"Horde Minion giggles", "Horde Minion yelps", "Horde Minion squeals",
					"Le sous-fifre ricane", "Le sous-fifre glapit", "Le sous-fifre couine"};
				case TOOTHY -> new String[]{"Horde Minion snickers", "Horde Minion yells", "Horde Minion cheers",
					"Le sous-fifre glousse", "Le sous-fifre crie", "Le sous-fifre exulte"};
			};
			String[] actions = {"ambient", "attack", "boost"};
			for (int i = 0; i < actions.length; i++) {
				MINION_SOUNDS.put(variant.id() + "." + actions[i],
					register("entity.horde_minion." + variant.id() + "." + actions[i], subtitles[i], subtitles[i + 3]));
			}
		}
	}

	private ModSounds() {
	}

	/** Son d'une variante de sous-fifre : action = « ambient » (au repos), « attack » ou « boost » (il reçoit le cri du chef). */
	public static SoundEvent minion(HordeMinionVariant variant, String action) {
		return MINION_SOUNDS.get(variant.id() + "." + action);
	}

	private static SoundEvent register(String name, String en, String fr) {
		SoundEvent event = Registry.register(Registries.SOUND_EVENT, ZombieExpansion.id(name), SoundEvent.of(ZombieExpansion.id(name)));
		ENTRIES.add(new Entry(event, en, fr));
		return event;
	}

	/** Force le chargement de la classe (donc l'enregistrement des champs statiques). */
	public static void register() {
	}
}
