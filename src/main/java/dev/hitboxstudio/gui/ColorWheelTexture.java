package dev.hitboxstudio.gui;

import com.mojang.blaze3d.platform.NativeImage;
import dev.hitboxstudio.HitboxStudio;
import dev.hitboxstudio.render.ColorMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/** HSV wheel texture (hue = angle, saturation = distance from center), rebuilt when brightness changes. */
final class ColorWheelTexture {
	static final Identifier ID = Identifier.fromNamespaceAndPath(HitboxStudio.MOD_ID, "dynamic/color_wheel");
	static final int SIZE = 256;
	private static DynamicTexture texture;
	private static int builtValue = -1;

	private ColorWheelTexture() {
	}

	static Identifier get(float value) {
		if (texture == null) {
			texture = new DynamicTexture(() -> "Hitbox Studio color wheel", SIZE, SIZE, true);
			Minecraft.getInstance().getTextureManager().register(ID, texture);
			builtValue = -1;
		}
		int quantized = Math.round(Math.max(0.0F, Math.min(1.0F, value)) * 255.0F);
		if (quantized != builtValue) {
			builtValue = quantized;
			rebuild(quantized / 255.0F);
		}
		return ID;
	}

	private static void rebuild(float value) {
		NativeImage image = texture.getPixels();
		if (image == null) {
			return;
		}
		float radius = SIZE / 2.0F;
		for (int py = 0; py < SIZE; py++) {
			for (int px = 0; px < SIZE; px++) {
				float dx = (px + 0.5F - radius) / radius;
				float dy = (py + 0.5F - radius) / radius;
				float distance = (float) Math.sqrt(dx * dx + dy * dy);
				// Soft 1px edge so the circle isn't jagged.
				float edge = Math.max(0.0F, Math.min(1.0F, (1.0F - distance) * radius + 0.5F));
				if (edge <= 0.0F) {
					image.setPixel(px, py, 0);
					continue;
				}
				float hue = (float) (Math.atan2(dy, dx) / (Math.PI * 2.0));
				if (hue < 0.0F) {
					hue += 1.0F;
				}
				int rgb = ColorMath.hsvToRgb(hue, Math.min(distance, 1.0F), value);
				image.setPixel(px, py, ColorMath.withAlpha(rgb, Math.round(edge * 255.0F)));
			}
		}
		texture.upload();
	}
}
