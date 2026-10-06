package dev.hitboxstudio.gui.widgets;

import dev.hitboxstudio.config.ConfigManager;
import dev.hitboxstudio.config.PartStyle;
import dev.hitboxstudio.gui.GuiDraw;
import dev.hitboxstudio.render.ColorMath;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/** A normal Minecraft button with a little color swatch on the right. Click opens the color wheel, right-click resets. */
public final class ColorButton extends Button {
	private static final int SWATCH_W = 18;
	private static final int SWATCH_H = 10;
	private final Supplier<PartStyle> part;
	private final int defaultColor;
	private final Runnable onReset;

	public ColorButton(int width, Component label, Supplier<PartStyle> part, int defaultColor, OnPress onPress, Runnable onReset) {
		super(0, 0, width, 20, label, onPress, DEFAULT_NARRATION);
		this.part = part;
		this.defaultColor = defaultColor;
		this.onReset = onReset;
	}

	@Override
	protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.renderDefaultSprite(g);
		int sx = this.getX() + this.getWidth() - SWATCH_W - 5;
		int sy = this.getY() + (this.getHeight() - SWATCH_H) / 2;
		int color = ColorMath.animated(this.part.get(), Util.getMillis());
		g.fill(sx - 1, sy - 1, sx + SWATCH_W + 1, sy + SWATCH_H + 1, 0xFF000000);
		GuiDraw.checkerboard(g, sx, sy, SWATCH_W, SWATCH_H, 3);
		g.fill(sx, sy, sx + SWATCH_W, sy + SWATCH_H, color);
		if (!this.active) {
			g.fill(sx, sy, sx + SWATCH_W, sy + SWATCH_H, 0xA0303030);
		}
		int textCenter = this.getX() + (this.getWidth() - SWATCH_W - 6) / 2;
		g.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), textCenter, this.getY() + (this.getHeight() - 8) / 2,
			this.active ? 0xFFFFFFFF : 0xFFA0A0A0);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 1 && this.active && this.visible && this.isMouseOver(event.x(), event.y())) {
			PartStyle style = this.part.get();
			style.color = this.defaultColor;
			style.rainbow = false;
			ConfigManager.markChanged();
			this.playDownSound(Minecraft.getInstance().getSoundManager());
			this.onReset.run();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}
}
