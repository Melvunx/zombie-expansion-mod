package net.melvunx.zombieexpansion.entity;

import net.melvunx.zombieexpansion.entity.ai.InvestigateVibrationGoal;
import net.melvunx.zombieexpansion.entity.ai.SearchGoal;
import net.melvunx.zombieexpansion.faction.Faction;
import net.melvunx.zombieexpansion.registry.ModEntityTypeTags;
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
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.GameEventTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

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
 *  - une boule de neige lancée au loin attire le zombie vers l'endroit où elle retombe ;
 *  - il n'écoute que le joueur (ses pas, ses projectiles, ses objets lâchés), pas les autres mobs.
 *
 * Implémente aussi {@link GeoEntity} : modèle et animations GeckoLib (the_blinded.*).
 */
public class BlindedZombieEntity extends ZombieEntity implements Vibrations, GeoEntity {
	/** Portée d'écoute en blocs. Passée par une méthode pour que l'effet « Ouïe aiguisée » puisse la modifier plus tard. */
	public static final int BASE_LISTENING_RANGE = 12;

	/** 15 % plus rapide que le zombie vanilla (0,23). */
	private static final double MOVEMENT_SPEED = 0.23 * 1.15;
	private static final double MAX_HEALTH = 20.0;

	private static final float DARKNESS_CHANCE = 0.10f;
	/** Durée de l'effet Darkness : à valider (le CLAUDE.md dit « court », 100 ticks = 5 s). */
	private static final int DARKNESS_TICKS = 100;

	/** Délai minimal entre deux bruits pris en compte (comme le VIBRATION_COOLDOWN du Warden). */
	private static final int VIBRATION_COOLDOWN_TICKS = 40;
	/** Durée pendant laquelle il garde une cible entendue sans la ré-entendre. */
	private static final int HEARD_TARGET_TICKS = 100;

	/** Altitude maximale du spawn naturel (zone des Abysses : Y -8 et en dessous). */
	private static final int MAX_SPAWN_Y = Faction.ABYSS_MAX_Y;

	/** Plafond de densité : pas de nouveau spawn naturel s'il y a déjà 5 aveugles à moins de 24 blocs. */
	public static final int MAX_NEARBY = 5;
	public static final int DENSITY_RADIUS = 24;

	/** Durée d'une recherche à l'arrivée sur un bruit : 3 à 5 secondes. */
	private static final int SEARCH_MIN_TICKS = 60;
	private static final int SEARCH_MAX_TICKS = 100;

	// États synchronisés pour l'animation : le client ne connaît ni la cible ni la position à vérifier.
	private static final int BEHAVIOR_IDLE = 0;
	private static final int BEHAVIOR_INVESTIGATING = 1;
	private static final int BEHAVIOR_CHASING = 2;
	private static final int BEHAVIOR_SEARCH_SNIFF = 3;
	private static final int BEHAVIOR_SEARCH_LISTEN = 4;
	private static final int BEHAVIOR_SEARCH_GROUND = 5;
	/** Poursuite avec l'animation run_frenzy ; tirée au hasard à chaque nouvelle poursuite. */
	private static final int BEHAVIOR_CHASING_FRENZY = 6;
	private static final float FRENZY_CHANCE = 0.5f;
	private static final TrackedData<Integer> BEHAVIOR =
		DataTracker.registerData(BlindedZombieEntity.class, TrackedDataHandlerRegistry.INTEGER);

	// Noms définis dans the_blinded.animation.json.
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.the_blinded.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.the_blinded.walk");
	private static final RawAnimation WALK_GROPE = RawAnimation.begin().thenLoop("animation.the_blinded.walk_grope");
	private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.the_blinded.run");
	private static final RawAnimation RUN_FRENZY = RawAnimation.begin().thenLoop("animation.the_blinded.run_frenzy");
	private static final RawAnimation SEARCH_SNIFF = RawAnimation.begin().thenLoop("animation.the_blinded.search_sniff");
	private static final RawAnimation SEARCH_LISTEN = RawAnimation.begin().thenLoop("animation.the_blinded.search_listen");
	private static final RawAnimation SEARCH_GROUND = RawAnimation.begin().thenLoop("animation.the_blinded.search_ground");

	private static final String ATTACK_CONTROLLER = "attack";
	private static final String[] ATTACK_TRIGGERS = {"slam", "grab_bite", "swipe"};

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
	/** Ticks de recherche restants (0 = ne cherche pas) et animation choisie. */
	private int searchTicks;
	private int searchBehavior = BEHAVIOR_SEARCH_SNIFF;
	private boolean wasChasing;
	private boolean frenzied;

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

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
		if (!HostileEntity.canSpawnInDark(type, world, reason, pos, random)) {
			return false;
		}
		if (!natural) {
			return true;
		}
		if (pos.getY() > MAX_SPAWN_Y) {
			return false;
		}
		// Plafond de densité : le plafond de monstres vanilla ne distingue pas les espèces, donc dans le
		// Deep Dark (où nous sommes le seul monstre) rien d'autre ne limiterait les aveugles.
		int nearby = world.toServerWorld()
			.getEntitiesByClass(BlindedZombieEntity.class, new Box(pos).expand(DENSITY_RADIUS), entity -> true)
			.size();
		return nearby < MAX_NEARBY;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(BEHAVIOR, BEHAVIOR_IDLE);
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
		this.goalSelector.add(4, new SearchGoal(this));
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

	/** Nom d'état pour les commandes de diagnostic : idle, investigating, chasing ou searching. */
	public String getBehaviorName() {
		return switch (getDataTracker().get(BEHAVIOR)) {
			case BEHAVIOR_CHASING, BEHAVIOR_CHASING_FRENZY -> "chasing";
			case BEHAVIOR_INVESTIGATING -> "investigating";
			case BEHAVIOR_SEARCH_SNIFF, BEHAVIOR_SEARCH_LISTEN, BEHAVIOR_SEARCH_GROUND -> "searching";
			default -> "idle";
		};
	}

	public boolean isSearching() {
		return searchTicks > 0;
	}

	/** Lance une recherche à tâtons : renifler, écouter ou palper le sol, choisi au hasard. */
	public void beginSearch() {
		searchTicks = SEARCH_MIN_TICKS + random.nextInt(SEARCH_MAX_TICKS - SEARCH_MIN_TICKS + 1);
		searchBehavior = BEHAVIOR_SEARCH_SNIFF + random.nextInt(3);
	}

	// ----- Écoute des vibrations -----

	public int getListeningRange() {
		return BASE_LISTENING_RANGE;
	}

	/**
	 * Le joueur à l'origine d'un bruit, ou null. Même logique que le Sculk Shrieker : le joueur lui-même,
	 * sa monture, ses projectiles et les objets qu'il a lâchés. Créatif et spectateur sont ignorés.
	 */
	@Nullable
	private static PlayerEntity getResponsiblePlayer(@Nullable Entity source) {
		PlayerEntity player = null;
		if (source instanceof PlayerEntity p) {
			player = p;
		} else if (source != null && source.getControllingPassenger() instanceof PlayerEntity p) {
			player = p;
		} else if (source instanceof ProjectileEntity projectile && projectile.getOwner() instanceof PlayerEntity p) {
			player = p;
		} else if (source instanceof ItemEntity item && item.getOwner() instanceof PlayerEntity p) {
			player = p;
		}
		return player != null && EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(player) ? player : null;
	}

	/**
	 * Les membres de sa faction (tag abyss_faction) et les Déterrés, zombies vanilla compris (tag unearthed_faction)
	 * ne sont jamais des cibles, sauf s'ils portent l'effet Provocation. Les relations avec les autres
	 * factions viendront du système de relations en JSON : tant qu'il n'existe pas, on ne les cible pas
	 * non plus par défaut, puisque seuls le joueur et ceux qui le frappent le sont.
	 */
	@Override
	public boolean canTarget(LivingEntity target) {
		boolean protectedFromBlinded = target.getType().isIn(ModEntityTypeTags.ABYSS_FACTION)
			|| target.getType().isIn(ModEntityTypeTags.UNEARTHED_FACTION);
		if (protectedFromBlinded && !isProvoked(target)) {
			return false;
		}
		return super.canTarget(target);
	}

	/** Pas encore d'effet Provocation dans le mod : personne n'est provoqué. */
	private boolean isProvoked(LivingEntity target) {
		return false;
	}

	@Override
	public void tick() {
		if (getWorld() instanceof ServerWorld serverWorld) {
			// Fait avancer les vibrations en transit, puis appelle VibrationCallback#accept à l'arrivée.
			Vibrations.Ticker.tick(serverWorld, vibrationListenerData, vibrationCallback);

			if (vibrationCooldown > 0) {
				vibrationCooldown--;
			}
			if (searchTicks > 0) {
				searchTicks--;
			}
			updateBehavior();
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

	/** Recalcule l'état d'animation. DataTracker#set ne synchronise que si la valeur change. */
	private void updateBehavior() {
		int behavior;
		boolean chasing = getTarget() != null;
		if (chasing && !wasChasing) {
			// Nouvelle poursuite : on retire au sort la variante de course, qui reste la même jusqu'à la fin.
			frenzied = random.nextFloat() < FRENZY_CHANCE;
		}
		wasChasing = chasing;

		if (chasing) {
			behavior = frenzied ? BEHAVIOR_CHASING_FRENZY : BEHAVIOR_CHASING;
		} else if (searchTicks > 0) {
			behavior = searchBehavior;
		} else if (investigatePos != null) {
			behavior = BEHAVIOR_INVESTIGATING;
		} else {
			behavior = BEHAVIOR_IDLE;
		}
		getDataTracker().set(BEHAVIOR, behavior);
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
			// Il n'écoute que le joueur : ni les autres mobs, ni les blocs, ni lui-même.
			return source != BlindedZombieEntity.this && getResponsiblePlayer(source) != null;
		}

		/**
		 * @param sourceEntity l'entité qui a émis le bruit (le joueur qui marche, la boule de neige...)
		 * @param entity       le propriétaire du projectile, non nul seulement pour un projectile
		 */
		@Override
		public void accept(ServerWorld world, BlockPos pos, RegistryEntry<GameEvent> event, @Nullable Entity sourceEntity, @Nullable Entity entity, float distance) {
			vibrationCooldown = VIBRATION_COOLDOWN_TICKS;
			investigatePos = pos;
			searchTicks = 0; // un nouveau bruit interrompt la recherche en cours

			// Seul le joueur lui-même est ciblé. Un projectile, un objet lâché ou une monture désignent un lieu,
			// pas une cible : c'est ce qui permet de l'éloigner avec une boule de neige.
			if (sourceEntity instanceof PlayerEntity player) {
				heardTarget = player;
				heardTargetTicks = HEARD_TARGET_TICKS;
				setTarget(heardTarget);
			}
		}
	}

	// ----- Combat -----

	@Override
	public boolean tryAttack(Entity target) {
		// Variante au hasard côté serveur : triggerAnim la synchronise vers tous les clients.
		triggerAnim(ATTACK_CONTROLLER, ATTACK_TRIGGERS[random.nextInt(ATTACK_TRIGGERS.length)]);
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

	// ----- Animations GeckoLib -----

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "movement", 5, this::movementPredicate));

		// Ne joue rien seul : démarre sur triggerAnim("attack", "<variante>").
		controllers.add(new AnimationController<>(this, ATTACK_CONTROLLER, 0, state -> PlayState.STOP)
			.triggerableAnim("slam", RawAnimation.begin().thenPlay("animation.the_blinded.attack_slam"))
			.triggerableAnim("grab_bite", RawAnimation.begin().thenPlay("animation.the_blinded.attack_grab_bite"))
			.triggerableAnim("swipe", RawAnimation.begin().thenPlay("animation.the_blinded.attack_swipe")));
	}

	private PlayState movementPredicate(AnimationState<BlindedZombieEntity> state) {
		boolean moving = state.isMoving();
		return state.setAndContinue(switch (getDataTracker().get(BEHAVIOR)) {
			// Course : run ou run_frenzy, tirés au hasard à chaque poursuite côté serveur (synchronisés).
			case BEHAVIOR_CHASING -> moving ? RUN : IDLE;
			case BEHAVIOR_CHASING_FRENZY -> moving ? RUN_FRENZY : IDLE;
			// À tâtons vers un bruit.
			case BEHAVIOR_INVESTIGATING -> moving ? WALK_GROPE : IDLE;
			case BEHAVIOR_SEARCH_SNIFF -> SEARCH_SNIFF;
			case BEHAVIOR_SEARCH_LISTEN -> SEARCH_LISTEN;
			case BEHAVIOR_SEARCH_GROUND -> SEARCH_GROUND;
			default -> moving ? WALK : IDLE;
		});
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return geoCache;
	}
}
