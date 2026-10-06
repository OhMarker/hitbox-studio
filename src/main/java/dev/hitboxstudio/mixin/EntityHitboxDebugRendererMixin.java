package dev.hitboxstudio.mixin;

import dev.hitboxstudio.config.ConfigManager;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.util.debug.DebugValueAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Turns off vanilla's F3+B drawing while Hitbox Studio is enabled; HitboxRenderer draws instead. */
@Mixin(EntityHitboxDebugRenderer.class)
public abstract class EntityHitboxDebugRendererMixin {
	@Inject(method = "emitGizmos", at = @At("HEAD"), cancellable = true)
	private void hitboxstudio$replaceVanillaHitboxes(
		double cameraX, double cameraY, double cameraZ, DebugValueAccess debugValueAccess, Frustum frustum, float partialTick, CallbackInfo ci
	) {
		if (ConfigManager.get().general.enabled) {
			ci.cancel();
		}
	}
}
