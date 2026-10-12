package net.melvunx.zombieexpansion.registry;

import net.melvunx.zombieexpansion.ZombieExpansion;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

/**
 * Tags d'entités du mod : une faction = un tag. Ajouter un membre à une faction se fait donc
 * en données (tag JSON), sans toucher au code ; un datapack ou un autre mod peut le faire aussi.
 */
public final class ModEntityTypeTags {
	public static final TagKey<EntityType<?>> FROST_FACTION = of("frost_faction");
	public static final TagKey<EntityType<?>> ABYSS_FACTION = of("abyss_faction");
	/** Nom provisoire : la faction des zombies vanilla n'a pas encore de nom (voir CLAUDE.md). */
	public static final TagKey<EntityType<?>> VANILLA_ZOMBIE_FACTION = of("vanilla_zombie_faction");

	private ModEntityTypeTags() {
	}

	private static TagKey<EntityType<?>> of(String name) {
		return TagKey.of(RegistryKeys.ENTITY_TYPE, ZombieExpansion.id(name));
	}
}
