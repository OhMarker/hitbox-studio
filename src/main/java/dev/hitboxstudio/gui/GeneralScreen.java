package dev.hitboxstudio.gui;

import dev.hitboxstudio.config.ConfigManager;
import dev.hitboxstudio.config.HitboxConfig;
import dev.hitboxstudio.gui.widgets.Widgets;
import java.util.Locale;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** When hitboxes show, max distance, and the global width/opacity multipliers. */
final class GeneralScreen extends HitboxOptionsScreen {
	GeneralScreen(Screen parent) {
		super(parent, Component.translatable("hitboxstudio.page.general"));
	}

	@Override
	protected HitboxOptionsScreen recreate() {
		return new GeneralScreen(this.lastScreen);
	}

	@Override
	protected void addOptions() {
		this.row(
			Widgets.toggle(StyleEditor.opt("hide_in_f1"), StyleEditor.tip("hide_in_f1"),
				() -> cfg().general.hideInF1, v -> cfg().general.hideInF1 = v, this::refresh),
			StyleEditor.slider(this, "max_distance", 4.0F, 132.0F, 4.0F, 132.0F,
				() -> cfg().general.maxDistance <= 0.0F ? 132.0F : Math.max(4.0F, Math.min(132.0F, cfg().general.maxDistance)),
				v -> cfg().general.maxDistance = v >= 132.0F ? 0.0F : v,
				v -> v >= 132.0F
					? Component.translatable("hitboxstudio.value.unlimited").getString()
					: Component.translatable("hitboxstudio.value.blocks", (int) v).getString())
		);

		this.header(Component.translatable("hitboxstudio.header.global_look"));
		this.row(
			StyleEditor.slider(this, "line_width_multiplier", 0.1F, 5.0F, 0.05F, 1.0F,
				() -> cfg().general.lineWidthMultiplier, v -> cfg().general.lineWidthMultiplier = v,
				v -> String.format(Locale.ROOT, "%.2fx", v)),
			StyleEditor.slider(this, "opacity_multiplier", 0.0F, 1.0F, 0.01F, 1.0F,
				() -> cfg().general.opacityMultiplier, v -> cfg().general.opacityMultiplier = v, StyleEditor::percent)
		);
	}

	@Override
	protected void addFooter() {
		this.resetAndDoneFooter(Component.translatable("hitboxstudio.button.reset_all"),
			Component.translatable("hitboxstudio.confirm.reset_all.title"), Component.translatable("hitboxstudio.confirm.reset_all.message"),
			() -> ConfigManager.replace(new HitboxConfig()));
	}
}
