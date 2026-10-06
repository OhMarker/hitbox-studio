package dev.hitboxstudio.render;

import dev.hitboxstudio.config.Category;
import dev.hitboxstudio.config.ProjectileKind;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.entity.vehicle.VehicleEntity;

/** Sorts entities into config sections. Results are cached per EntityType since an entity's class never changes. */
public final class EntityClassifier {
	private static final Map<EntityType<?>, Category> CATEGORY_CACHE = new IdentityHashMap<>();
	private static final Map<EntityType<?>, ProjectileKind> PROJECTILE_CACHE = new IdentityHashMap<>();

	private EntityClassifier() {
	}

	public static Category classify(Entity entity, Entity localPlayer) {
		if (entity == localPlayer) {
			return Category.SELF;
		}
		Category cached = CATEGORY_CACHE.get(entity.getType());
		if (cached == null) {
			cached = compute(entity);
			CATEGORY_CACHE.put(entity.getType(), cached);
		}
		return cached;
	}

	private static Category compute(Entity entity) {
		if (entity instanceof EndCrystal) return Category.CRYSTALS;
		if (entity instanceof Player) return Category.PLAYERS;
		if (entity instanceof ExperienceOrb) return Category.XP_ORBS;
		if (entity instanceof ItemEntity) return Category.ITEMS;
		if (entity instanceof Projectile) return Category.PROJECTILES;
		if (entity instanceof VehicleEntity) return Category.VEHICLES;
		if (entity instanceof ArmorStand) return Category.ARMOR_STANDS;
		if (entity instanceof BlockAttachedEntity) return Category.DECORATIONS;
		if (entity instanceof Enemy) return Category.HOSTILE;
		if (entity instanceof LivingEntity) return Category.PASSIVE;
		return Category.OTHER;
	}

	public static ProjectileKind projectileKind(Entity entity) {
		ProjectileKind cached = PROJECTILE_CACHE.get(entity.getType());
		if (cached == null) {
			cached = computeProjectile(entity);
			PROJECTILE_CACHE.put(entity.getType(), cached);
		}
		return cached;
	}

	private static ProjectileKind computeProjectile(Entity entity) {
		if (entity instanceof ThrownTrident) return ProjectileKind.TRIDENT;
		if (entity instanceof AbstractArrow) return ProjectileKind.ARROW;
		if (entity instanceof ThrownEnderpearl) return ProjectileKind.ENDER_PEARL;
		if (entity instanceof Snowball || entity instanceof ThrownEgg) return ProjectileKind.SNOWBALL_EGG;
		if (entity instanceof AbstractThrownPotion || entity instanceof ThrownExperienceBottle) return ProjectileKind.POTION;
		if (entity instanceof AbstractWindCharge) return ProjectileKind.WIND_CHARGE;
		if (entity instanceof AbstractHurtingProjectile) return ProjectileKind.FIREBALL;
		if (entity instanceof FireworkRocketEntity) return ProjectileKind.FIREWORK;
		if (entity instanceof FishingHook) return ProjectileKind.FISHING_HOOK;
		return ProjectileKind.OTHER;
	}
}
