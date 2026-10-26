package net.melvunx.zombieexpansion.datagen;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.condition.KilledByPlayerLootCondition;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;

/** Génère data/zombie_expansion/loot_table/entities/*.json. */
public class ModEntityLootTableProvider extends SimpleFabricLootTableProvider {
	public ModEntityLootTableProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
		super(output, registryLookup, LootContextTypes.ENTITY);
	}

	@Override
	public void accept(BiConsumer<RegistryKey<LootTable>, LootTable.Builder> exporter) {
		// getLootTableId() = "zombie_expansion:entities/frozen_zombie", le chemin que le jeu consulte à la mort du mob.
		exporter.accept(ModEntities.FROZEN_ZOMBIE.getLootTableId(), LootTable.builder()
			.pool(LootPool.builder()
				.rolls(ConstantLootNumberProvider.create(1))
				.with(ItemEntry.builder(Items.ROTTEN_FLESH)
					.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(0, 2)))))
			.pool(LootPool.builder()
				.rolls(ConstantLootNumberProvider.create(1))
				.with(ItemEntry.builder(Items.SNOWBALL)
					.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(0, 3))))));

		// Zombie lanceur : chair putréfiée, comme le zombie vanilla.
		exporter.accept(ModEntities.THROWER_ZOMBIE.getLootTableId(), LootTable.builder()
			.pool(LootPool.builder()
				.rolls(ConstantLootNumberProvider.create(1))
				.with(ItemEntry.builder(Items.ROTTEN_FLESH)
					.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(0, 2))))));

		// Chef de horde : chair putréfiée (l'item d'invocation du sous-fifre viendra avec les sous-fifres).
		exporter.accept(ModEntities.HORDE_CHIEF.getLootTableId(), LootTable.builder()
			.pool(LootPool.builder()
				.rolls(ConstantLootNumberProvider.create(1))
				.with(ItemEntry.builder(Items.ROTTEN_FLESH)
					.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(0, 2))))));

		// Sous-fifre : chair putréfiée seulement ; l'item d'invocation ne tombe que du chef.
		exporter.accept(ModEntities.HORDE_MINION.getLootTableId(), LootTable.builder()
			.pool(LootPool.builder()
				.rolls(ConstantLootNumberProvider.create(1))
				.with(ItemEntry.builder(Items.ROTTEN_FLESH)
					.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(0, 2))))));

		// Coureur : chair putréfiée, comme le zombie vanilla.
		exporter.accept(ModEntities.RUNNER_ZOMBIE.getLootTableId(), LootTable.builder()
			.pool(LootPool.builder()
				.rolls(ConstantLootNumberProvider.create(1))
				.with(ItemEntry.builder(Items.ROTTEN_FLESH)
					.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(0, 2))))));

		// Zombie aveugle : chair putréfiée + 0 à 2 fragments d'écho (moyenne 1 par mort), ces derniers
		// seulement si le joueur porte le coup mortel : c'est une récompense, pas un farm à la lave.
		exporter.accept(ModEntities.BLINDED_ZOMBIE.getLootTableId(), LootTable.builder()
			.pool(LootPool.builder()
				.rolls(ConstantLootNumberProvider.create(1))
				.with(ItemEntry.builder(Items.ROTTEN_FLESH)
					.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(0, 2)))))
			.pool(LootPool.builder()
				.rolls(ConstantLootNumberProvider.create(1))
				.conditionally(KilledByPlayerLootCondition.builder())
				.with(ItemEntry.builder(Items.ECHO_SHARD)
					.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(0, 2))))));
	}
}
