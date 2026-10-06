package dev.hitboxstudio.render;

import dev.hitboxstudio.config.HitboxConfig;
import dev.hitboxstudio.config.PartStyle;

/** Color helpers shared by the world renderer, the preview and the color wheel. All colors are ARGB ints. */
public final class ColorMath {
	/** Marker for "no highlight color". Real highlight colors always have alpha forced to 0xFF. */
	public static final int NO_HIGHLIGHT = 0;

	private ColorMath() {
	}

	public static int alpha(int argb) {
		return argb >>> 24;
	}

	public static int rgb(int argb) {
		return argb & 0xFFFFFF;
	}

	public static int withAlpha(int argb, int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0xFFFFFF);
	}

	/** h, s, v in 0..1. Returns an opaque RGB color. */
	public static int hsvToRgb(float h, float s, float v) {
		h = h - (float) Math.floor(h);
		float r;
		float g;
		float b;
		float scaled = h * 6.0F;
		int sector = (int) scaled;
		float f = scaled - sector;
		float p = v * (1.0F - s);
		float q = v * (1.0F - f * s);
		float t = v * (1.0F - (1.0F - f) * s);
		switch (sector % 6) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return 0xFF000000 | (Math.round(r * 255.0F) << 16) | (Math.round(g * 255.0F) << 8) | Math.round(b * 255.0F);
	}

	/** Writes {h, s, v} (0..1) into out. */
	public static void rgbToHsv(int argb, float[] out) {
		float r = ((argb >> 16) & 0xFF) / 255.0F;
		float g = ((argb >> 8) & 0xFF) / 255.0F;
		float b = (argb & 0xFF) / 255.0F;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float delta = max - min;
		float h;
		if (delta < 1.0E-6F) {
			h = 0.0F;
		} else if (max == r) {
			h = ((g - b) / delta) / 6.0F;
		} else if (max == g) {
			h = ((b - r) / delta + 2.0F) / 6.0F;
		} else {
			h = ((r - g) / delta + 4.0F) / 6.0F;
		}
		if (h < 0.0F) h += 1.0F;
		out[0] = h;
		out[1] = max < 1.0E-6F ? 0.0F : delta / max;
		out[2] = max;
	}

	/** RGB lerp; alpha taken from a. */
	public static int mix(int a, int b, float t) {
		int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
		int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
		int r = Math.round(ar + (br - ar) * t);
		int g = Math.round(ag + (bg - ag) * t);
		int bl = Math.round(ab + (bb - ab) * t);
		return (a & 0xFF000000) | (r << 16) | (g << 8) | bl;
	}

	/** The part's base color with rainbow applied (alpha kept from the stored color). */
	public static int animated(PartStyle part, long timeMillis) {
		if (!part.rainbow) {
			return part.color;
		}
		float hue = (float) ((timeMillis * (double) part.rainbowSpeed / 4000.0) % 1.0);
		return withAlpha(hsvToRgb(hue, 1.0F, 1.0F), alpha(part.color));
	}

	/** Highlight color as opaque RGB, or NO_HIGHLIGHT when the highlight is off. */
	public static int highlightOf(PartStyle highlight, long timeMillis) {
		return highlight.enabled ? (animated(highlight, timeMillis) | 0xFF000000) : NO_HIGHLIGHT;
	}

	public static int applyHighlight(int argb, int highlight, HitboxConfig.BlendMode mode) {
		if (highlight == NO_HIGHLIGHT) {
			return argb;
		}
		return mode == HitboxConfig.BlendMode.MIX ? mix(argb, highlight, 0.5F) : (argb & 0xFF000000) | (highlight & 0xFFFFFF);
	}

	/**
	 * Final color for a line: color alpha x part opacity x global opacity. Lines thinner than one pixel
	 * are drawn one pixel wide and faded instead, because sub-pixel lines break up into dots on most GPUs.
	 * Returns 0 when nothing would be visible.
	 */
	public static int finalColor(int argb, float opacity, float globalOpacity, float widthPixels) {
		float a = alpha(argb) / 255.0F * opacity * globalOpacity;
		if (widthPixels < 1.0F) {
			a *= Math.max(widthPixels, 0.0F);
		}
		int alpha = Math.round(a * 255.0F);
		if (alpha <= 0) {
			return 0;
		}
		return (Math.min(alpha, 255) << 24) | (argb & 0xFFFFFF);
	}

	public static float drawnWidth(float widthPixels) {
		return Math.max(1.0F, widthPixels);
	}
}
