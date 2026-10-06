package dev.hitboxstudio.gui;

import dev.hitboxstudio.config.HitboxConfig;
import dev.hitboxstudio.config.HitboxStyle;
import dev.hitboxstudio.config.PartStyle;
import dev.hitboxstudio.render.ColorMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * Slowly spinning 3D hitbox drawn with 2D projected quads, using the exact same color/width/opacity math
 * as the in-world renderer, so what you see here is what you get in game.
 */
public final class PreviewPanel {
	/** Everything the preview needs for one frame. */
	public record State(HitboxStyle style, PreviewShape shape, HitboxConfig config, int highlight, @Nullable Component caption, boolean visible) {
	}

	private static final int[][] FACES = {
		{0, 2, 6, 4}, {1, 3, 7, 5}, {0, 1, 5, 4}, {2, 3, 7, 6}, {0, 1, 3, 2}, {4, 5, 7, 6}
	};

	private final float[] px = new float[8];
	private final float[] py = new float[8];
	private final float[] pz = new float[8];
	private final float[] out = new float[3];
	private final int[] faceOrder = new int[6];
	private final float[] faceDepth = new float[6];

	private float yaw;
	private float pitch;
	private float scale;
	private float centerX;
	private float centerY;
	private float cameraDistance;
	private float halfHeight;

	public void render(GuiGraphics g, Font font, int x, int y, int w, int h, State state) {
		HitboxStyle style = state.style();
		PreviewShape shape = state.shape();
		HitboxConfig config = state.config();
		int highlight = state.highlight();
		// No panel, just text and the hitbox over the menu background, like the player model in skin settings.
		Component title = Component.translatable("hitboxstudio.preview.title", Component.translatable("hitboxstudio.preview.shape." + shape.key));
		g.drawCenteredString(font, title, x + w / 2, y + 2, 0xFFFFFFFF);
		if (state.caption() != null) {
			g.drawCenteredString(font, state.caption(), x + w / 2, y + h - 10, 0xFFFFFF55);
		}

		HitboxConfig.General general = config.general;
		if (!state.visible() || !general.enabled) {
			Component text = Component.translatable(general.enabled ? "hitboxstudio.preview.section_off" : "hitboxstudio.preview.mod_off");
			g.drawCenteredString(font, text, x + w / 2, y + h / 2 - 4, 0xFFA0A0A0);
			return;
		}

		g.enableScissor(x, y + 14, x + w, y + h - 12);
		long now = Util.getMillis();
		this.setupCamera(x, y, w, h, style, shape, now);
		float guiScale = Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());

		this.drawFloor(g, shape);

		float expand = style.boxExpand;
		float minHalf = Math.min(Math.min(shape.width, shape.height), shape.depth) / 2.0F;
		expand = Math.max(expand, -minHalf + 0.005F);
		float bx = shape.width / 2.0F + expand;
		float bz = shape.depth / 2.0F + expand;
		float by0 = -expand;
		float by1 = shape.height + expand;
		for (int i = 0; i < 8; i++) {
			this.project((i & 1) == 0 ? -bx : bx, (i & 2) == 0 ? by0 : by1, (i & 4) == 0 ? -bz : bz);
			this.px[i] = this.out[0];
			this.py[i] = this.out[1];
			this.pz[i] = this.out[2];
		}

		// Fill: faces painted back to front so translucency stacks correctly.
		if (style.fill.enabled) {
			int color = ColorMath.animated(style.fill, now);
			if (config.highlights.applyToFill) {
				color = ColorMath.applyHighlight(color, highlight, config.highlights.blendMode);
			}
			int fill = ColorMath.finalColor(color, style.fill.opacity, general.opacityMultiplier, 1.0F);
			if (fill != 0) {
				for (int f = 0; f < 6; f++) {
					int[] face = FACES[f];
					this.faceOrder[f] = f;
					this.faceDepth[f] = this.pz[face[0]] + this.pz[face[1]] + this.pz[face[2]] + this.pz[face[3]];
				}
				for (int i = 1; i < 6; i++) {
					for (int j = i; j > 0 && this.faceDepth[this.faceOrder[j]] > this.faceDepth[this.faceOrder[j - 1]]; j--) {
						int tmp = this.faceOrder[j];
						this.faceOrder[j] = this.faceOrder[j - 1];
						this.faceOrder[j - 1] = tmp;
					}
				}
				for (int f : this.faceOrder) {
					int[] face = FACES[f];
					GuiDraw.quad(g, this.px[face[0]], this.py[face[0]], this.px[face[1]], this.py[face[1]],
						this.px[face[2]], this.py[face[2]], this.px[face[3]], this.py[face[3]], fill);
				}
			}
		}

		if (style.box.enabled) {
			float widthPx = style.box.width * general.lineWidthMultiplier;
			int color = ColorMath.applyHighlight(ColorMath.animated(style.box, now), highlight, config.highlights.blendMode);
			int stroke = ColorMath.finalColor(color, style.box.opacity, general.opacityMultiplier, widthPx);
			float lineWidth = ColorMath.drawnWidth(widthPx) / guiScale;
			if (stroke != 0) {
				for (int i = 0; i < 8; i++) {
					for (int bit = 1; bit <= 4; bit <<= 1) {
						if ((i & bit) == 0) {
							int j = i | bit;
							GuiDraw.line(g, this.px[i], this.py[i], this.px[j], this.py[j], lineWidth, stroke);
						}
					}
				}
			}
		}

		if (shape.living && style.eyeLine.enabled) {
			float widthPx = style.eyeLine.width * general.lineWidthMultiplier;
			int color = this.partColor(style.eyeLine, general, widthPx, now);
			if (color != 0) {
				float ex = shape.width / 2.0F;
				float ez = shape.depth / 2.0F;
				float lw = ColorMath.drawnWidth(widthPx) / guiScale;
				this.line3d(g, -ex, shape.eyeHeight, -ez, ex, shape.eyeHeight, -ez, lw, color);
				this.line3d(g, ex, shape.eyeHeight, -ez, ex, shape.eyeHeight, ez, lw, color);
				this.line3d(g, ex, shape.eyeHeight, ez, -ex, shape.eyeHeight, ez, lw, color);
				this.line3d(g, -ex, shape.eyeHeight, ez, -ex, shape.eyeHeight, -ez, lw, color);
			}
		}

		if (style.lookVector.enabled) {
			float widthPx = style.lookVector.width * general.lineWidthMultiplier;
			int color = this.partColor(style.lookVector, general, widthPx, now);
			if (color != 0) {
				float lw = ColorMath.drawnWidth(widthPx) / guiScale;
				float length = this.visibleLookLength(style, shape);
				float eye = shape.eyeHeight;
				this.line3d(g, 0, eye, 0, 0, eye, length, lw, color);
				if (style.lookArrowHead) {
					// Same arrow head shape as vanilla's ArrowGizmo, pointing along +Z.
					float head = Math.max(0.1F, Math.min(1.0F, length * 0.1F));
					this.line3d(g, 0, eye + head, length - head, 0, eye, length, lw, color);
					this.line3d(g, -head, eye, length - head, 0, eye, length, lw, color);
					this.line3d(g, 0, eye - head, length - head, 0, eye, length, lw, color);
					this.line3d(g, head, eye, length - head, 0, eye, length, lw, color);
				}
			}
		}

		if (style.positionDot.enabled) {
			float sizePx = style.positionDot.width * general.lineWidthMultiplier;
			int color = this.partColor(style.positionDot, general, sizePx, now);
			if (color != 0) {
				this.project(0, 0, 0);
				float s = Math.max(ColorMath.drawnWidth(sizePx) / guiScale, 1.0F) / 2.0F + 0.5F;
				GuiDraw.quad(g, this.out[0] - s, this.out[1] - s, this.out[0] + s, this.out[1] - s, this.out[0] + s, this.out[1] + s, this.out[0] - s, this.out[1] + s, color);
			}
		}

		g.disableScissor();
	}

	private int partColor(PartStyle part, HitboxConfig.General general, float widthPx, long now) {
		return ColorMath.finalColor(ColorMath.animated(part, now), part.opacity, general.opacityMultiplier, widthPx);
	}

	private float visibleLookLength(HitboxStyle style, PreviewShape shape) {
		float maxDim = Math.max(shape.width, Math.max(shape.height, shape.depth));
		return Math.min(style.lookLength, maxDim * 1.6F + 0.4F);
	}

	private void setupCamera(int x, int y, int w, int h, HitboxStyle style, PreviewShape shape, long now) {
		this.yaw = (float) ((now / 1000.0 * 0.6) % (Math.PI * 2.0));
		this.pitch = 0.42F;
		this.halfHeight = shape.height / 2.0F;
		float boxRadius = (float) Math.sqrt(shape.width * shape.width + shape.height * shape.height + shape.depth * shape.depth) / 2.0F
			+ Math.max(0.0F, style.boxExpand);
		float radius = boxRadius;
		if (style.lookVector.enabled) {
			float length = this.visibleLookLength(style, shape);
			float dy = shape.eyeHeight - this.halfHeight;
			radius = Math.max(radius, (float) Math.sqrt(dy * dy + length * length));
		}
		radius = Math.max(radius, 0.3F);
		int innerTop = y + 16;
		int innerBottom = y + h - 16;
		this.centerX = x + w / 2.0F;
		this.centerY = (innerTop + innerBottom) / 2.0F;
		this.scale = Math.min(w - 16, innerBottom - innerTop) * 0.46F / radius;
		this.cameraDistance = radius * 4.0F;
	}

	/** Projects a point in entity space (feet at y=0) into GUI coordinates; writes x, y, depth into out. */
	private void project(float x, float y, float z) {
		y -= this.halfHeight;
		float cosYaw = (float) Math.cos(this.yaw);
		float sinYaw = (float) Math.sin(this.yaw);
		float cosPitch = (float) Math.cos(this.pitch);
		float sinPitch = (float) Math.sin(this.pitch);
		float x1 = x * cosYaw + z * sinYaw;
		float z1 = -x * sinYaw + z * cosYaw;
		float y2 = y * cosPitch + z1 * sinPitch;
		float z2 = -y * sinPitch + z1 * cosPitch;
		float perspective = this.cameraDistance / (this.cameraDistance + z2);
		this.out[0] = this.centerX + x1 * this.scale * perspective;
		this.out[1] = this.centerY - y2 * this.scale * perspective;
		this.out[2] = z2;
	}

	private void line3d(GuiGraphics g, float x0, float y0, float z0, float x1, float y1, float z1, float width, int color) {
		this.project(x0, y0, z0);
		float ax = this.out[0];
		float ay = this.out[1];
		this.project(x1, y1, z1);
		GuiDraw.line(g, ax, ay, this.out[0], this.out[1], width, color);
	}

	private void drawFloor(GuiGraphics g, PreviewShape shape) {
		float size = Math.max(shape.width, shape.depth) * 0.9F + 0.4F;
		int steps = 4;
		for (int i = 0; i <= steps; i++) {
			float t = -size + 2.0F * size * i / steps;
			this.line3d(g, -size, 0, t, size, 0, t, 0.5F, 0x28FFFFFF);
			this.line3d(g, t, 0, -size, t, 0, size, 0.5F, 0x28FFFFFF);
		}
	}
}
