package dev.hitboxstudio.config;

/** Projectiles section, with a switch for each kind of projectile. */
public final class ProjectileConfig extends CategoryConfig {
	public boolean arrows = true;
	public boolean tridents = true;
	public boolean enderPearls = true;
	public boolean snowballsAndEggs = true;
	public boolean potions = true;
	public boolean fireballs = true;
	public boolean windCharges = true;
	public boolean fireworks = true;
	public boolean fishingHooks = true;
	public boolean otherProjectiles = true;

	public boolean isShown(ProjectileKind kind) {
		return switch (kind) {
			case ARROW -> this.arrows;
			case TRIDENT -> this.tridents;
			case ENDER_PEARL -> this.enderPearls;
			case SNOWBALL_EGG -> this.snowballsAndEggs;
			case POTION -> this.potions;
			case FIREBALL -> this.fireballs;
			case WIND_CHARGE -> this.windCharges;
			case FIREWORK -> this.fireworks;
			case FISHING_HOOK -> this.fishingHooks;
			case OTHER -> this.otherProjectiles;
		};
	}
}
