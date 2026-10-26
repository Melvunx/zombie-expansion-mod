package net.melvunx.zombieexpansion.registry;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.melvunx.zombieexpansion.ZombieExpansion;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Registre des objets du mod (pour l'instant : les œufs d'apparition). */
public final class ModItems {
	// Les deux entiers sont les couleurs RVB de la base et des taches de l'œuf.
	public static final SpawnEggItem FROZEN_ZOMBIE_SPAWN_EGG = register(
		"frozen_zombie_spawn_egg",
		new SpawnEggItem(ModEntities.FROZEN_ZOMBIE, 0x9FD3E6, 0xE8F6FB, new Item.Settings())
	);

	public static final SpawnEggItem BLINDED_ZOMBIE_SPAWN_EGG = register(
		"blinded_zombie_spawn_egg",
		new SpawnEggItem(ModEntities.BLINDED_ZOMBIE, 0x0E2A33, 0x1CBFC4, new Item.Settings())
	);

	public static final SpawnEggItem THROWER_ZOMBIE_SPAWN_EGG = register(
		"thrower_zombie_spawn_egg",
		new SpawnEggItem(ModEntities.THROWER_ZOMBIE, 0x4F7A3A, 0x9BD13C, new Item.Settings())
	);

	public static final SpawnEggItem HORDE_CHIEF_SPAWN_EGG = register(
		"horde_chief_spawn_egg",
		new SpawnEggItem(ModEntities.HORDE_CHIEF, 0x5E7F4A, 0x8B2E2E, new Item.Settings())
	);

	public static final SpawnEggItem HORDE_MINION_SPAWN_EGG = register(
		"horde_minion_spawn_egg",
		new SpawnEggItem(ModEntities.HORDE_MINION, 0x5E7F4A, 0xC8A26A, new Item.Settings())
	);

	public static final SpawnEggItem RUNNER_ZOMBIE_SPAWN_EGG = register(
		"runner_zombie_spawn_egg",
		new SpawnEggItem(ModEntities.RUNNER_ZOMBIE, 0x5E7F4A, 0xD9D2A3, new Item.Settings())
	);

	private ModItems() {
	}

	private static <T extends Item> T register(String name, T item) {
		return Registry.register(Registries.ITEM, ZombieExpansion.id(name), item);
	}

	public static void register() {
		// Sans ça, l'œuf existe mais n'apparaît dans aucun onglet du mode créatif.
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS)
			.register(entries -> {
				entries.add(FROZEN_ZOMBIE_SPAWN_EGG);
				entries.add(BLINDED_ZOMBIE_SPAWN_EGG);
				entries.add(THROWER_ZOMBIE_SPAWN_EGG);
				entries.add(HORDE_CHIEF_SPAWN_EGG);
				entries.add(HORDE_MINION_SPAWN_EGG);
				entries.add(RUNNER_ZOMBIE_SPAWN_EGG);
			});
	}
}
