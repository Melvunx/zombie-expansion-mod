package net.melvunx.zombieexpansion.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.melvunx.zombieexpansion.damage.DifficultyDamage;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;

/**
 * Écrit data/zombie_expansion/damage_type/*.json. Un type de dégâts est une entrée de registre
 * dynamique (comme un biome) : on la déclare dans {@code buildRegistry} puis on l'exporte ici.
 */
public class ModDamageTypeProvider extends FabricDynamicRegistryProvider {
	public ModDamageTypeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) {
		super(output, registries);
	}

	@Override
	protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
		entries.add(registries.getWrapperOrThrow(RegistryKeys.DAMAGE_TYPE), DifficultyDamage.HORDE_ATTACK);
	}

	@Override
	public String getName() {
		return "Zombie Expansion/Damage types";
	}
}
