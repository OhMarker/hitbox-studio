package dev.hitboxstudio.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hitboxstudio.config.ConfigManager;
import dev.hitboxstudio.gui.widgets.Widgets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** Entity types (any ID, modded too) that never get a hitbox, whatever section they're in. */
final class HiddenEntitiesScreen extends HitboxOptionsScreen {
	private @Nullable EditBox input;
	private @Nullable StringWidget message;
	private Component pendingMessage = Component.empty();

	HiddenEntitiesScreen(Screen parent) {
		super(parent, Component.translatable("hitboxstudio.page.hidden_entities"));
	}

	@Override
	protected HitboxOptionsScreen recreate() {
		return new HiddenEntitiesScreen(this.lastScreen);
	}

	@Override
	protected void addOptions() {
		this.input = new EditBox(this.font, 0, 0, Widgets.HALF, 20, StyleEditor.opt("hidden_add"));
		this.input.setMaxLength(128);
		this.input.setHint(Component.translatable("hitboxstudio.hidden.hint").withStyle(ChatFormatting.DARK_GRAY));
		this.input.setTooltip(Widgets.tooltip(StyleEditor.tip("hidden_add")));
		this.input.setResponder(text -> this.setMessage(Component.empty()));
		this.row(this.input, Widgets.button(Component.translatable("hitboxstudio.button.add"), null, Widgets.HALF, b -> this.add()));

		this.message = new StringWidget(0, 0, Widgets.FULL, 20, this.pendingMessage, this.font);
		this.row(this.message, null);

		List<String> hidden = new ArrayList<>(cfg().general.hiddenEntities);
		this.header(Component.translatable("hitboxstudio.header.hidden_list", hidden.size()));
		if (hidden.isEmpty()) {
			this.row(new StringWidget(0, 0, Widgets.FULL, 20,
				Component.translatable("hitboxstudio.hidden.none").withStyle(ChatFormatting.GRAY), this.font), null);
		}
		for (String id : hidden) {
			StringWidget label = new StringWidget(0, 0, Widgets.HALF, 20, Component.literal(id), this.font);
			label.setMaxWidth(Widgets.HALF);
			this.row(label, Widgets.button(Component.translatable("hitboxstudio.button.remove"), StyleEditor.tip("hidden_entry"), Widgets.HALF, b -> {
				cfg().general.hiddenEntities.remove(id);
				ConfigManager.markChanged();
				this.rebuild();
			}));
		}
	}

	private void setMessage(Component text) {
		this.pendingMessage = text;
		if (this.message != null) {
			this.message.setMessage(text);
		}
	}

	private void add() {
		if (this.input == null) {
			return;
		}
		String text = this.input.getValue().trim().toLowerCase(Locale.ROOT);
		if (text.isEmpty()) {
			return;
		}
		Identifier id = Identifier.tryParse(text);
		if (id == null) {
			this.setMessage(Component.translatable("hitboxstudio.hidden.error.invalid").withStyle(ChatFormatting.RED));
			return;
		}
		if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
			this.setMessage(Component.translatable("hitboxstudio.hidden.error.unknown", id.toString()).withStyle(ChatFormatting.RED));
			return;
		}
		if (cfg().general.hiddenEntities.contains(id.toString())) {
			this.setMessage(Component.translatable("hitboxstudio.hidden.error.duplicate").withStyle(ChatFormatting.RED));
			return;
		}
		cfg().general.hiddenEntities.add(id.toString());
		ConfigManager.markChanged();
		this.rebuild();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.input != null && this.input.isFocused()
			&& (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER)) {
			this.add();
			return true;
		}
		return super.keyPressed(event);
	}
}
