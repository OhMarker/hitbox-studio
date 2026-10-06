package dev.hitboxstudio.mixin;

import dev.hitboxstudio.render.HitboxRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DebugRenderer#emitGizmos runs every frame inside the level's gizmo collector, whether or not F3+B is on.
 * Hooking its tail lets "Always on" mode work without faking the F3+B toggle.
 */
@Mixin(DebugRenderer.class)
public abstract class DebugRendererMixin {
	@Inject(method = "emitGizmos", at = @At("TAIL"))
	private void hitboxstudio$emitHitboxes(Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick, CallbackInfo ci) {
		HitboxRenderer.emitFromDebugRenderer(frustum, partialTick);
	}
}
