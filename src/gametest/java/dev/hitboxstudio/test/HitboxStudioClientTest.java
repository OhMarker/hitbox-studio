package dev.hitboxstudio.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hitboxstudio.config.ConfigManager;
import dev.hitboxstudio.config.HitboxConfig;
import dev.hitboxstudio.gui.ColorPickerScreen;
import dev.hitboxstudio.gui.HitboxStudioScreen;
import java.lang.reflect.Field;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.MixinEnvironment;

/**
 * Drives the real game: clicks through every Hitbox Studio page with real mouse/keyboard input, then builds a
 * world, spawns crystals and mobs, turns on F3+B and screenshots each look.
 * Screenshots land in build/run/clientGameTest/screenshots.
 */
public final class HitboxStudioClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext ctx) {
		ctx.getInput().resizeWindow(1280, 720);
		ctx.runOnClient(mc -> {
			mc.options.guiScale().set(2);
			mc.resizeDisplay();
		});
		ctx.waitTicks(2);

		this.menuTests(ctx);
		this.colorPickerTests(ctx);
		this.worldTests(ctx);

		ctx.runOnClient(mc -> MixinEnvironment.getCurrentEnvironment().audit());
		log("ALL CHECKS PASSED");
	}

	// ------------------------------------------------------------ menus

	private void menuTests(ClientGameTestContext ctx) {
		ctx.runOnClient(mc -> ConfigManager.replace(new HitboxConfig()));
		ctx.setScreen(() -> new HitboxStudioScreen(new TitleScreen()));
		ctx.waitTicks(3);
		shot(ctx, "gui_01_main");

		click(ctx, "Show: With F3+B");
		check(ctx.computeOnClient(mc -> ConfigManager.get().general.mode) == HitboxConfig.Mode.ALWAYS_ON, "mode button cycles");
		click(ctx, "Show: Always");
		check(ctx.computeOnClient(mc -> ConfigManager.get().general.mode) == HitboxConfig.Mode.FOLLOW_F3B, "mode button cycles back");

		// General page: slider click at 25%, then right-click reset.
		click(ctx, "General...");
		shot(ctx, "gui_02_general");
		clickAt(ctx, "Opacity: 100%", 0.25, 0);
		float opacity = ctx.computeOnClient(mc -> ConfigManager.get().general.opacityMultiplier);
		check(opacity > 0.15F && opacity < 0.35F, "opacity slider click (" + opacity + ")");
		clickAt(ctx, "Opacity: ", 0.5, 1);
		check(ctx.computeOnClient(mc -> ConfigManager.get().general.opacityMultiplier) == 1.0F, "right-click resets slider");
		click(ctx, "Done");

		// Hidden entities: type an ID and press Enter.
		click(ctx, "Hidden Entities...");
		click(ctx, "Entity ID");
		ctx.getInput().typeChars("bat");
		ctx.getInput().pressKey(InputConstants.KEY_RETURN);
		ctx.waitTicks(3);
		check(ctx.computeOnClient(mc -> ConfigManager.get().general.hiddenEntities.contains("minecraft:bat")), "hidden entity added");
		shot(ctx, "gui_03_hidden_entities");
		click(ctx, "Done");

		click(ctx, "Default Style...");
		shot(ctx, "gui_04_default_style");
		click(ctx, "Done");

		click(ctx, "Highlights...");
		click(ctx, "Crosshair Target: OFF");
		check(ctx.computeOnClient(mc -> ConfigManager.get().highlights.target.enabled), "highlight toggle");
		shot(ctx, "gui_05_highlights");
		click(ctx, "Done");

		click(ctx, "End Crystals...");
		shot(ctx, "gui_06_crystals");
		click(ctx, "Custom Style: ON");
		check(!ctx.computeOnClient(mc -> ConfigManager.get().crystals.customStyle), "custom style toggle");
		shot(ctx, "gui_07_crystals_default_style");
		click(ctx, "Custom Style: OFF");
		click(ctx, "Done");

		click(ctx, "Projectiles...");
		click(ctx, "Hitboxes: ON");
		check(!ctx.computeOnClient(mc -> ConfigManager.get().projectiles.enabled), "section toggle");
		check(!ctx.computeOnClient(mc -> findWidget(mc, "Arrows: ON").active), "section off greys out its buttons");
		shot(ctx, "gui_08_projectiles_off");
		click(ctx, "Done");
		shot(ctx, "gui_09_main_after_edits");
		check(ctx.computeOnClient(mc -> findWidget(mc, "Projectiles").getMessage().getString().contains("(Off)")), "main screen shows section off");

		// Cancel undoes everything done since the screen opened.
		click(ctx, "Cancel");
		HitboxConfig after = ctx.computeOnClient(mc -> ConfigManager.get());
		check(after.projectiles.enabled && after.general.hiddenEntities.isEmpty() && !after.highlights.target.enabled, "cancel restores snapshot");

		// Small window, auto GUI scale: vanilla list still fits, preview hides.
		ctx.runOnClient(mc -> {
			mc.options.guiScale().set(0);
			mc.resizeDisplay();
		});
		ctx.getInput().resizeWindow(854, 480);
		ctx.setScreen(() -> new HitboxStudioScreen(new TitleScreen()));
		ctx.waitTicks(3);
		shot(ctx, "gui_10_small_main");
		click(ctx, "End Crystals...");
		shot(ctx, "gui_11_small_crystals");
		ctx.getInput().resizeWindow(1280, 720);
		ctx.runOnClient(mc -> {
			mc.options.guiScale().set(2);
			mc.resizeDisplay();
		});
		ctx.setScreen(TitleScreen::new);
		ctx.waitTicks(2);
	}

	// ------------------------------------------------------------ color wheel

	private void colorPickerTests(ClientGameTestContext ctx) {
		ctx.setScreen(() -> new HitboxStudioScreen(new TitleScreen()));
		ctx.waitTicks(3);
		click(ctx, "Default Style...");
		// First "Color" button on the page belongs to Box Outline.
		click(ctx, "Color");
		ctx.waitForScreen(ColorPickerScreen.class);
		ctx.waitTicks(2);
		shot(ctx, "gui_12_color_wheel");

		// Left edge of the wheel is cyan (hue 180) at full saturation.
		int wheelX = ctx.computeOnClient(mc -> intField(mc.screen, "wheelX"));
		int wheelY = ctx.computeOnClient(mc -> intField(mc.screen, "wheelY"));
		int size = ctx.computeOnClient(mc -> intField(mc.screen, "wheelSize"));
		clickGui(ctx, wheelX + 4, wheelY + size / 2.0, 0);
		shot(ctx, "gui_13_color_wheel_picked");
		click(ctx, "Done");
		int color = ctx.computeOnClient(mc -> ConfigManager.get().defaultStyle.box.color);
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = color & 0xFF;
		check(r < 60 && g > 150 && b > 150, String.format("wheel picked cyan (got #%08X)", color));
		check(!ctx.computeOnClient(mc -> ConfigManager.get().recentColors.isEmpty()), "recent colors recorded");
		shot(ctx, "gui_14_default_style_after_pick");
		click(ctx, "Done");
		click(ctx, "Done");
	}

	// ------------------------------------------------------------ in world

	private void worldTests(ClientGameTestContext ctx) {
		ctx.runOnClient(mc -> ConfigManager.replace(new HitboxConfig()));
		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			sp.getClientWorld().waitForChunksRender();
			sp.getServer().runCommand("difficulty easy");
			sp.getServer().runCommand("gamemode creative @a");
			sp.getServer().runCommand("time set noon");
			sp.getServer().runCommand("weather clear");
			sp.getServer().runCommand("tp @p 0.5 ~ 0.5 0 22");
			ctx.waitTicks(5);
			String spawn = "execute as @p at @s rotated ~ 0 run summon ";
			sp.getServer().runCommand(spawn + "minecraft:end_crystal ^ ^ ^3.5 {ShowBottom:0b}");
			sp.getServer().runCommand(spawn + "minecraft:end_crystal ^-4.5 ^ ^7 {ShowBottom:0b}");
			sp.getServer().runCommand(spawn + "minecraft:zombie ^3 ^ ^5 {NoAI:1b,PersistenceRequired:1b}");
			sp.getServer().runCommand(spawn + "minecraft:pig ^-2.5 ^ ^4 {NoAI:1b}");
			sp.getServer().runCommand(spawn + "minecraft:armor_stand ^1.5 ^ ^8");
			sp.getServer().runCommand(spawn + "minecraft:experience_orb ^0.5 ^ ^2 {Value:5}");
			ctx.waitTicks(10);
			sp.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~ facing entity @e[type=minecraft:end_crystal,sort=nearest,limit=1] feet");
			ctx.waitTicks(10);

			ctx.getInput().holdKey(InputConstants.KEY_F3);
			ctx.getInput().pressKey(InputConstants.KEY_B);
			ctx.getInput().releaseKey(InputConstants.KEY_F3);
			ctx.waitTicks(3);
			check(ctx.computeOnClient(mc -> mc.debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES)), "F3+B on");
			check(ctx.computeOnClient(mc -> mc.crosshairPickEntity != null && mc.crosshairPickEntity.getType().toString().contains("end_crystal")),
				"crosshair on crystal");
			realShot(ctx, "world_01_defaults_vanilla_look");

			ctx.runOnClient(mc -> {
				HitboxConfig c = ConfigManager.get();
				c.defaultStyle.box.color = 0xFFFFD84A;
				c.defaultStyle.box.width = 1.5F;
				c.defaultStyle.eyeLine.enabled = false;
				c.defaultStyle.lookVector.color = 0xFF38E08A;
				c.crystals.style.box.color = 0xFFD050FF;
				c.crystals.style.box.width = 4.0F;
				c.crystals.style.fill.enabled = true;
				c.crystals.style.fill.color = 0xFFD050FF;
				c.crystals.style.fill.opacity = 0.18F;
				c.crystals.targetColor.enabled = true;
				c.crystals.inReachColor.enabled = true;
				c.highlights.inReach.enabled = true;
				ConfigManager.markChanged();
			});
			realShot(ctx, "world_02_custom");

			// Config screen over the world (vanilla in-game menu background).
			ctx.setScreen(() -> new HitboxStudioScreen(null));
			ctx.waitTicks(3);
			click(ctx, "End Crystals...");
			realShot(ctx, "world_03_crystal_page_in_game");
			click(ctx, "Done");
			click(ctx, "Done");
			ctx.waitTicks(2);
		}
	}

	// ------------------------------------------------------------ helpers

	private static void log(String message) {
		System.out.println("[HitboxStudioTest] " + message);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("FAILED: " + what);
		}
		log("ok - " + what);
	}

	private static void shot(ClientGameTestContext ctx, String name) {
		ctx.waitTicks(1);
		log("screenshot " + ctx.takeScreenshot(name));
	}

	/**
	 * The harness's own screenshots render an extra frame outside the normal game loop, where the per-frame gizmo
	 * collector isn't active, so debug gizmos (all F3+B hitboxes, vanilla included) never appear in them.
	 * This grabs the last frame the game rendered normally, the same way F2 does.
	 */
	private static void realShot(ClientGameTestContext ctx, String name) {
		ctx.waitTicks(4);
		ctx.runOnClient(mc -> Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), 1,
			message -> log("screenshot " + message.getString())));
		ctx.waitTicks(4);
	}

	private static int intField(Screen screen, String name) {
		try {
			Field field = screen.getClass().getDeclaredField(name);
			field.setAccessible(true);
			return field.getInt(screen);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	/** First visible widget (in screen order) whose text starts with the given label. */
	private static AbstractWidget findWidget(Minecraft mc, String label) {
		AbstractWidget found = search(mc.screen, label);
		if (found == null) {
			throw new AssertionError("No widget labelled '" + label + "' on " + mc.screen.getClass().getSimpleName());
		}
		return found;
	}

	private static @Nullable AbstractWidget search(ContainerEventHandler parent, String label) {
		for (GuiEventListener child : parent.children()) {
			if (child instanceof AbstractWidget widget && widget.visible && !(child instanceof ContainerEventHandler)
				&& widget.getMessage().getString().startsWith(label)) {
				return widget;
			}
			if (child instanceof ContainerEventHandler container) {
				AbstractWidget found = search(container, label);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private static void click(ClientGameTestContext ctx, String label) {
		clickAt(ctx, label, 0.5, 0);
	}

	/** Clicks a widget found by label, at a fraction of its width. */
	private static void clickAt(ClientGameTestContext ctx, String label, double fraction, int button) {
		ctx.waitTicks(1);
		double[] pos = ctx.computeOnClient(mc -> {
			AbstractWidget widget = findWidget(mc, label);
			return new double[] {widget.getX() + 2 + (widget.getWidth() - 4) * fraction, widget.getY() + widget.getHeight() / 2.0};
		});
		clickGui(ctx, pos[0], pos[1], button);
		ctx.waitTicks(2);
	}

	private static void clickGui(ClientGameTestContext ctx, double guiX, double guiY, int button) {
		double scale = ctx.computeOnClient(mc -> (double) mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth());
		ctx.getInput().setCursorPos(guiX * scale, guiY * scale);
		ctx.getInput().pressMouse(button);
		ctx.waitTicks(1);
	}
}
