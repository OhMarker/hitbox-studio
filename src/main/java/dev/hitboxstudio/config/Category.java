package dev.hitboxstudio.config;

/** Entity sections, in the order their tabs appear in the config screen. */
public enum Category {
	CRYSTALS("crystals"),
	PLAYERS("players"),
	SELF("self"),
	HOSTILE("hostile"),
	PASSIVE("passive"),
	ITEMS("items"),
	XP_ORBS("xp_orbs"),
	PROJECTILES("projectiles"),
	VEHICLES("vehicles"),
	ARMOR_STANDS("armor_stands"),
	DECORATIONS("decorations"),
	OTHER("other");

	public final String key;

	Category(String key) {
		this.key = key;
	}
}
