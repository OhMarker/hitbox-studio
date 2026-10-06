package dev.hitboxstudio.gui;

import dev.hitboxstudio.config.HitboxStyle;
import dev.hitboxstudio.render.ColorMath;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The look every entity section uses unless it has "Custom Style" on. */
final class DefaultStyleScreen extends HitboxOptionsScreen {
	DefaultStyleScreen(Screen parent) {
		super(parent, Component.translatable("hitboxstudio.page.default_style"));
	}

	@Override
	protected HitboxOptionsScreen recreate() {
		return new DefaultStyleScreen(this.lastScreen);
	}

	@Override
	protected void addOptions() {
		StyleEditor.add(this, () -> cfg().defaultStyle, HitboxStyle.vanilla(), () -> true);
	}

	@Override
	protected void addFooter() {
		Component name = Component.translatable("hitboxstudio.page.default_style");
		this.resetAndDoneFooter(Component.translatable("hitboxstudio.button.reset"),
			Component.translatable("hitboxstudio.confirm.reset_section.title"), Component.translatable("hitboxstudio.confirm.reset_section.message", name),
			() -> cfg().defaultStyle.set(HitboxStyle.vanilla()));
	}

	@Override
	protected PreviewPanel.State previewState() {
		return preview(cfg().defaultStyle, PreviewShape.PLAYER, ColorMath.NO_HIGHLIGHT, null, true);
	}
}
