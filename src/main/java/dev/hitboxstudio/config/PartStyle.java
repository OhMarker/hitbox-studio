package dev.hitboxstudio.config;

import com.google.gson.annotations.JsonAdapter;

/**
 * Look of one hitbox part (box outline, eye line, look vector, ...).
 * Width is in screen pixels, same unit vanilla gizmos use (vanilla default is 2.5).
 */
public final class PartStyle {
	public boolean enabled = true;
	@JsonAdapter(HexColorAdapter.class)
	public int color = 0xFFFFFFFF;
	public float width = 2.5F;
	public float opacity = 1.0F;
	public boolean rainbow = false;
	public float rainbowSpeed = 1.0F;

	public PartStyle() {
	}

	public PartStyle(boolean enabled, int color, float width, float opacity) {
		this.enabled = enabled;
		this.color = color;
		this.width = width;
		this.opacity = opacity;
	}

	public PartStyle copy() {
		PartStyle copy = new PartStyle(this.enabled, this.color, this.width, this.opacity);
		copy.rainbow = this.rainbow;
		copy.rainbowSpeed = this.rainbowSpeed;
		return copy;
	}

	public void set(PartStyle other) {
		this.enabled = other.enabled;
		this.color = other.color;
		this.width = other.width;
		this.opacity = other.opacity;
		this.rainbow = other.rainbow;
		this.rainbowSpeed = other.rainbowSpeed;
	}

	void sanitize(PartStyle defaults) {
		if (!Float.isFinite(this.width)) this.width = defaults.width;
		if (!Float.isFinite(this.opacity)) this.opacity = defaults.opacity;
		if (!Float.isFinite(this.rainbowSpeed)) this.rainbowSpeed = defaults.rainbowSpeed;
		this.width = clamp(this.width, 0.1F, 10.0F);
		this.opacity = clamp(this.opacity, 0.0F, 1.0F);
		this.rainbowSpeed = clamp(this.rainbowSpeed, 0.1F, 5.0F);
	}

	static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}
}
