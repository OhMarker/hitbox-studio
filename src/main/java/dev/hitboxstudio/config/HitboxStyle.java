package dev.hitboxstudio.config;

/** Full look of a hitbox: every part vanilla F3+B draws, plus fill. Defaults match vanilla 1.21.11 exactly. */
public final class HitboxStyle {
	public static final int VANILLA_WHITE = 0xFFFFFFFF;
	public static final int VANILLA_RED = 0xFFFF0000;
	public static final int VANILLA_BLUE = 0xFF0000FF;
	public static final int VANILLA_YELLOW = 0xFFFFFF00;
	public static final int VANILLA_DRAGON_PART = 0xFF40FF00;

	/** The white bounding box. */
	public PartStyle box = new PartStyle(true, VANILLA_WHITE, 2.5F, 1.0F);
	/** Visual-only grow/shrink of the drawn box, in blocks. Does not change the real hitbox. */
	public float boxExpand = 0.0F;
	/** Translucent faces inside the bounding box (not in vanilla). */
	public PartStyle fill = new PartStyle(false, VANILLA_WHITE, 2.5F, 0.25F);
	/** The red rectangle at eye height (living entities only, like vanilla). */
	public PartStyle eyeLine = new PartStyle(true, VANILLA_RED, 2.5F, 1.0F);
	/** The blue arrow in the look direction. */
	public PartStyle lookVector = new PartStyle(true, VANILLA_BLUE, 2.5F, 1.0F);
	public float lookLength = 2.0F;
	public boolean lookArrowHead = true;
	/** The small dot at the entity's feet position. Width is used as the dot size. */
	public PartStyle positionDot = new PartStyle(true, VANILLA_WHITE, 2.0F, 1.0F);
	/** The flat yellow box showing where a passenger sits on its vehicle. */
	public PartStyle rideSeat = new PartStyle(true, VANILLA_YELLOW, 2.5F, 1.0F);
	/** Ender Dragon part boxes. */
	public PartStyle multipart = new PartStyle(true, VANILLA_DRAGON_PART, 2.5F, 1.0F);

	public static HitboxStyle vanilla() {
		return new HitboxStyle();
	}

	/** End crystals are not living, so vanilla never draws their eye line; the look arrow is useless on them too. */
	public static HitboxStyle crystalDefault() {
		HitboxStyle style = new HitboxStyle();
		style.eyeLine.enabled = false;
		style.lookVector.enabled = false;
		return style;
	}

	public HitboxStyle copy() {
		HitboxStyle copy = new HitboxStyle();
		copy.set(this);
		return copy;
	}

	public void set(HitboxStyle other) {
		this.box.set(other.box);
		this.boxExpand = other.boxExpand;
		this.fill.set(other.fill);
		this.eyeLine.set(other.eyeLine);
		this.lookVector.set(other.lookVector);
		this.lookLength = other.lookLength;
		this.lookArrowHead = other.lookArrowHead;
		this.positionDot.set(other.positionDot);
		this.rideSeat.set(other.rideSeat);
		this.multipart.set(other.multipart);
	}

	void sanitize() {
		HitboxStyle defaults = vanilla();
		if (this.box == null) this.box = defaults.box;
		if (this.fill == null) this.fill = defaults.fill;
		if (this.eyeLine == null) this.eyeLine = defaults.eyeLine;
		if (this.lookVector == null) this.lookVector = defaults.lookVector;
		if (this.positionDot == null) this.positionDot = defaults.positionDot;
		if (this.rideSeat == null) this.rideSeat = defaults.rideSeat;
		if (this.multipart == null) this.multipart = defaults.multipart;
		this.box.sanitize(defaults.box);
		this.fill.sanitize(defaults.fill);
		this.eyeLine.sanitize(defaults.eyeLine);
		this.lookVector.sanitize(defaults.lookVector);
		this.positionDot.sanitize(defaults.positionDot);
		this.rideSeat.sanitize(defaults.rideSeat);
		this.multipart.sanitize(defaults.multipart);
		if (!Float.isFinite(this.boxExpand)) this.boxExpand = 0.0F;
		if (!Float.isFinite(this.lookLength)) this.lookLength = 2.0F;
		this.boxExpand = PartStyle.clamp(this.boxExpand, -0.5F, 0.5F);
		this.lookLength = PartStyle.clamp(this.lookLength, 0.5F, 10.0F);
	}
}
