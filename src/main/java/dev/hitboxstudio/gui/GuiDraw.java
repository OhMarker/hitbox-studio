package dev.hitboxstudio.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/** Small drawing helpers on top of GuiGraphics: free-form quads and lines (for the preview) and checkerboards (behind colors). */
public final class GuiDraw {
	private GuiDraw() {
	}

	/** Any convex quad. Winding is fixed up to match vanilla GUI quads so backface culling never eats it. */
	public static void quad(GuiGraphics g, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
		if ((color >>> 24) == 0) {
			return;
		}
		// Vanilla GUI quads have a negative shoelace sum in screen space (y down).
		float area = (x0 * y1 - x1 * y0) + (x1 * y2 - x2 * y1) + (x2 * y3 - x3 * y2) + (x3 * y0 - x0 * y3);
		if (area > 0.0F) {
			float tx = x1;
			float ty = y1;
			x1 = x3;
			y1 = y3;
			x3 = tx;
			y3 = ty;
		}
		Matrix3x2f pose = new Matrix3x2f(g.pose());
		ScreenRectangle scissor = g.scissorStack.peek();
		int minX = (int) Math.floor(Math.min(Math.min(x0, x1), Math.min(x2, x3)));
		int minY = (int) Math.floor(Math.min(Math.min(y0, y1), Math.min(y2, y3)));
		int maxX = (int) Math.ceil(Math.max(Math.max(x0, x1), Math.max(x2, x3)));
		int maxY = (int) Math.ceil(Math.max(Math.max(y0, y1), Math.max(y2, y3)));
		ScreenRectangle bounds = new ScreenRectangle(minX, minY, Math.max(1, maxX - minX), Math.max(1, maxY - minY)).transformMaxBounds(pose);
		if (scissor != null) {
			bounds = scissor.intersection(bounds);
			if (bounds == null) {
				return;
			}
		}
		g.guiRenderState.submitGuiElement(new QuadState(pose, x0, y0, x1, y1, x2, y2, x3, y3, color, scissor, bounds));
	}

	/** A straight line of the given width (GUI units) between two points. */
	public static void line(GuiGraphics g, float xa, float ya, float xb, float yb, float width, int color) {
		float dx = xb - xa;
		float dy = yb - ya;
		float length = (float) Math.sqrt(dx * dx + dy * dy);
		if (length < 1.0E-4F) {
			return;
		}
		float half = width / 2.0F;
		float nx = -dy / length * half;
		float ny = dx / length * half;
		// Extend the ends by half the width so corners of boxes join without notches.
		float ex = dx / length * half;
		float ey = dy / length * half;
		quad(g, xa - ex + nx, ya - ey + ny, xb + ex + nx, yb + ey + ny, xb + ex - nx, yb + ey - ny, xa - ex - nx, ya - ey - ny, color);
	}

	public static void checkerboard(GuiGraphics g, int x, int y, int w, int h, int cell) {
		g.fill(x, y, x + w, y + h, 0xFFBDBDBD);
		for (int cy = 0; cy * cell < h; cy++) {
			for (int cx = 0; cx * cell < w; cx++) {
				if (((cx + cy) & 1) == 0) {
					int x0 = x + cx * cell;
					int y0 = y + cy * cell;
					g.fill(x0, y0, Math.min(x0 + cell, x + w), Math.min(y0 + cell, y + h), 0xFF7A7A7A);
				}
			}
		}
	}

	public static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	record QuadState(
		Matrix3x2fc pose,
		float x0,
		float y0,
		float x1,
		float y1,
		float x2,
		float y2,
		float x3,
		float y3,
		int color,
		@Nullable ScreenRectangle scissorArea,
		@Nullable ScreenRectangle bounds
	) implements GuiElementRenderState {
		@Override
		public void buildVertices(VertexConsumer consumer) {
			consumer.addVertexWith2DPose(this.pose, this.x0, this.y0).setColor(this.color);
			consumer.addVertexWith2DPose(this.pose, this.x1, this.y1).setColor(this.color);
			consumer.addVertexWith2DPose(this.pose, this.x2, this.y2).setColor(this.color);
			consumer.addVertexWith2DPose(this.pose, this.x3, this.y3).setColor(this.color);
		}

		@Override
		public RenderPipeline pipeline() {
			return RenderPipelines.GUI;
		}

		@Override
		public TextureSetup textureSetup() {
			return TextureSetup.noTexture();
		}
	}
}
