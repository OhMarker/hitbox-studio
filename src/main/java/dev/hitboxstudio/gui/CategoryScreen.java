package dev.hitboxstudio.gui;

import dev.hitboxstudio.config.Category;
import dev.hitboxstudio.config.CategoryConfig;
import dev.hitboxstudio.config.CrystalConfig;
import dev.hitboxstudio.config.HitboxStyle;
import dev.hitboxstudio.config.PartStyle;
import dev.hitboxstudio.gui.widgets.ColorButton;
import dev.hitboxstudio.gui.widgets.Widgets;
import dev.hitboxstudio.render.ColorMath;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/** One entity section: on/off, custom style or Default Style, plus crystal highlights / projectile types. */
final class CategoryScreen extends HitboxOptionsScreen {
	private final Category category;

	CategoryScreen(Screen parent, Category category) {
		super(parent, Component.translatable("hitboxstudio.tab." + category.key));
		this.category = category;
	}

	@Override
	protected HitboxOptionsScreen recreate() {
		return new CategoryScreen(this.lastScreen, this.category);
	}

	private CategoryConfig section() {
		return cfg().get(this.category);
	}

	@Override
	protected void addOptions() {
		BooleanSupplier on = () -> this.section().enabled;
		this.row(
			Widgets.toggle(StyleEditor.opt("show_category"), Component.translatable("hitboxstudio.category." + this.category.key + ".info"),
				on, v -> this.section().enabled = v, this::refresh),
			// Switching style source changes which rows exist, so the page is rebuilt.
			this.gate(Widgets.toggle(StyleEditor.opt("custom_style"), StyleEditor.tip("custom_style"),
				() -> this.section().customStyle, v -> this.section().customStyle = v, this::rebuild), on)
		);

		if (this.category == Category.CRYSTALS) {
			this.header(Component.translatable("hitboxstudio.header.crystal_highlights"));
			CrystalConfig defaults = new CrystalConfig();
			this.highlightRow("crystal_target", () -> cfg().crystals.targetColor, defaults.targetColor, on);
			this.highlightRow("crystal_in_reach", () -> cfg().crystals.inReachColor, defaults.inReachColor, on);
		}

		if (this.category == Category.PROJECTILES) {
			this.header(Component.translatable("hitboxstudio.header.projectile_types"));
			List<AbstractWidget> toggles = new ArrayList<>();
			toggles.add(this.projectileToggle("arrows", () -> cfg().projectiles.arrows, v -> cfg().projectiles.arrows = v, on));
			toggles.add(this.projectileToggle("tridents", () -> cfg().projectiles.tridents, v -> cfg().projectiles.tridents = v, on));
			toggles.add(this.projectileToggle("ender_pearls", () -> cfg().projectiles.enderPearls, v -> cfg().projectiles.enderPearls = v, on));
			toggles.add(this.projectileToggle("snowballs_eggs", () -> cfg().projectiles.snowballsAndEggs, v -> cfg().projectiles.snowballsAndEggs = v, on));
			toggles.add(this.projectileToggle("potions", () -> cfg().projectiles.potions, v -> cfg().projectiles.potions = v, on));
			toggles.add(this.projectileToggle("fireballs", () -> cfg().projectiles.fireballs, v -> cfg().projectiles.fireballs = v, on));
			toggles.add(this.projectileToggle("wind_charges", () -> cfg().projectiles.windCharges, v -> cfg().projectiles.windCharges = v, on));
			toggles.add(this.projectileToggle("fireworks", () -> cfg().projectiles.fireworks, v -> cfg().projectiles.fireworks = v, on));
			toggles.add(this.projectileToggle("fishing_hooks", () -> cfg().projectiles.fishingHooks, v -> cfg().projectiles.fishingHooks = v, on));
			toggles.add(this.projectileToggle("other_projectiles", () -> cfg().projectiles.otherProjectiles, v -> cfg().projectiles.otherProjectiles = v, on));
			for (int i = 0; i < toggles.size(); i += 2) {
				this.row(toggles.get(i), i + 1 < toggles.size() ? toggles.get(i + 1) : null);
			}
		}

		if (this.section().customStyle) {
			Component name = this.title;
			this.row(this.gate(Widgets.button(Component.translatable("hitboxstudio.button.copy_default"), StyleEditor.tip("copy_default"), Widgets.FULL,
				b -> this.confirm(Component.translatable("hitboxstudio.confirm.copy_default.title"),
					Component.translatable("hitboxstudio.confirm.copy_default.message", name),
					() -> this.section().style.set(cfg().defaultStyle))), on), null);
			HitboxStyle defaults = this.category == Category.CRYSTALS ? HitboxStyle.crystalDefault() : HitboxStyle.vanilla();
			StyleEditor.add(this, () -> this.section().style, defaults, on);
		} else {
			this.header(Component.translatable("hitboxstudio.header.using_default"));
			this.row(this.gate(Widgets.button(
				Component.translatable("hitboxstudio.button.open", Component.translatable("hitboxstudio.page.default_style")),
				Component.translatable("hitboxstudio.page.default_style.tooltip"), Widgets.FULL,
				b -> this.minecraft.setScreen(new DefaultStyleScreen(this))), on), null);
		}
	}

	private void highlightRow(String key, Supplier<PartStyle> part, PartStyle defaults, BooleanSupplier on) {
		Component title = StyleEditor.opt(key);
		ColorButton color = new ColorButton(Widgets.HALF, StyleEditor.opt("color"), part, defaults.color,
			b -> this.openColorPicker(title, part.get()), this::refresh);
		color.setTooltip(Widgets.tooltip(StyleEditor.tip("highlight_color")));
		this.row(
			this.gate(Widgets.toggle(title, StyleEditor.tip(key), () -> part.get().enabled, v -> part.get().enabled = v, this::refresh), on),
			this.gate(color, () -> on.getAsBoolean() && part.get().enabled)
		);
	}

	private AbstractWidget projectileToggle(String key, BooleanSupplier getter, Consumer<Boolean> setter, BooleanSupplier on) {
		return this.gate(Widgets.toggle(Component.translatable("hitboxstudio.projectile." + key),
			Component.translatable("hitboxstudio.projectile." + key + ".tooltip"), getter, setter, this::refresh), on);
	}

	@Override
	protected void addFooter() {
		this.resetAndDoneFooter(Component.translatable("hitboxstudio.button.reset"),
			Component.translatable("hitboxstudio.confirm.reset_section.title"),
			Component.translatable("hitboxstudio.confirm.reset_section.message", this.title),
			() -> cfg().reset(this.category));
	}

	@Override
	protected PreviewPanel.State previewState() {
		HitboxStyle style = cfg().effectiveStyle(this.category);
		int highlight = ColorMath.NO_HIGHLIGHT;
		Component caption = null;
		if (this.category == Category.CRYSTALS) {
			List<PartStyle> states = new ArrayList<>();
			List<Component> names = new ArrayList<>();
			states.add(null);
			names.add(null);
			if (cfg().crystals.targetColor.enabled) {
				states.add(cfg().crystals.targetColor);
				names.add(Component.translatable("hitboxstudio.preview.state.target"));
			}
			if (cfg().crystals.inReachColor.enabled) {
				states.add(cfg().crystals.inReachColor);
				names.add(Component.translatable("hitboxstudio.preview.state.in_reach"));
			}
			long now = Util.getMillis();
			int index = (int) ((now / 1500L) % states.size());
			if (states.get(index) != null) {
				highlight = ColorMath.highlightOf(states.get(index), now);
				caption = names.get(index);
			}
		}
		return preview(style, PreviewShape.forCategory(this.category), highlight, caption, this.section().enabled);
	}
}
