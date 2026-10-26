package net.melvunx.zombieexpansion.entity;

import net.melvunx.zombieexpansion.damage.DifficultyDamage;
import net.melvunx.zombieexpansion.faction.Faction;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

/**
 * Coureur : un zombie maigre et très rapide des Déterrés. Sa vitesse est sa force, il est fragile et frappe
 * moins fort : le joueur doit se battre en reculant, sans pouvoir le semer à pied.
 *
 * Stats : 12 PV (20 pour le zombie vanilla), vitesse ×1,4 (0,23 → 0,322), dégâts de mêlée 1 PV (0,5 cœur) sous
 * ceux du zombie vanilla à chaque difficulté. Les dégâts passent par {@link FixedDamageZombieEntity} : l'attribut
 * d'attaque est rééchelonné par le jeu contre un joueur, il ne donnerait pas exactement « vanilla - 1 PV ».
 * Plus faible que le zombie vanilla, il reste dans le budget de la faction (aucune exception à valider).
 *
 * Il brûle au soleil comme un zombie vanilla. Aucun bébé : un bébé est déjà vif, et le modèle est un adulte maigre.
 */
public class RunnerZombieEntity extends FixedDamageZombieEntity {
	private static final double MAX_HEALTH = 12.0;
	/** 40 % plus rapide que le zombie vanilla (0,23). */
	private static final double MOVEMENT_SPEED = 0.23 * 1.4;
	/** Zombie vanilla : 2,5 / 3 / 4,5 PV, moins 1 PV (0,5 cœur) à chaque difficulté. */
	private static final DifficultyDamage.Table DAMAGE = new DifficultyDamage.Table(1.5f, 2.0f, 3.5f);

	public RunnerZombieEntity(EntityType<? extends ZombieEntity> entityType, World world) {
		super(entityType, world);
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return ZombieEntity.createZombieAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, MAX_HEALTH)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, MOVEMENT_SPEED);
	}

	/** Règle vanilla (sombre, au sol) + territoire des Déterrés (surface et grottes jusqu'à Y -8). */
	public static boolean canSpawn(EntityType<RunnerZombieEntity> type, ServerWorldAccess world, SpawnReason reason, BlockPos pos, Random random) {
		boolean natural = reason == SpawnReason.NATURAL || reason == SpawnReason.CHUNK_GENERATION;
		return HostileEntity.canSpawnInDark(type, world, reason, pos, random)
			&& (!natural || Faction.allowsUnearthedSpawn(world, pos));
	}

	@Override
	protected DifficultyDamage.Table getDamageTable() {
		return DAMAGE;
	}

	@Override
	public void setBaby(boolean baby) {
		super.setBaby(false);
	}
}
