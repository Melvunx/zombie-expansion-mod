package net.melvunx.zombieexpansion.entity;

import net.melvunx.zombieexpansion.entity.ai.InvestigateVibrationGoal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.GameEventTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.event.EntityPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.Vibrations;
import net.minecraft.world.event.listener.EntityGameEventHandler;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * Zombie aveugle (« The Blinded ») : un zombie du Deep Dark qui chasse au bruit.
 *
 * Il hérite de {@link ZombieEntity} pour garder le corps à corps, la casse de portes et les
 * dégâts vanilla, mais on remplace son IA de ciblage : il ne repère plus personne à la vue.
 * Il utilise à la place le système de vibrations du Warden et du sculk sensor
 * ({@link Vibrations}) : pas, chutes, projectiles qui atterrissent, etc.
 *
 * Conséquences voulues :
 *  - un joueur accroupi n'émet pas de vibration de pas : il est indétectable ;
 *  - une boule de neige lancée au loin attire le zombie vers l'endroit où elle retombe.
 */
public class BlindedZombieEntity extends ZombieEntity implements Vibrations {
	/** Portée d'écoute en blocs. Passée par une méthode pour que l'effet « Ouïe aiguisée » puisse la modifier plus tard. */
	public static final int BASE_LISTENING_RANGE = 12;

	/** 15 % plus rapide que le zombie vanilla (0,23). */
	private static final double MOVEMENT_SPEED = 0.23 * 1.15;
	private static final double MAX_HEALTH = 14.0;

	private static final float DARKNESS_CHANCE = 0.10f;
	/** Durée de l'effet Darkness : à valider (le CLAUDE.md dit « court », 100 ticks = 5 s). */
	private static final int DARKNESS_TICKS = 100;

	/** Délai minimal entre deux bruits pris en compte (comme le VIBRATION_COOLDOWN du Warden). */
	private static final int VIBRATION_COOLDOWN_TICKS = 40;
	/** Durée pendant laquelle il garde une cible entendue sans la ré-entendre. */
	private static final int HEARD_TARGET_TICKS = 100;

	/** Altitude maximale du spawn naturel (zone des Abysses : Y -8 et en dessous). */
	private static final int MAX_SPAWN_Y = -8;

	private final Vibrations.Callback vibrationCallback;
	private final Vibrations.ListenerData vibrationListenerData = new Vibrations.ListenerData();
	private final EntityGameEventHandler<Vibrations.VibrationListener> gameEventHandler;

	@Nullable
	private BlockPos investigatePos;
	@Nullable
	private LivingEntity heardTarget;
	private int heardTargetTicks;
	private int vibrationCooldown;
	/** Vrai pendant un coup porté par un joueur accroupi : les bruits de dégât/mort sont alors supprimés. */
	private boolean silentHit;

	public BlindedZombieEntity(EntityType<? extends ZombieEntity> entityType, World world) {
		super(entityType, world);
		this.vibrationCallback = new VibrationCallback();
		this.gameEventHandler = new EntityGameEventHandler<>(new Vibrations.VibrationListener(this));
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return ZombieEntity.createZombieAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, MAX_HEALTH)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, MOVEMENT_SPEED);
	}

	/** Dark + sur le sol, comme le zombie vanilla, et en dessous de Y -8 pour les spawns naturels. */
	public static boolean canSpawn(EntityType<BlindedZombieEntity> type, ServerWorldAccess world, SpawnReason reason, BlockPos pos, Random random) {
		boolean natural = reason == SpawnReason.NATURAL || reason == SpawnReason.CHUNK_GENERATION;
		return HostileEntity.canSpawnInDark(type, world, reason, pos, random) && (!natural || pos.getY() <= MAX_SPAWN_Y);
	}

	// ----- Pas de version bébé -----

	@Override
	public void setBaby(boolean baby) {
		super.setBaby(false);
	}

	// ----- IA : plus de ciblage à la vue -----

	/**
	 * On remplace entièrement les goals du zombie vanilla : ses ActiveTargetGoal repèrent les joueurs
	 * à vue, ce qui est exactement ce qu'un zombie aveugle ne doit pas faire.
	 */
	@Override
	protected void initGoals() {
		this.goalSelector.add(2, new ZombieAttackGoal(this, 1.0, false));
		this.goalSelector.add(3, new InvestigateVibrationGoal(this, 1.0));
		this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
		this.goalSelector.add(8, new LookAroundGoal(this));
		// Il se défend seulement : il riposte à qui le frappe.
		this.targetSelector.add(1, new RevengeGoal(this));
	}

	@Nullable
	public BlockPos getInvestigatePos() {
		return investigatePos;
	}

	public void clearInvestigation() {
		investigatePos = null;
	}

	// ----- Écoute des vibrations -----

	public int getListeningRange() {
		return BASE_LISTENING_RANGE;
	}

	/** Cibles qu'un zombie vanilla attaque aussi, hors créatif et spectateur. */
	private boolean isHeardTarget(@Nullable Entity entity) {
		return entity instanceof LivingEntity living
			&& living.isAlive()
			&& EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(living)
			&& (living instanceof PlayerEntity || living instanceof MerchantEntity || living instanceof IronGolemEntity);
	}

	@Override
	public void tick() {
		if (getWorld() instanceof ServerWorld serverWorld) {
			// Fait avancer les vibrations en transit, puis appelle VibrationCallback#accept à l'arrivée.
			Vibrations.Ticker.tick(serverWorld, vibrationListenerData, vibrationCallback);

			if (vibrationCooldown > 0) {
				vibrationCooldown--;
			}
			if (heardTarget != null && (--heardTargetTicks <= 0 || !heardTarget.isAlive())) {
				// On n'efface que la cible qu'il a entendue : pas celle que RevengeGoal a posée.
				if (getTarget() == heardTarget) {
					setTarget(null);
				}
				heardTarget = null;
			}
		}
		super.tick();
	}

	@Override
	public void updateEventHandler(BiConsumer<EntityGameEventHandler<?>, ServerWorld> callback) {
		if (getWorld() instanceof ServerWorld serverWorld) {
			callback.accept(gameEventHandler, serverWorld);
		}
	}

	@Override
	public Vibrations.ListenerData getVibrationListenerData() {
		return vibrationListenerData;
	}

	@Override
	public Vibrations.Callback getVibrationCallback() {
		return vibrationCallback;
	}

	/** Reçoit les vibrations : filtre ce qu'il entend (accepts) puis réagit (accept). */
	private class VibrationCallback implements Vibrations.Callback {
		private final PositionSource positionSource = new EntityPositionSource(BlindedZombieEntity.this, BlindedZombieEntity.this.getStandingEyeHeight());

		@Override
		public int getRange() {
			return getListeningRange();
		}

		@Override
		public PositionSource getPositionSource() {
			return positionSource;
		}

		/** Mêmes événements que le Warden : c'est ce qui rend la boule de neige efficace. */
		@Override
		public TagKey<GameEvent> getTag() {
			return GameEventTags.WARDEN_CAN_LISTEN;
		}

		@Override
		public boolean accepts(ServerWorld world, BlockPos pos, RegistryEntry<GameEvent> event, GameEvent.Emitter emitter) {
			if (isAiDisabled() || isDead() || vibrationCooldown > 0 || !world.getWorldBorder().contains(pos)) {
				return false;
			}
			Entity source = emitter.sourceEntity();
			// Il s'ignore lui-même, et ignore les autres zombies : sinon une troupe s'attirerait en boucle.
			if (source == BlindedZombieEntity.this || source instanceof ZombieEntity) {
				return false;
			}
			// Un animal ou un mob qui marche ne l'intéresse pas ; un projectile ou un bloc, si.
			return !(source instanceof LivingEntity) || isHeardTarget(source);
		}

		/**
		 * @param sourceEntity l'entité qui a émis le bruit (le joueur qui marche, la boule de neige...)
		 * @param entity       le propriétaire du projectile, non nul seulement pour un projectile
		 */
		@Override
		public void accept(ServerWorld world, BlockPos pos, RegistryEntry<GameEvent> event, @Nullable Entity sourceEntity, @Nullable Entity entity, float distance) {
			vibrationCooldown = VIBRATION_COOLDOWN_TICKS;
			investigatePos = pos;

			// Un projectile (entity != null) ne désigne PAS son lanceur : il attire seulement vers le point d'impact.
			if (entity == null && isHeardTarget(sourceEntity)) {
				heardTarget = (LivingEntity) sourceEntity;
				heardTargetTicks = HEARD_TARGET_TICKS;
				setTarget(heardTarget);
			}
		}
	}

	// ----- Combat -----

	@Override
	public boolean tryAttack(Entity target) {
		boolean hit = super.tryAttack(target);
		if (hit && target instanceof LivingEntity living && random.nextFloat() < DARKNESS_CHANCE) {
			living.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, DARKNESS_TICKS), this);
		}
		return hit;
	}

	/**
	 * Mort discrète : si le coup vient d'un joueur accroupi, les événements « dégât » et « mort »
	 * ne sont pas émis, donc les autres zombies aveugles alentour ne les entendent pas.
	 */
	@Override
	public boolean damage(DamageSource source, float amount) {
		silentHit = source.getAttacker() instanceof PlayerEntity player && player.isSneaking();
		try {
			return super.damage(source, amount);
		} finally {
			silentHit = false;
		}
	}

	@Override
	public void emitGameEvent(RegistryEntry<GameEvent> event, @Nullable Entity entity) {
		if (silentHit && (event.equals(GameEvent.ENTITY_DAMAGE) || event.equals(GameEvent.ENTITY_DIE))) {
			return;
		}
		super.emitGameEvent(event, entity);
	}

	/** Les ténèbres du Deep Dark le protègent : pas de combustion au soleil (de toute façon sous terre). */
	@Override
	protected boolean burnsInDaylight() {
		return false;
	}
}
