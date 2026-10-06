package dev.hitboxstudio.gui;

import dev.hitboxstudio.config.ConfigManager;
import dev.hitboxstudio.config.HitboxConfig;
import dev.hitboxstudio.config.HitboxStyle;
import dev.hitboxstudio.config.PartStyle;
import dev.hitboxstudio.gui.widgets.Widgets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Base for every Hitbox Studio page: a vanilla options screen (title, scrolling two-column list, Done footer),
 * plus widgets that grey themselves out when the option they depend on is off, and an optional live preview.
 */
public abstract class HitboxOptionsScreen extends OptionsSubScreen {
	private record Gated(AbstractWidget widget, BooleanSupplier condition) {
	}

	private final List<Gated> gated = new ArrayList<>();
	private final PreviewPanel preview = new PreviewPanel();
	private double pendingScroll;

	protected HitboxOptionsScreen(Screen parent, Component title) {
		super(parent, Minecraft.getInstance().options, title);
	}

	/** A fresh copy of this page with the same parent. Used to rebuild after structural changes. */
	protected abstract HitboxOptionsScreen recreate();

	protected static HitboxConfig cfg() {
		return ConfigManager.get();
	}

	// ------------------------------------------------------------ list helpers

	protected void header(Component text) {
		this.list.addHeader(text);
	}

	protected void row(AbstractWidget left, @Nullable AbstractWidget right) {
		this.list.addSmall(left, right);
	}

	/** Greys the widget out whenever condition is false (checked again after every change on the page). */
	protected <T extends AbstractWidget> T gate(T widget, BooleanSupplier condition) {
		this.gated.add(new Gated(widget, condition));
		widget.active = condition.getAsBoolean();
		return widget;
	}

	/** Re-evaluates which widgets are greyed out. */
	public void refresh() {
		for (Gated entry : this.gated) {
			entry.widget.active = entry.condition.getAsBoolean();
		}
	}

	/**
	 * Rebuilds the page (after resets, custom style on/off, list edits), keeping the scroll position.
	 * Vanilla's header/footer layout can't be filled twice, so this swaps in a fresh copy of the page.
	 */
	public void rebuild() {
		HitboxOptionsScreen next = this.recreate();
		next.pendingScroll = this.list != null ? this.list.scrollAmount() : 0.0;
		this.minecraft.setScreen(next);
	}

	protected void openColorPicker(Component title, PartStyle part) {
		this.minecraft.setScreen(new ColorPickerScreen(this, title, part));
	}

	/** Vanilla confirm dialog; on yes runs the action and comes back to a rebuilt copy of this page. */
	protected void confirm(Component title, Component message, Runnable action) {
		double scroll = this.list != null ? this.list.scrollAmount() : 0.0;
		this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
			if (confirmed) {
				action.run();
				ConfigManager.markChanged();
				HitboxOptionsScreen next = this.recreate();
				next.pendingScroll = scroll;
				this.minecraft.setScreen(next);
			} else {
				this.minecraft.setScreen(this);
			}
		}, title, message));
	}

	// ------------------------------------------------------------ lifecycle

	@Override
	protected void init() {
		this.gated.clear();
		super.init();
		if (this.pendingScroll > 0.0 && this.list != null) {
			this.list.setScrollAmount(this.pendingScroll);
		}
		this.pendingScroll = 0.0;
	}

	/** Footer with a Reset button (asks first) next to Done. */
	protected void resetAndDoneFooter(Component resetLabel, Component confirmTitle, Component confirmMessage, Runnable reset) {
		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		footer.addChild(Widgets.button(resetLabel, null, Widgets.HALF, b -> this.confirm(confirmTitle, confirmMessage, reset)));
		footer.addChild(Widgets.button(CommonComponents.GUI_DONE, null, Widgets.HALF, b -> this.onClose()));
	}

	// ------------------------------------------------------------ preview

	/** What the preview beside the list shows; null means no preview on this page. */
	protected PreviewPanel.@Nullable State previewState() {
		return null;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		PreviewPanel.State state = this.previewState();
		if (state == null) {
			return;
		}
		// Only in the empty space right of the list (and its scrollbar); hidden on narrow windows.
		int x0 = this.width / 2 + 176;
		int x1 = this.width - 10;
		int top = this.layout.getHeaderHeight() + 6;
		int bottom = Math.min(this.height - this.layout.getFooterHeight() - 6, top + 170);
		if (x1 - x0 >= 110 && bottom - top >= 110) {
			this.preview.render(g, this.font, x0, top, x1 - x0, bottom - top, state);
		}
	}

	protected static PreviewPanel.State preview(HitboxStyle style, PreviewShape shape, int highlight, @Nullable Component caption, boolean visible) {
		return new PreviewPanel.State(style, shape, cfg(), highlight, caption, visible);
	}
}
