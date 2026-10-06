package dev.hitboxstudio.gui.widgets;

import dev.hitboxstudio.config.ConfigManager;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Builders for the vanilla widgets used on every Hitbox Studio page. */
public final class Widgets {
	public static final int HALF = 150;
	public static final int FULL = 310;

	private Widgets() {
	}

	public static @Nullable Tooltip tooltip(@Nullable Component text) {
		return text == null ? null : Tooltip.create(text);
	}

	/** "Label: ON/OFF" cycle button. */
	public static CycleButton<Boolean> toggle(Component label, @Nullable Component tooltip, BooleanSupplier getter, Consumer<Boolean> setter, Runnable onChange) {
		return CycleButton.onOffBuilder(getter.getAsBoolean())
			.withTooltip(value -> tooltip(tooltip))
			.create(0, 0, HALF, 20, label, (button, value) -> {
				setter.accept(value);
				ConfigManager.markChanged();
				onChange.run();
			});
	}

	/** "Label: Value" cycle button over an enum. */
	public static <E extends Enum<E>> CycleButton<E> cycle(
		Component label, @Nullable Component tooltip, E[] values, Supplier<E> getter, Consumer<E> setter, Function<E, Component> namer, Runnable onChange
	) {
		return CycleButton.builder(namer, getter.get())
			.withValues(values)
			.withTooltip(value -> tooltip(tooltip))
			.create(0, 0, HALF, 20, label, (button, value) -> {
				setter.accept(value);
				ConfigManager.markChanged();
				onChange.run();
			});
	}

	public static Button button(Component label, @Nullable Component tooltip, int width, Button.OnPress onPress) {
		return Button.builder(label, onPress).width(width).tooltip(tooltip(tooltip)).build();
	}
}
