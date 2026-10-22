package net.melvunx.zombieexpansion.entity;

import net.melvunx.zombieexpansion.registry.ModEntityTypeTags;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.melvunx.zombieexpansion.registry.ModStatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/**
 * Le bras lancé par le zombie lanceur.
 *
 * Hérite de {@link ThrownItemEntity} (comme la boule de neige ou l'œuf) : on récupère gratuitement
 * la physique (gravité, collision), la synchronisation client et le rendu « objet en vol ».
 * Provisoirement, le bras s'affiche comme de la chair putréfiée ; un modèle dédié viendra plus tard.
 *
 * Au contact d'une créature : un peu de dégâts, puis l'effet {@link ModStatusEffects#GLUED}.
 * Il traverse les membres de la faction Les Déterrés (pas de tir ami).
 */
public class ThrownArmEntity extends ThrownItemEntity {
	/** Dégâts d'impact : 2 PV (1 cœur), sous la mêlée du zombie vanilla. À ajuster en jeu. */
	private static final float DAMAGE = 2.0f;
	/** Durée de l'effet Englué : 3 secondes. */
	private static final int GLUED_TICKS = 60;
	/** Code d'événement « le projectile éclate » : lu par handleStatus côté client. */
	private static final byte STATUS_SPLAT = 3;

	public ThrownArmEntity(EntityType<? extends ThrownArmEntity> entityType, World world) {
		super(entityType, world);
	}

	public ThrownArmEntity(World world, LivingEntity owner) {
		super(ModEntities.THROWN_ARM, owner, world);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.ROTTEN_FLESH;
	}

	/** Un zombie de la faction ne blesse pas ses alliés : le bras passe à travers. */
	@Override
	protected boolean canHit(Entity entity) {
		return super.canHit(entity) && !entity.getType().isIn(ModEntityTypeTags.UNEARTHED_FACTION);
	}

	@Override
	protected void onEntityHit(EntityHitResult hit) {
		super.onEntityHit(hit);
		Entity target = hit.getEntity();
		if (target.damage(getDamageSources().thrown(this, getOwner()), DAMAGE) && target instanceof LivingEntity living) {
			living.addStatusEffect(new StatusEffectInstance(ModStatusEffects.GLUED, GLUED_TICKS), getOwner());
		}
	}

	/** Appelé pour tout impact (créature ou bloc) : le bras éclate et disparaît. */
	@Override
	protected void onCollision(HitResult hit) {
		super.onCollision(hit);
		if (!getWorld().isClient) {
			getWorld().sendEntityStatus(this, STATUS_SPLAT);
			discard();
		}
	}

	/** Reçoit l'événement envoyé par le serveur : éclaboussure de bave verte. */
	@Override
	public void handleStatus(byte status) {
		if (status == STATUS_SPLAT) {
			for (int i = 0; i < 8; i++) {
				getWorld().addParticle(ParticleTypes.ITEM_SLIME, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
			}
		} else {
			super.handleStatus(status);
		}
	}
}
