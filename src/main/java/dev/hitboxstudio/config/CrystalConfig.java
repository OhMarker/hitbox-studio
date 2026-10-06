package dev.hitboxstudio.config;

/** End crystal section. Uses its own style by default so the useless eye line / look arrow are hidden. */
public final class CrystalConfig extends CategoryConfig {
	/** Outline/fill color while your crosshair is on the crystal. */
	public PartStyle targetColor = new PartStyle(false, 0xFFFF3B3B, 2.5F, 1.0F);
	/** Outline/fill color while the crystal is inside your entity interaction range. */
	public PartStyle inReachColor = new PartStyle(false, 0xFF4CFF6A, 2.5F, 1.0F);

	public CrystalConfig() {
		this.customStyle = true;
		this.style = HitboxStyle.crystalDefault();
	}

	@Override
	void sanitize() {
		super.sanitize();
		CrystalConfig defaults = new CrystalConfig();
		if (this.targetColor == null) this.targetColor = defaults.targetColor;
		if (this.inReachColor == null) this.inReachColor = defaults.inReachColor;
		this.targetColor.sanitize(defaults.targetColor);
		this.inReachColor.sanitize(defaults.inReachColor);
	}
}
