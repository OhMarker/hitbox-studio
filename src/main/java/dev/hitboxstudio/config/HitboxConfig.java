package dev.hitboxstudio.config;

import java.util.ArrayList;
import java.util.List;

/** Root of config/hitboxstudio.json. Defaults look exactly like vanilla F3+B, except XP orbs are hidden. */
public final class HitboxConfig {
	public int configVersion = 1;
	public General general = new General();
	public HitboxStyle defaultStyle = HitboxStyle.vanilla();
	public CrystalConfig crystals = new CrystalConfig();
	public CategoryConfig players = new CategoryConfig();
	public CategoryConfig self = new CategoryConfig();
	public CategoryConfig hostile = new CategoryConfig();
	public CategoryConfig passive = new CategoryConfig();
	public CategoryConfig items = new CategoryConfig();
	public CategoryConfig xpOrbs = new CategoryConfig(false);
	public ProjectileConfig projectiles = new ProjectileConfig();
	public CategoryConfig vehicles = new CategoryConfig();
	public CategoryConfig armorStands = new CategoryConfig();
	public CategoryConfig decorations = new CategoryConfig();
	public CategoryConfig other = new CategoryConfig();
	public Highlights highlights = new Highlights();
	/** Last colors picked in the color wheel, newest first, as #AARRGGBB. */
	public List<String> recentColors = new ArrayList<>();

	public enum Mode {
		FOLLOW_F3B,
		ALWAYS_ON
	}

	public enum BlendMode {
		REPLACE,
		MIX
	}

	public static final class General {
		public boolean enabled = true;
		public Mode mode = Mode.FOLLOW_F3B;
		public boolean hideInF1 = false;
		/** In blocks. 0 means unlimited. */
		public float maxDistance = 0.0F;
		public float lineWidthMultiplier = 1.0F;
		public float opacityMultiplier = 1.0F;
		/** Entity type IDs that never get a hitbox, e.g. "minecraft:experience_orb". */
		public List<String> hiddenEntities = new ArrayList<>();
	}

	/** Dynamic colors for every section except crystals (crystals have their own on the Crystals tab). */
	public static final class Highlights {
		public BlendMode blendMode = BlendMode.REPLACE;
		public boolean applyToFill = true;
		public PartStyle target = new PartStyle(false, 0xFFFFAA00, 2.5F, 1.0F);
		public PartStyle inReach = new PartStyle(false, 0xFF55FF55, 2.5F, 1.0F);
		public PartStyle hurt = new PartStyle(false, 0xFFFF5555, 2.5F, 1.0F);
	}

	public CategoryConfig get(Category category) {
		return switch (category) {
			case CRYSTALS -> this.crystals;
			case PLAYERS -> this.players;
			case SELF -> this.self;
			case HOSTILE -> this.hostile;
			case PASSIVE -> this.passive;
			case ITEMS -> this.items;
			case XP_ORBS -> this.xpOrbs;
			case PROJECTILES -> this.projectiles;
			case VEHICLES -> this.vehicles;
			case ARMOR_STANDS -> this.armorStands;
			case DECORATIONS -> this.decorations;
			case OTHER -> this.other;
		};
	}

	public void reset(Category category) {
		switch (category) {
			case CRYSTALS -> this.crystals = new CrystalConfig();
			case PLAYERS -> this.players = new CategoryConfig();
			case SELF -> this.self = new CategoryConfig();
			case HOSTILE -> this.hostile = new CategoryConfig();
			case PASSIVE -> this.passive = new CategoryConfig();
			case ITEMS -> this.items = new CategoryConfig();
			case XP_ORBS -> this.xpOrbs = new CategoryConfig(false);
			case PROJECTILES -> this.projectiles = new ProjectileConfig();
			case VEHICLES -> this.vehicles = new CategoryConfig();
			case ARMOR_STANDS -> this.armorStands = new CategoryConfig();
			case DECORATIONS -> this.decorations = new CategoryConfig();
			case OTHER -> this.other = new CategoryConfig();
		}
	}

	/** The style actually used to draw a section: its own when "Use custom style" is on, else the Default Style. */
	public HitboxStyle effectiveStyle(Category category) {
		CategoryConfig section = this.get(category);
		return section.customStyle ? section.style : this.defaultStyle;
	}

	/** Fixes anything missing or broken after loading (old or hand-edited files). */
	public void sanitize() {
		HitboxConfig defaults = new HitboxConfig();
		if (this.general == null) this.general = defaults.general;
		if (this.general.mode == null) this.general.mode = Mode.FOLLOW_F3B;
		if (this.general.hiddenEntities == null) this.general.hiddenEntities = new ArrayList<>();
		this.general.hiddenEntities.removeIf(s -> s == null || s.isBlank());
		if (!Float.isFinite(this.general.maxDistance)) this.general.maxDistance = 0.0F;
		if (!Float.isFinite(this.general.lineWidthMultiplier)) this.general.lineWidthMultiplier = 1.0F;
		if (!Float.isFinite(this.general.opacityMultiplier)) this.general.opacityMultiplier = 1.0F;
		this.general.maxDistance = PartStyle.clamp(this.general.maxDistance, 0.0F, 512.0F);
		this.general.lineWidthMultiplier = PartStyle.clamp(this.general.lineWidthMultiplier, 0.1F, 5.0F);
		this.general.opacityMultiplier = PartStyle.clamp(this.general.opacityMultiplier, 0.0F, 1.0F);

		if (this.defaultStyle == null) this.defaultStyle = HitboxStyle.vanilla();
		this.defaultStyle.sanitize();

		for (Category category : Category.values()) {
			if (this.get(category) == null) this.reset(category);
			this.get(category).sanitize();
		}

		if (this.highlights == null) this.highlights = defaults.highlights;
		if (this.highlights.blendMode == null) this.highlights.blendMode = BlendMode.REPLACE;
		if (this.highlights.target == null) this.highlights.target = defaults.highlights.target;
		if (this.highlights.inReach == null) this.highlights.inReach = defaults.highlights.inReach;
		if (this.highlights.hurt == null) this.highlights.hurt = defaults.highlights.hurt;
		this.highlights.target.sanitize(defaults.highlights.target);
		this.highlights.inReach.sanitize(defaults.highlights.inReach);
		this.highlights.hurt.sanitize(defaults.highlights.hurt);

		if (this.recentColors == null) this.recentColors = new ArrayList<>();
		this.recentColors.removeIf(s -> HexColorAdapter.parse(s) == null);
		while (this.recentColors.size() > 8) this.recentColors.remove(this.recentColors.size() - 1);
	}
}
