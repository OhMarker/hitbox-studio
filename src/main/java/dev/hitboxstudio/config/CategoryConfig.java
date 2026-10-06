package dev.hitboxstudio.config;

/** One entity section: on/off, and either the Default Style or its own custom style. */
public class CategoryConfig {
	public boolean enabled = true;
	public boolean customStyle = false;
	public HitboxStyle style = HitboxStyle.vanilla();

	public CategoryConfig() {
	}

	public CategoryConfig(boolean enabled) {
		this.enabled = enabled;
	}

	void sanitize() {
		if (this.style == null) this.style = HitboxStyle.vanilla();
		this.style.sanitize();
	}
}
