package dev.hitboxstudio.gui;

import dev.hitboxstudio.config.HitboxConfig;
import dev.hitboxstudio.config.PartStyle;
import dev.hitboxstudio.gui.widgets.ColorButton;
import dev.hitboxstudio.gui.widgets.Widgets;
import dev.hitboxstudio.render.ColorMath;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/** Crosshair target / in reach / hurt flash colors for every section except crystals. */
final class HighlightsScreen extends HitboxOptionsScreen {
	HighlightsScreen(Screen parent) {
		super(parent, Component.translatable("hitboxstudio.page.highlights"));
	}

	@Override
	protected HitboxOptionsScreen recreate() {
		return new HighlightsScreen(this.lastScreen);
	}

	@Override
	protected void addOptions() {
		this.row(
			Widgets.cycle(StyleEditor.opt("blend_mode"), StyleEditor.tip("blend_mode"), HitboxConfig.BlendMode.values(),
				() -> cfg().highlights.blendMode, v -> cfg().highlights.blendMode = v,
				m -> Component.translatable("hitboxstudio.blend." + m.name().toLowerCase(Locale.ROOT)), this::refresh),
			Widgets.toggle(StyleEditor.opt("apply_to_fill"), StyleEditor.tip("apply_to_fill"),
				() -> cfg().highlights.applyToFill, v -> cfg().highlights.applyToFill = v, this::refresh)
		);
		this.header(Component.translatable("hitboxstudio.header.highlight_colors"));
		HitboxConfig.Highlights defaults = new HitboxConfig.Highlights();
		this.highlightRow("target", () -> cfg().highlights.target, defaults.target);
		this.highlightRow("in_reach", () -> cfg().highlights.inReach, defaults.inReach);
		this.highlightRow("hurt", () -> cfg().highlights.hurt, defaults.hurt);
	}

	private void highlightRow(String key, Supplier<PartStyle> part, PartStyle defaults) {
		Component title = Component.translatable("hitboxstudio.highlight." + key);
		ColorButton color = new ColorButton(Widgets.HALF, StyleEditor.opt("color"), part, defaults.color,
			b -> this.openColorPicker(title, part.get()), this::refresh);
		color.setTooltip(Widgets.tooltip(StyleEditor.tip("highlight_color")));
		this.row(
			Widgets.toggle(title, Component.translatable("hitboxstudio.highlight." + key + ".tooltip"),
				() -> part.get().enabled, v -> part.get().enabled = v, this::refresh),
			this.gate(color, () -> part.get().enabled)
		);
	}

	@Override
	protected void addFooter() {
		Component name = Component.translatable("hitboxstudio.page.highlights");
		this.resetAndDoneFooter(Component.translatable("hitboxstudio.button.reset"),
			Component.translatable("hitboxstudio.confirm.reset_section.title"), Component.translatable("hitboxstudio.confirm.reset_section.message", name),
			() -> cfg().highlights = new HitboxConfig.Highlights());
	}

	@Override
	protected PreviewPanel.State previewState() {
		HitboxConfig.Highlights highlights = cfg().highlights;
		List<PartStyle> states = new ArrayList<>();
		List<Component> names = new ArrayList<>();
		states.add(null);
		names.add(Component.translatable("hitboxstudio.preview.state.normal"));
		if (highlights.target.enabled) {
			states.add(highlights.target);
			names.add(Component.translatable("hitboxstudio.highlight.target"));
		}
		if (highlights.inReach.enabled) {
			states.add(highlights.inReach);
			names.add(Component.translatable("hitboxstudio.highlight.in_reach"));
		}
		if (highlights.hurt.enabled) {
			states.add(highlights.hurt);
			names.add(Component.translatable("hitboxstudio.highlight.hurt"));
		}
		long now = Util.getMillis();
		int index = (int) ((now / 1500L) % states.size());
		int highlight = states.get(index) == null ? ColorMath.NO_HIGHLIGHT : ColorMath.highlightOf(states.get(index), now);
		return preview(cfg().defaultStyle, PreviewShape.ZOMBIE, highlight, names.get(index), true);
	}
}
