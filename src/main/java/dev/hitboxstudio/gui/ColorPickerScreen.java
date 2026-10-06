package dev.hitboxstudio.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hitboxstudio.config.ConfigManager;
import dev.hitboxstudio.config.HexColorAdapter;
import dev.hitboxstudio.config.HitboxConfig;
import dev.hitboxstudio.config.PartStyle;
import dev.hitboxstudio.gui.widgets.ValueSlider;
import dev.hitboxstudio.gui.widgets.Widgets;
import dev.hitboxstudio.render.ColorMath;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/**
 * Color wheel page. The wheel (hue + saturation) and brightness bar are the only custom parts;
 * hex box, opacity, rainbow and buttons are regular Minecraft widgets.
 */
public final class ColorPickerScreen extends Screen {
	private static final int[] PRESETS = {
		0xFFFFFFFF, 0xFFAAAAAA, 0xFF555555, 0xFF000000, 0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFF55FF55,
		0xFF00AA00, 0xFF55FFFF, 0xFF00AAAA, 0xFF5555FF, 0xFF0000AA, 0xFFAA00AA, 0xFFFF55FF, 0xFF8B4513
	};
	private static final int COLUMN_W = 150;
	private static final int BAR_W = 12;
	private static final int SWATCH = 12;
	private static final int SWATCH_GAP = 3;

	private enum Drag {
		NONE, WHEEL, BRIGHTNESS
	}

	private final Screen parent;
	private final PartStyle target;
	private final int originalColor;
	private final float[] hsv = new float[3];
	private float alpha;
	private boolean rainbow;
	private float rainbowSpeed;

	private EditBox hexBox;
	private ValueSlider opacitySlider;
	private ValueSlider speedSlider;
	private boolean updatingHex;
	private Drag drag = Drag.NONE;

	private int wheelSize;
	private int wheelX;
	private int wheelY;
	private int barX;
	private int columnX;
	private int swatchRowsY;

	public ColorPickerScreen(Screen parent, Component label, PartStyle target) {
		super(Component.translatable("hitboxstudio.picker.title", label));
		this.parent = parent;
		this.target = target;
		this.originalColor = target.color;
		this.setColor(target.color, true);
		this.rainbow = target.rainbow;
		this.rainbowSpeed = target.rainbowSpeed;
	}

	private int currentColor() {
		return ColorMath.withAlpha(ColorMath.hsvToRgb(this.hsv[0], this.hsv[1], this.hsv[2]), Math.round(this.alpha * 255.0F));
	}

	private void setColor(int argb, boolean withAlpha) {
		float hue = this.hsv[0];
		ColorMath.rgbToHsv(argb, this.hsv);
		// Greys have no hue; keep the old one so the marker doesn't jump to red.
		if (this.hsv[1] < 1.0E-4F || this.hsv[2] < 1.0E-4F) {
			this.hsv[0] = hue;
		}
		if (withAlpha) {
			this.alpha = ColorMath.alpha(argb) / 255.0F;
		}
	}

	/** Called after the color changed from anything except the hex box itself. */
	private void colorChanged() {
		if (this.hexBox != null && !this.hexBox.isFocused()) {
			int color = this.currentColor();
			this.updatingHex = true;
			this.hexBox.setValue(ColorMath.alpha(color) == 255 ? HexColorAdapter.toHexRgb(color) : HexColorAdapter.toHex(color));
			this.updatingHex = false;
		}
		if (this.opacitySlider != null) {
			this.opacitySlider.syncFromGetter();
		}
	}

	@Override
	protected void init() {
		// Same header/footer sizes as vanilla option screens (33 px each).
		Button done = Widgets.button(CommonComponents.GUI_DONE, null, Widgets.HALF, b -> this.apply());
		done.setPosition(this.width / 2 - 154, this.height - 27);
		this.addRenderableWidget(done);
		Button cancel = Widgets.button(CommonComponents.GUI_CANCEL, null, Widgets.HALF, b -> this.minecraft.setScreen(this.parent));
		cancel.setPosition(this.width / 2 + 4, this.height - 27);
		this.addRenderableWidget(cancel);

		int top = 33 + 4;
		int bottom = this.height - 33 - 4;
		int swatchRows = 2 * (SWATCH + 6);
		this.wheelSize = Math.max(64, Math.min(150, bottom - top - swatchRows - 4));
		int totalW = this.wheelSize + 6 + BAR_W + 12 + COLUMN_W;
		this.wheelX = (this.width - totalW) / 2;
		this.wheelY = top;
		this.barX = this.wheelX + this.wheelSize + 6;
		this.columnX = this.barX + BAR_W + 12;
		this.swatchRowsY = top + Math.max(this.wheelSize, 5 * 24 - 4) + 6;

		// Right column: (swatches drawn in render) hex, opacity, rainbow, speed.
		int y = top + 24;
		this.hexBox = new EditBox(this.font, this.columnX, y, COLUMN_W, 20, Component.translatable("hitboxstudio.picker.hex"));
		this.hexBox.setMaxLength(9);
		this.hexBox.setFilter(text -> text.chars().allMatch(c -> c == '#' || Character.digit(c, 16) >= 0));
		this.hexBox.setTooltip(Widgets.tooltip(Component.translatable("hitboxstudio.picker.hex.tooltip")));
		this.hexBox.setResponder(this::onHexTyped);
		this.addRenderableWidget(this.hexBox);

		y += 24;
		this.opacitySlider = new ValueSlider(COLUMN_W, Component.translatable("hitboxstudio.option.opacity"), 0.0F, 1.0F, 0.01F, 1.0F,
			() -> this.alpha, v -> {
				this.alpha = v;
				this.colorChanged();
			}, StyleEditor::percent, () -> {
			});
		this.opacitySlider.setPosition(this.columnX, y);
		this.opacitySlider.setTooltip(Widgets.tooltip(Component.translatable("hitboxstudio.picker.opacity.tooltip")));
		this.addRenderableWidget(this.opacitySlider);

		y += 24;
		CycleButton<Boolean> rainbowButton = CycleButton.onOffBuilder(this.rainbow)
			.withTooltip(v -> Widgets.tooltip(Component.translatable("hitboxstudio.picker.rainbow.tooltip")))
			.create(this.columnX, y, COLUMN_W, 20, Component.translatable("hitboxstudio.picker.rainbow"), (button, value) -> {
				this.rainbow = value;
				this.speedSlider.active = value;
			});
		this.addRenderableWidget(rainbowButton);

		y += 24;
		this.speedSlider = new ValueSlider(COLUMN_W, Component.translatable("hitboxstudio.picker.rainbow_speed"), 0.1F, 5.0F, 0.1F, 1.0F,
			() -> this.rainbowSpeed, v -> this.rainbowSpeed = v, v -> String.format(Locale.ROOT, "%.1fx", v), () -> {
			});
		this.speedSlider.setPosition(this.columnX, y);
		this.speedSlider.active = this.rainbow;
		this.addRenderableWidget(this.speedSlider);

		this.colorChanged();
	}

	private void onHexTyped(String text) {
		if (this.updatingHex) {
			return;
		}
		Integer parsed = HexColorAdapter.parse(text);
		if (parsed != null) {
			String digits = text.trim().startsWith("#") ? text.trim().substring(1) : text.trim();
			this.setColor(parsed, digits.length() == 8);
			if (this.opacitySlider != null) {
				this.opacitySlider.syncFromGetter();
			}
		}
	}

	private void apply() {
		this.target.color = this.currentColor();
		this.target.rainbow = this.rainbow;
		this.target.rainbowSpeed = this.rainbowSpeed;
		HitboxConfig config = ConfigManager.get();
		String hex = HexColorAdapter.toHex(this.target.color);
		config.recentColors.remove(hex);
		config.recentColors.add(0, hex);
		while (config.recentColors.size() > 8) {
			config.recentColors.remove(config.recentColors.size() - 1);
		}
		ConfigManager.markChanged();
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void onClose() {
		this.apply();
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		g.drawCenteredString(this.font, this.title, this.width / 2, (33 - 9) / 2, 0xFFFFFFFF);
		int size = this.wheelSize;
		int radius = size / 2;

		// Hue/saturation wheel with a small marker.
		g.blit(RenderPipelines.GUI_TEXTURED, ColorWheelTexture.get(this.hsv[2]), this.wheelX, this.wheelY, 0.0F, 0.0F, size, size,
			ColorWheelTexture.SIZE, ColorWheelTexture.SIZE, ColorWheelTexture.SIZE, ColorWheelTexture.SIZE);
		double angle = this.hsv[0] * Math.PI * 2.0;
		int mx = (int) Math.round(this.wheelX + radius + Math.cos(angle) * this.hsv[1] * radius);
		int my = (int) Math.round(this.wheelY + radius + Math.sin(angle) * this.hsv[1] * radius);
		g.renderOutline(mx - 3, my - 3, 7, 7, 0xFF000000);
		g.renderOutline(mx - 2, my - 2, 5, 5, 0xFFFFFFFF);

		// Brightness bar.
		int bright = ColorMath.hsvToRgb(this.hsv[0], this.hsv[1], 1.0F);
		g.fill(this.barX - 1, this.wheelY - 1, this.barX + BAR_W + 1, this.wheelY + size + 1, 0xFF000000);
		g.fillGradient(this.barX, this.wheelY, this.barX + BAR_W, this.wheelY + size, bright, 0xFF000000);
		int markerY = this.wheelY + Math.round((1.0F - this.hsv[2]) * size);
		g.fill(this.barX - 2, markerY - 2, this.barX + BAR_W + 2, markerY + 2, 0xFF000000);
		g.fill(this.barX - 1, markerY - 1, this.barX + BAR_W + 1, markerY + 1, 0xFFFFFFFF);

		// New / old swatches on top of the right column.
		int half = COLUMN_W / 2 - 2;
		int shown = this.rainbow
			? ColorMath.withAlpha(ColorMath.hsvToRgb((float) ((Util.getMillis() * (double) this.rainbowSpeed / 4000.0) % 1.0), 1.0F, 1.0F),
				Math.round(this.alpha * 255.0F))
			: this.currentColor();
		this.swatch(g, this.columnX, this.wheelY, half, 20, shown);
		this.swatch(g, this.columnX + half + 4, this.wheelY, half, 20, this.originalColor);
		this.swatchLabel(g, Component.translatable("hitboxstudio.picker.new"), this.columnX + half / 2, shown);
		this.swatchLabel(g, Component.translatable("hitboxstudio.picker.old"), this.columnX + half + 4 + half / 2, this.originalColor);

		// Recent colors and presets.
		int labelW = Math.max(this.font.width(Component.translatable("hitboxstudio.picker.recent")),
			this.font.width(Component.translatable("hitboxstudio.picker.presets"))) + 6;
		List<String> recents = ConfigManager.get().recentColors;
		int rowY = this.swatchRowsY;
		g.drawString(this.font, Component.translatable("hitboxstudio.picker.recent"), this.wheelX, rowY + 2, 0xFFFFFFFF);
		if (recents.isEmpty()) {
			g.drawString(this.font, Component.translatable("hitboxstudio.picker.recent_none"), this.wheelX + labelW, rowY + 2, 0xFFA0A0A0);
		}
		for (int i = 0; i < recents.size(); i++) {
			Integer recent = HexColorAdapter.parse(recents.get(i));
			if (recent != null) {
				this.swatch(g, this.wheelX + labelW + i * (SWATCH + SWATCH_GAP), rowY, SWATCH, SWATCH, recent);
			}
		}
		rowY += SWATCH + 6;
		g.drawString(this.font, Component.translatable("hitboxstudio.picker.presets"), this.wheelX, rowY + 2, 0xFFFFFFFF);
		for (int i = 0; i < PRESETS.length; i++) {
			this.swatch(g, this.wheelX + labelW + i * (SWATCH + SWATCH_GAP), rowY, SWATCH, SWATCH, PRESETS[i]);
		}

		if (GuiDraw.inside(mouseX, mouseY, this.columnX + half + 4, this.wheelY, half, 20)) {
			g.setTooltipForNextFrame(this.font, Component.translatable("hitboxstudio.picker.old.tooltip"), mouseX, mouseY);
		}
	}

	/** Dark text on light colors, white text on dark ones, so the label is always readable. */
	private void swatchLabel(GuiGraphics g, Component text, int centerX, int argb) {
		float a = ColorMath.alpha(argb) / 255.0F;
		// What the swatch looks like over the grey checkerboard.
		float r = ((argb >> 16) & 0xFF) * a + 158 * (1.0F - a);
		float gr = ((argb >> 8) & 0xFF) * a + 158 * (1.0F - a);
		float b = (argb & 0xFF) * a + 158 * (1.0F - a);
		float luminance = (0.299F * r + 0.587F * gr + 0.114F * b) / 255.0F;
		int x = centerX - this.font.width(text) / 2;
		if (luminance > 0.55F) {
			g.drawString(this.font, text, x, this.wheelY + 6, 0xFF202020, false);
		} else {
			g.drawString(this.font, text, x, this.wheelY + 6, 0xFFFFFFFF, true);
		}
	}

	/** Swatch framed like an inventory slot. */
	private void swatch(GuiGraphics g, int x, int y, int w, int h, int argb) {
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF373737);
		g.fill(x, y, x + w + 1, y + h + 1, 0xFFFFFFFF);
		g.fill(x, y, x + w, y + h, 0xFF8B8B8B);
		GuiDraw.checkerboard(g, x, y, w, h, 3);
		g.fill(x, y, x + w, y + h, argb);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		if (event.button() != 0) {
			return false;
		}
		double mx = event.x();
		double my = event.y();
		int radius = this.wheelSize / 2;
		double dx = mx - (this.wheelX + radius);
		double dy = my - (this.wheelY + radius);
		if (dx * dx + dy * dy <= (radius + 4.0) * (radius + 4.0)) {
			this.drag = Drag.WHEEL;
			this.updateDrag(mx, my);
			return true;
		}
		if (GuiDraw.inside(mx, my, this.barX - 3, this.wheelY - 3, BAR_W + 6, this.wheelSize + 6)) {
			this.drag = Drag.BRIGHTNESS;
			this.updateDrag(mx, my);
			return true;
		}
		int half = COLUMN_W / 2 - 2;
		if (GuiDraw.inside(mx, my, this.columnX + half + 4, this.wheelY, half, 20)) {
			this.setColor(this.originalColor, true);
			this.colorChanged();
			return true;
		}
		int labelW = Math.max(this.font.width(Component.translatable("hitboxstudio.picker.recent")),
			this.font.width(Component.translatable("hitboxstudio.picker.presets"))) + 6;
		List<String> recents = ConfigManager.get().recentColors;
		for (int i = 0; i < recents.size(); i++) {
			if (GuiDraw.inside(mx, my, this.wheelX + labelW + i * (SWATCH + SWATCH_GAP), this.swatchRowsY, SWATCH, SWATCH)) {
				Integer recent = HexColorAdapter.parse(recents.get(i));
				if (recent != null) {
					this.setColor(recent, true);
					this.colorChanged();
				}
				return true;
			}
		}
		int presetY = this.swatchRowsY + SWATCH + 6;
		for (int i = 0; i < PRESETS.length; i++) {
			if (GuiDraw.inside(mx, my, this.wheelX + labelW + i * (SWATCH + SWATCH_GAP), presetY, SWATCH, SWATCH)) {
				this.setColor(PRESETS[i], false);
				this.colorChanged();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.drag != Drag.NONE) {
			this.updateDrag(event.x(), event.y());
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		this.drag = Drag.NONE;
		return super.mouseReleased(event);
	}

	private void updateDrag(double mx, double my) {
		int size = this.wheelSize;
		if (this.drag == Drag.WHEEL) {
			int radius = size / 2;
			double dx = mx - (this.wheelX + radius);
			double dy = my - (this.wheelY + radius);
			float hue = (float) (Math.atan2(dy, dx) / (Math.PI * 2.0));
			if (hue < 0.0F) {
				hue += 1.0F;
			}
			this.hsv[0] = hue;
			this.hsv[1] = (float) Math.min(1.0, Math.sqrt(dx * dx + dy * dy) / radius);
			if (this.hsv[2] < 0.05F) {
				// Picking a hue on a black wheel does nothing visible, so lift brightness.
				this.hsv[2] = 1.0F;
			}
		} else if (this.drag == Drag.BRIGHTNESS) {
			this.hsv[2] = (float) Math.max(0.0, Math.min(1.0, 1.0 - (my - this.wheelY) / size));
		}
		this.colorChanged();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.hexBox != null && this.hexBox.isFocused()
			&& (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER)) {
			this.setFocused(null);
			this.colorChanged();
			return true;
		}
		return super.keyPressed(event);
	}
}
