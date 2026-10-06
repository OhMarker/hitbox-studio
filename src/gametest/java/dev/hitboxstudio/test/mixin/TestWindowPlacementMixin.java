package dev.hitboxstudio.test.mixin;

import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Test runs only (never in the mod jar): opens the test window without taking keyboard focus, and on the
 * monitor whose top-left corner matches -Dhitboxstudio.test.monitor=X,Y, so it doesn't interrupt a game
 * being played on the main screen.
 */
@Mixin(Window.class)
public abstract class TestWindowPlacementMixin {
	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"))
	private void hitboxstudioTest$dontStealFocus(
		WindowEventHandler eventHandler, ScreenManager screenManager, DisplayData displayData, @Nullable String videoMode, String title, CallbackInfo ci
	) {
		GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
	}

	@ModifyArg(method = "<init>", at = @At(value = "INVOKE",
		target = "Lcom/mojang/blaze3d/platform/ScreenManager;getMonitor(J)Lcom/mojang/blaze3d/platform/Monitor;"))
	private long hitboxstudioTest$pickMonitor(long primaryMonitor) {
		String wanted = System.getProperty("hitboxstudio.test.monitor");
		if (wanted == null) {
			return primaryMonitor;
		}
		String[] parts = wanted.split(",");
		int wantedX = Integer.parseInt(parts[0].trim());
		int wantedY = Integer.parseInt(parts[1].trim());
		PointerBuffer monitors = GLFW.glfwGetMonitors();
		if (monitors == null) {
			return primaryMonitor;
		}
		int[] x = new int[1];
		int[] y = new int[1];
		for (int i = 0; i < monitors.limit(); i++) {
			long monitor = monitors.get(i);
			GLFW.glfwGetMonitorPos(monitor, x, y);
			if (x[0] == wantedX && y[0] == wantedY) {
				System.out.println("[HitboxStudioTest] test window on monitor at " + wanted + " (" + GLFW.glfwGetMonitorName(monitor) + ")");
				return monitor;
			}
		}
		System.out.println("[HitboxStudioTest] no monitor at " + wanted + ", using the main one");
		return primaryMonitor;
	}
}
