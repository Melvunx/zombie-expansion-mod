package net.melvunx.zombieexpansion.entity;

import software.bernie.geckolib.animation.RawAnimation;

/**
 * Les trois looks du sous-fifre. Même modèle GeckoLib (horde_minion.geo.json), mais chaque variante a sa
 * texture (visage et tatouages) et ses propres animations, nommées animation.horde_minion.&lt;id&gt;.&lt;action&gt;
 * dans horde_minion.animation.json.
 */
public enum HordeMinionVariant {
	/** Regard dans le vide, bouche ouverte, tatouages ocre en points. Lent et mou. */
	DOPEY("dopey"),
	/** Yeux qui louchent, tatouages bleus en zigzag. Tressaille, trébuche, frappe en moulinet. */
	CROSS_EYED("cross_eyed"),
	/** Grosse dent et sourire niais, tatouages blancs en os. Sautille, frappe en estoc. */
	TOOTHY("toothy");

	private final String id;
	private final RawAnimation idle;
	private final RawAnimation walk;
	private final RawAnimation attack;
	private final RawAnimation boost;

	HordeMinionVariant(String id) {
		this.id = id;
		this.idle = RawAnimation.begin().thenLoop(animationName("idle"));
		this.walk = RawAnimation.begin().thenLoop(animationName("walk"));
		this.attack = RawAnimation.begin().thenPlay(animationName("attack"));
		this.boost = RawAnimation.begin().thenPlay(animationName("boost"));
	}

	public String id() {
		return id;
	}

	/** Nom exact dans le fichier d'animations, par ex. animation.horde_minion.dopey.walk. */
	private String animationName(String action) {
		return "animation.horde_minion." + id + "." + action;
	}

	public RawAnimation idle() {
		return idle;
	}

	public RawAnimation walk() {
		return walk;
	}

	public RawAnimation attack() {
		return attack;
	}

	public RawAnimation boost() {
		return boost;
	}

	/** Clé de déclenchement (triggerAnim) d'une animation ponctuelle de cette variante, par ex. « attack_dopey ». */
	public String trigger(String action) {
		return action + "_" + id;
	}

	public static HordeMinionVariant byOrdinal(int ordinal) {
		HordeMinionVariant[] all = values();
		return all[Math.floorMod(ordinal, all.length)];
	}
}
