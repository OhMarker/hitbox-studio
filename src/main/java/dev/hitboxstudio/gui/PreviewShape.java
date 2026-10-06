package dev.hitboxstudio.gui;

import dev.hitboxstudio.config.Category;

/** Box sizes used by the live preview, matching the real vanilla entity dimensions. */
public enum PreviewShape {
	PLAYER("player", 0.6F, 1.8F, 0.6F, 1.62F, true),
	CRYSTAL("crystal", 2.0F, 2.0F, 2.0F, 1.7F, false),
	ZOMBIE("zombie", 0.6F, 1.95F, 0.6F, 1.74F, true),
	PIG("pig", 0.9F, 0.9F, 0.9F, 0.765F, true),
	ITEM("item", 0.25F, 0.25F, 0.25F, 0.2125F, false),
	XP_ORB("xp_orb", 0.5F, 0.5F, 0.5F, 0.425F, false),
	ARROW("arrow", 0.5F, 0.5F, 0.5F, 0.13F, false),
	BOAT("boat", 1.375F, 0.5625F, 1.375F, 0.5625F, false),
	ARMOR_STAND("armor_stand", 0.5F, 1.975F, 0.5F, 1.7775F, true),
	PAINTING("painting", 1.0F, 1.0F, 0.0625F, 0.85F, false),
	OTHER("box", 0.98F, 0.98F, 0.98F, 0.833F, false);

	public final String key;
	public final float width;
	public final float height;
	public final float depth;
	public final float eyeHeight;
	/** Vanilla only draws the eye line on living entities. */
	public final boolean living;

	PreviewShape(String key, float width, float height, float depth, float eyeHeight, boolean living) {
		this.key = key;
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.eyeHeight = eyeHeight;
		this.living = living;
	}

	public static PreviewShape forCategory(Category category) {
		return switch (category) {
			case CRYSTALS -> CRYSTAL;
			case PLAYERS, SELF -> PLAYER;
			case HOSTILE -> ZOMBIE;
			case PASSIVE -> PIG;
			case ITEMS -> ITEM;
			case XP_ORBS -> XP_ORB;
			case PROJECTILES -> ARROW;
			case VEHICLES -> BOAT;
			case ARMOR_STANDS -> ARMOR_STAND;
			case DECORATIONS -> PAINTING;
			case OTHER -> OTHER;
		};
	}
}
