package dev.hitboxstudio.gui.widgets;

import dev.hitboxstudio.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Vanilla slider showing "Label: value". Snaps to a step; right-click resets to the default. */
public final class ValueSlider extends AbstractSliderButton {
	@FunctionalInterface
	public interface FloatGetter {
		float get();
	}

	@FunctionalInterface
	public interface FloatSetter {
		void set(float value);
	}

	@FunctionalInterface
	public interface Formatter {
		String format(float value);
	}

	private final Component label;
	private final float min;
	private final float max;
	private final float step;
	private final float defaultValue;
	private final FloatGetter getter;
	private final FloatSetter setter;
	private final Formatter formatter;
	private final Runnable onChange;

	public ValueSlider(
		int width, Component label, float min, float max, float step, float defaultValue,
		FloatGetter getter, FloatSetter setter, Formatter formatter, Runnable onChange
	) {
		super(0, 0, width, 20, Component.empty(), 0.0);
		this.label = label;
		this.min = min;
		this.max = max;
		this.step = step;
		this.defaultValue = defaultValue;
		this.getter = getter;
		this.setter = setter;
		this.formatter = formatter;
		this.onChange = onChange;
		this.syncFromGetter();
	}

	/** Re-reads the stored value (after something else changed it). */
	public void syncFromGetter() {
		this.value = (this.getter.get() - this.min) / (this.max - this.min);
		this.value = Math.max(0.0, Math.min(1.0, this.value));
		this.updateMessage();
	}

	private float snapped() {
		float raw = (float) (this.min + this.value * (this.max - this.min));
		float snapped = this.min + Math.round((raw - this.min) / this.step) * this.step;
		snapped = Math.max(this.min, Math.min(this.max, snapped));
		// Trim float noise like 0.30000001 so the JSON stays clean.
		return Math.round(snapped * 10000.0F) / 10000.0F;
	}

	@Override
	protected void updateMessage() {
		this.setMessage(CommonComponents.optionNameValue(this.label, Component.literal(this.formatter.format(this.snapped()))));
	}

	@Override
	protected void applyValue() {
		float value = this.snapped();
		if (value != this.getter.get()) {
			this.setter.set(value);
			ConfigManager.markChanged();
			this.onChange.run();
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 1 && this.active && this.visible && this.isMouseOver(event.x(), event.y())) {
			this.setter.set(this.defaultValue);
			ConfigManager.markChanged();
			this.syncFromGetter();
			this.playDownSound(Minecraft.getInstance().getSoundManager());
			this.onChange.run();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}
}
