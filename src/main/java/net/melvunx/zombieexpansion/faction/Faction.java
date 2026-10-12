package net.melvunx.zombieexpansion.faction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.melvunx.zombieexpansion.registry.ModBiomeTags;
import net.melvunx.zombieexpansion.registry.ModEntityTypeTags;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

/**
 * Les factions du mod. Chacune est définie par un tag d'entités : l'appartenance d'un mob
 * est donc une donnée, jamais un {@code instanceof}.
 *
 * Reste volontairement sans logique de relations (alliés / ennemis) : c'est l'étape suivante
 * de la feuille de route, qui les chargera depuis des JSON de datapack.
 */
public enum Faction {
	FROST("frost", ModEntityTypeTags.FROST_FACTION),
	ABYSS("abyss", ModEntityTypeTags.ABYSS_FACTION),
	VANILLA("vanilla", ModEntityTypeTags.VANILLA_ZOMBIE_FACTION);

	/** Altitude maximale du territoire des Abysses : en dessous, c'est leur zone quel que soit le biome. */
	public static final int ABYSS_MAX_Y = -8;

	private final String id;
	private final TagKey<EntityType<?>> tag;

	Faction(String id, TagKey<EntityType<?>> tag) {
		this.id = id;
		this.tag = tag;
	}

	public TagKey<EntityType<?>> getTag() {
		return tag;
	}

	public Text getDisplayName() {
		return Text.translatable("faction.zombie_expansion." + id);
	}

	public boolean includes(EntityType<?> type) {
		return type.isIn(tag);
	}

	/** Types d'entités membres, lus dans le tag (donc tels que le datapack les définit). */
	public List<EntityType<?>> getMembers() {
		List<EntityType<?>> members = new ArrayList<>();
		for (RegistryEntry<EntityType<?>> entry : Registries.ENTITY_TYPE.iterateEntries(tag)) {
			members.add(entry.value());
		}
		return members;
	}

	/** La faction d'un type d'entité, si elle en a une. */
	public static Optional<Faction> of(EntityType<?> type) {
		for (Faction faction : values()) {
			if (faction.includes(type)) {
				return Optional.of(faction);
			}
		}
		return Optional.empty();
	}

	/**
	 * La faction dominante à une position : les Abysses sous Y -8 ou dans leur biome d'origine,
	 * puis le Givre dans ses biomes, sinon les zombies vanilla. L'ordre compte : sous terre le
	 * zombie gelé n'apparaît pas (il exige le ciel visible), les Abysses l'emportent donc.
	 */
	public static Faction at(World world, BlockPos pos) {
		RegistryEntry<Biome> biome = world.getBiome(pos);
		if (pos.getY() <= ABYSS_MAX_Y || biome.isIn(ModBiomeTags.ABYSS_FACTION_SPAWNS)) {
			return ABYSS;
		}
		if (biome.isIn(ModBiomeTags.FROST_FACTION_SPAWNS) || biome.isIn(ModBiomeTags.FROST_FACTION_MIXED_SPAWNS)) {
			return FROST;
		}
		return VANILLA;
	}
}
