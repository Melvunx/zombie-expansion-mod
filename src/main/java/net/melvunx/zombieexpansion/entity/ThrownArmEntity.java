package net.melvunx.zombieexpansion.entity;

import net.melvunx.zombieexpansion.effect.GluedStatusEffect;
import net.melvunx.zombieexpansion.faction.Faction;
import net.melvunx.zombieexpansion.registry.ModEntities;
import net.melvunx.zombieexpansion.registry.ModStatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.EntityEffectParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Le bras lancé par le zombie lanceur.
 *
 * Hérite de {@link ThrownItemEntity} (comme la boule de neige ou l'œuf) : on récupère gratuitement
 * la physique (gravité, collision), la synchronisation client et le rendu « objet en vol ».
 * Implémente {@link GeoEntity} : le bras est un modèle GeckoLib (thrown_arm.*) qui tournoie en vol.
 *
 * Au contact d'une créature quelconque (pas seulement le joueur) : les mêmes dégâts, puis l'effet
 * {@link ModStatusEffects#GLUED}. Il traverse les membres de la faction du lanceur (ni dégâts ni effet).
 * Enderman : comme avec une flèche, il se téléporte et ne subit ni dégâts ni effet.
 * En vol, il laisse une traînée de particules de potion vertes, de la couleur de l'effet.
 */
public class ThrownArmEntity extends ThrownItemEntity implements GeoEntity {
	/** Dégâts d'impact : 2 PV (1 cœur), sous la mêlée du zombie vanilla. À ajuster en jeu. */
	private static final float DAMAGE = 2.0f;
	/** Code d'événement « le projectile éclate » : lu par handleStatus côté client. */
	private static final byte STATUS_SPLAT = 3;

	private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.thrown_arm.spin");

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public ThrownArmEntity(EntityType<? extends ThrownArmEntity> entityType, World world) {
		super(entityType, world);
	}

	public ThrownArmEntity(World world, LivingEntity owner) {
		super(ModEntities.THROWN_ARM, owner, world);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "spin", 0, state -> state.setAndContinue(SPIN)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return geoCache;
	}

	/** L'entité garde la base ThrownItemEntity (physique, réseau) ; son « objet » n'est plus dessiné. */
	@Override
	protected Item getDefaultItem() {
		return Items.ROTTEN_FLESH;
	}

	/** Un membre de la faction du lanceur est un allié : le bras passe à travers, sans dégâts ni effet. */
	@Override
	protected boolean canHit(Entity entity) {
		return super.canHit(entity) && !isAlly(entity);
	}

	/**
	 * La faction se lit sur le type du lanceur (tags, jamais un instanceof). Si le lanceur a disparu
	 * entre-temps (mort, déchargé), on retombe sur le zombie lanceur, donc sur Les Déterrés.
	 */
	private boolean isAlly(Entity entity) {
		Entity owner = getOwner();
		EntityType<?> ownerType = owner != null ? owner.getType() : ModEntities.THROWER_ZOMBIE;
		return Faction.of(ownerType).map(faction -> faction.includes(entity.getType())).orElse(false);
	}

	/** Traînée verte côté client : le serveur n'a pas à envoyer de particules, chaque client dessine les siennes. */
	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			getWorld().addParticle(
				EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, ColorHelper.Argb.withAlpha(255, GluedStatusEffect.COLOR)),
				getParticleX(0.5), getY(), getParticleZ(0.5), 0.0, 0.0, 0.0);
		}
	}

	@Override
	protected void onEntityHit(EntityHitResult hit) {
		super.onEntityHit(hit);
		Entity target = hit.getEntity();
		boolean damaged = target.damage(getDamageSources().thrown(this, getOwner()), DAMAGE);
		// L'Enderman renvoie vrai même sans subir de dégâts (il s'est téléporté, comme face à une flèche) :
		// il faut donc l'exclure explicitement pour ne pas l'engluer.
		if (damaged && target instanceof LivingEntity living && !(target instanceof EndermanEntity)) {
			living.addStatusEffect(new StatusEffectInstance(ModStatusEffects.GLUED, GluedStatusEffect.DEFAULT_DURATION_TICKS), getOwner());
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
