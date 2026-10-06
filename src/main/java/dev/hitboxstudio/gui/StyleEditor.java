package dev.hitboxstudio.gui;

import dev.hitboxstudio.config.HitboxStyle;
import dev.hitboxstudio.config.PartStyle;
import dev.hitboxstudio.gui.widgets.ColorButton;
import dev.hitboxstudio.gui.widgets.ValueSlider;
import dev.hitboxstudio.gui.widgets.Widgets;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Adds one header + button rows per hitbox part (Box Outline, Fill, Eye Height Line, ...) to a page. */
final class StyleEditor {
	private StyleEditor() {
	}

	static Component opt(String key) {
		return Component.translatable("hitboxstudio.option." + key);
	}

	static Component tip(String key) {
		return Component.translatable("hitboxstudio.option." + key + ".tooltip");
	}

	static String px(float value) {
		return String.format(Locale.ROOT, "%.1f px", value);
	}

	static String percent(float value) {
		return Math.round(value * 100.0F) + "%";
	}

	/**
	 * @param style      the style being edited (read through the supplier so resets never leave stale references)
	 * @param defaults   values restored by right-click
	 * @param sectionOn  false greys out everything (e.g. the entity section is switched off)
	 */
	static void add(HitboxOptionsScreen screen, Supplier<HitboxStyle> style, HitboxStyle defaults, BooleanSupplier sectionOn) {
		part(screen, "box", () -> style.get().box, defaults.box, sectionOn, true, false, partOn ->
			screen.row(screen.gate(slider(screen, "box_expand", -0.5F, 0.5F, 0.01F, defaults.boxExpand,
				() -> style.get().boxExpand, v -> style.get().boxExpand = v,
				v -> Component.translatable("hitboxstudio.value.blocks", String.format(Locale.ROOT, "%+.2f", v)).getString()), partOn), null));

		part(screen, "fill", () -> style.get().fill, defaults.fill, sectionOn, false, false, partOn -> {
		});

		part(screen, "eye_line", () -> style.get().eyeLine, defaults.eyeLine, sectionOn, true, false, partOn -> {
		});

		part(screen, "look_vector", () -> style.get().lookVector, defaults.lookVector, sectionOn, true, false, partOn -> {
			AbstractWidget length = screen.gate(slider(screen, "look_length", 0.5F, 10.0F, 0.1F, defaults.lookLength,
				() -> style.get().lookLength, v -> style.get().lookLength = v,
				v -> Component.translatable("hitboxstudio.value.blocks", String.format(Locale.ROOT, "%.1f", v)).getString()), partOn);
			AbstractWidget arrow = screen.gate(Widgets.toggle(opt("look_arrow_head"), tip("look_arrow_head"),
				() -> style.get().lookArrowHead, v -> style.get().lookArrowHead = v, screen::refresh), partOn);
			screen.row(length, arrow);
		});

		part(screen, "position_dot", () -> style.get().positionDot, defaults.positionDot, sectionOn, true, true, partOn -> {
		});

		part(screen, "ride_seat", () -> style.get().rideSeat, defaults.rideSeat, sectionOn, true, false, partOn -> {
		});

		part(screen, "multipart", () -> style.get().multipart, defaults.multipart, sectionOn, true, false, partOn -> {
		});
	}

	private static void part(
		HitboxOptionsScreen screen, String key, Supplier<PartStyle> part, PartStyle defaults, BooleanSupplier sectionOn,
		boolean hasWidth, boolean widthIsSize, Consumer<BooleanSupplier> extraRows
	) {
		Component title = Component.translatable("hitboxstudio.part." + key);
		Component description = Component.translatable("hitboxstudio.part." + key + ".tooltip");
		BooleanSupplier partOn = () -> sectionOn.getAsBoolean() && part.get().enabled;
		screen.header(title);

		AbstractWidget show = screen.gate(Widgets.toggle(opt("show"), description,
			() -> part.get().enabled, v -> part.get().enabled = v, screen::refresh), sectionOn);
		ColorButton color = new ColorButton(Widgets.HALF, opt("color"), part, defaults.color,
			b -> screen.openColorPicker(title, part.get()), screen::refresh);
		color.setTooltip(Widgets.tooltip(tip("color")));
		screen.row(show, screen.gate(color, partOn));

		@Nullable AbstractWidget width = null;
		if (hasWidth) {
			String widthKey = widthIsSize ? "size" : "width";
			width = screen.gate(slider(screen, widthKey, 0.1F, 10.0F, 0.1F, defaults.width,
				() -> part.get().width, v -> part.get().width = v, StyleEditor::px), partOn);
		}
		AbstractWidget opacity = screen.gate(slider(screen, key.equals("fill") ? "fill_opacity" : "opacity", 0.0F, 1.0F, 0.01F, defaults.opacity,
			() -> part.get().opacity, v -> part.get().opacity = v, StyleEditor::percent), partOn);
		if (width != null) {
			screen.row(width, opacity);
		} else {
			screen.row(opacity, null);
		}
		extraRows.accept(partOn);
	}

	static ValueSlider slider(
		HitboxOptionsScreen screen, String key, float min, float max, float step, float defaultValue,
		ValueSlider.FloatGetter getter, ValueSlider.FloatSetter setter, ValueSlider.Formatter formatter
	) {
		ValueSlider slider = new ValueSlider(Widgets.HALF, opt(key), min, max, step, defaultValue, getter, setter, formatter, screen::refresh);
		slider.setTooltip(Widgets.tooltip(tip(key)));
		return slider;
	}
}
