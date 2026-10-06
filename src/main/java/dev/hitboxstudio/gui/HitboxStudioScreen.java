package dev.hitboxstudio.gui;

import dev.hitboxstudio.config.Category;
import dev.hitboxstudio.config.ConfigManager;
import dev.hitboxstudio.config.HitboxConfig;
import dev.hitboxstudio.gui.widgets.Widgets;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The screen Mod Menu opens. Laid out like vanilla's Options menu: quick switches on top,
 * then buttons leading to each settings page. Done saves; Cancel undoes everything since it was opened.
 */
public final class HitboxStudioScreen extends HitboxOptionsScreen {
	private final HitboxConfig snapshot;
	private final Map<Category, Button> categoryButtons = new EnumMap<>(Category.class);
	private @Nullable CycleButton<Boolean> enabledButton;
	private @Nullable CycleButton<HitboxConfig.Mode> modeButton;
	private boolean shownBefore;

	public HitboxStudioScreen(Screen parent) {
		super(parent, Component.translatable("hitboxstudio.title"));
		this.snapshot = ConfigManager.copy(ConfigManager.get());
	}

	@Override
	protected HitboxOptionsScreen recreate() {
		return this;
	}

	@Override
	protected void addOptions() {
		this.categoryButtons.clear();
		this.enabledButton = Widgets.toggle(Component.translatable("hitboxstudio.option.enabled"), StyleEditor.tip("enabled"),
			() -> cfg().general.enabled, v -> cfg().general.enabled = v, this::refresh);
		this.modeButton = this.gate(Widgets.cycle(Component.translatable("hitboxstudio.option.mode"), StyleEditor.tip("mode"),
			HitboxConfig.Mode.values(), () -> cfg().general.mode, v -> cfg().general.mode = v,
			m -> Component.translatable("hitboxstudio.mode." + m.name().toLowerCase(Locale.ROOT)), this::refresh), () -> cfg().general.enabled);
		this.row(this.enabledButton, this.modeButton);

		this.header(Component.translatable("hitboxstudio.header.settings"));
		this.row(
			this.pageButton("general", () -> new GeneralScreen(this)),
			this.pageButton("default_style", () -> new DefaultStyleScreen(this))
		);
		this.row(
			this.pageButton("highlights", () -> new HighlightsScreen(this)),
			this.pageButton("hidden_entities", () -> new HiddenEntitiesScreen(this))
		);

		this.header(Component.translatable("hitboxstudio.header.entities"));
		Category[] categories = Category.values();
		for (int i = 0; i < categories.length; i += 2) {
			Button left = this.categoryButton(categories[i]);
			Button right = i + 1 < categories.length ? this.categoryButton(categories[i + 1]) : null;
			this.row(left, right);
		}
	}

	private Button pageButton(String key, Supplier<Screen> page) {
		return Widgets.button(Component.translatable("hitboxstudio.button.open", Component.translatable("hitboxstudio.page." + key)),
			Component.translatable("hitboxstudio.page." + key + ".tooltip"), Widgets.HALF, b -> this.minecraft.setScreen(page.get()));
	}

	private Button categoryButton(Category category) {
		Button button = Widgets.button(categoryLabel(category), Component.translatable("hitboxstudio.category." + category.key + ".info"),
			Widgets.HALF, b -> this.minecraft.setScreen(new CategoryScreen(this, category)));
		this.categoryButtons.put(category, button);
		return button;
	}

	private static Component categoryLabel(Category category) {
		Component name = Component.translatable("hitboxstudio.tab." + category.key);
		return cfg().get(category).enabled
			? Component.translatable("hitboxstudio.button.open", name)
			: Component.translatable("hitboxstudio.button.open_off", name);
	}

	@Override
	public void added() {
		super.added();
		// Coming back from a page: show the current state (a section may have been switched off or reset).
		if (this.shownBefore && this.enabledButton != null && this.modeButton != null) {
			this.enabledButton.setValue(cfg().general.enabled);
			this.modeButton.setValue(cfg().general.mode);
			this.categoryButtons.forEach((category, button) -> button.setMessage(categoryLabel(category)));
			this.refresh();
		}
		this.shownBefore = true;
	}

	@Override
	protected void addFooter() {
		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		footer.addChild(Widgets.button(CommonComponents.GUI_DONE, null, Widgets.HALF, b -> this.onClose()));
		footer.addChild(Widgets.button(CommonComponents.GUI_CANCEL, Component.translatable("hitboxstudio.button.cancel.tooltip"), Widgets.HALF,
			b -> this.cancel()));
	}

	@Override
	public void onClose() {
		ConfigManager.markChanged();
		ConfigManager.save();
		this.minecraft.setScreen(this.lastScreen);
	}

	private void cancel() {
		ConfigManager.replace(this.snapshot);
		ConfigManager.save();
		this.minecraft.setScreen(this.lastScreen);
	}
}
