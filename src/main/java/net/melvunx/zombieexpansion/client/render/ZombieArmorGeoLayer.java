package net.melvunx.zombieexpansion.client.render;

import java.util.Map;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.ItemArmorGeoLayer;

/**
 * Calque d'armure pour les modèles de zombies du mod qui suivent le squelette d'os d'armure commun
 * ({@code armor_head}, {@code armor_body}, {@code armor_waist}, {@code armor_left_arm}, {@code armor_right_arm},
 * {@code armor_left_leg}, {@code armor_right_leg}, {@code armor_left_boot}, {@code armor_right_boot}).
 *
 * GeckoLib ne dessine pas l'armure seul : {@link ItemArmorGeoLayer} dessine, os par os, la pièce vanilla portée
 * dans l'emplacement voulu, à la taille du cube de l'os. Ce calque indique seulement, pour chaque os, quelle
 * pièce lire (casque, plastron...) et quelle partie du modèle d'armure vanilla (tête, corps, bras...) y dessiner.
 */
@Environment(EnvType.CLIENT)
public class ZombieArmorGeoLayer<T extends LivingEntity & GeoAnimatable> extends ItemArmorGeoLayer<T> {
	private static final Map<String, EquipmentSlot> SLOTS = Map.of(
		"armor_head", EquipmentSlot.HEAD,
		"armor_body", EquipmentSlot.CHEST,
		"armor_left_arm", EquipmentSlot.CHEST,
		"armor_right_arm", EquipmentSlot.CHEST,
		"armor_waist", EquipmentSlot.LEGS,
		"armor_left_leg", EquipmentSlot.LEGS,
		"armor_right_leg", EquipmentSlot.LEGS,
		"armor_left_boot", EquipmentSlot.FEET,
		"armor_right_boot", EquipmentSlot.FEET);

	public ZombieArmorGeoLayer(GeoRenderer<T> renderer) {
		super(renderer);
	}

	@Nullable
	@Override
	protected ItemStack getArmorItemForBone(GeoBone bone, T animatable) {
		EquipmentSlot slot = SLOTS.get(bone.getName());
		if (slot == null) {
			return null;
		}
		return switch (slot) {
			case HEAD -> helmetStack;
			case CHEST -> chestplateStack;
			case LEGS -> leggingsStack;
			default -> bootsStack;
		};
	}

	@NotNull
	@Override
	protected EquipmentSlot getEquipmentSlotForBone(GeoBone bone, ItemStack stack, T animatable) {
		EquipmentSlot slot = SLOTS.get(bone.getName());
		return slot != null ? slot : EquipmentSlot.CHEST;
	}

	@NotNull
	@Override
	protected ModelPart getModelPartForBone(GeoBone bone, EquipmentSlot slot, ItemStack stack, T animatable, BipedEntityModel<?> baseModel) {
		return switch (bone.getName()) {
			case "armor_head" -> baseModel.head;
			case "armor_left_arm" -> baseModel.leftArm;
			case "armor_right_arm" -> baseModel.rightArm;
			case "armor_left_leg", "armor_left_boot" -> baseModel.leftLeg;
			case "armor_right_leg", "armor_right_boot" -> baseModel.rightLeg;
			default -> baseModel.body;
		};
	}
}
