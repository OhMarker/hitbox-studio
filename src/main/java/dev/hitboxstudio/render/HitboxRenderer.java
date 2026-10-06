package dev.hitboxstudio.render;

import dev.hitboxstudio.config.Category;
import dev.hitboxstudio.config.CategoryConfig;
import dev.hitboxstudio.config.ConfigManager;
import dev.hitboxstudio.config.HitboxConfig;
import dev.hitboxstudio.config.HitboxStyle;
import dev.hitboxstudio.config.PartStyle;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws hitboxes through vanilla's gizmo system, the same path F3+B uses in 1.21.11, so they are
 * depth-tested exactly like vanilla (nothing through walls) and pick up per-line width support.
 */
public final class HitboxRenderer {
	private static Set<EntityType<?>> hiddenTypes = Collections.emptySet();
	private static int hiddenTypesVersion = -1;

	private HitboxRenderer() {
	}

	/** Called at the end of DebugRenderer#emitGizmos, while the per-frame gizmo collector is active. */
	public static void emitFromDebugRenderer(Frustum frustum, float partialTick) {
		HitboxConfig config = ConfigManager.get();
		if (!config.general.enabled) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		boolean f3b = minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES);
		if (!f3b) {
			if (config.general.mode != HitboxConfig.Mode.ALWAYS_ON) {
				return;
			}
			// Servers that turn on reduced debug info block F3+B; respect that for "Always on" too.
			if (minecraft.player != null && minecraft.player.isReducedDebugInfo()) {
				return;
			}
		}
		emit(minecraft, config, frustum, partialTick);
	}

	private static void emit(Minecraft minecraft, HitboxConfig config, Frustum frustum, float partialTick) {
		ClientLevel level = minecraft.level;
		if (level == null) {
			return;
		}
		HitboxConfig.General general = config.general;
		if (general.hideInF1 && minecraft.options.hideGui) {
			return;
		}
		refreshHiddenTypes(config);

		LocalPlayer player = minecraft.player;
		Entity cameraEntity = minecraft.getCameraEntity();
		boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
		Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().position();
		double maxDistanceSq = general.maxDistance > 0.0F ? (double) general.maxDistance * general.maxDistance : Double.MAX_VALUE;
		Entity crosshairTarget = minecraft.crosshairPickEntity;
		long now = Util.getMillis();

		for (Entity entity : level.entitiesForRendering()) {
			// Same visibility rules as vanilla's EntityHitboxDebugRenderer.
			if (entity.isInvisible()
				|| !frustum.isVisible(entity.getBoundingBox())
				|| (entity == cameraEntity && firstPerson)) {
				continue;
			}
			if (entity.distanceToSqr(cameraPos) > maxDistanceSq || hiddenTypes.contains(entity.getType())) {
				continue;
			}
			Category category = EntityClassifier.classify(entity, player);
			CategoryConfig section = config.get(category);
			if (!section.enabled) {
				continue;
			}
			if (category == Category.PROJECTILES && !config.projectiles.isShown(EntityClassifier.projectileKind(entity))) {
				continue;
			}
			HitboxStyle style = section.customStyle ? section.style : config.defaultStyle;
			int highlight = highlightFor(config, category, entity, player, crosshairTarget, now);
			drawEntity(entity, style, general, config.highlights, highlight, partialTick, now);
		}
	}

	private static int highlightFor(HitboxConfig config, Category category, Entity entity, LocalPlayer player, Entity target, long now) {
		if (category == Category.CRYSTALS) {
			if (config.crystals.targetColor.enabled && entity == target) {
				return ColorMath.highlightOf(config.crystals.targetColor, now);
			}
			if (config.crystals.inReachColor.enabled && isInReach(player, entity)) {
				return ColorMath.highlightOf(config.crystals.inReachColor, now);
			}
			return ColorMath.NO_HIGHLIGHT;
		}
		HitboxConfig.Highlights highlights = config.highlights;
		if (highlights.hurt.enabled && entity instanceof LivingEntity living && living.hurtTime > 0) {
			return ColorMath.highlightOf(highlights.hurt, now);
		}
		if (highlights.target.enabled && entity == target) {
			return ColorMath.highlightOf(highlights.target, now);
		}
		if (highlights.inReach.enabled && entity != player && isInReach(player, entity)) {
			return ColorMath.highlightOf(highlights.inReach, now);
		}
		return ColorMath.NO_HIGHLIGHT;
	}

	private static boolean isInReach(LocalPlayer player, Entity entity) {
		return player != null && player.isWithinEntityInteractionRange(entity.getBoundingBox(), 0.0);
	}

	private static void drawEntity(
		Entity entity, HitboxStyle style, HitboxConfig.General general, HitboxConfig.Highlights highlights, int highlight, float partialTick, long now
	) {
		Vec3 position = entity.position();
		Vec3 lerped = entity.getPosition(partialTick);
		Vec3 offset = lerped.subtract(position);
		AABB box = entity.getBoundingBox().move(offset);

		// Bounding box outline + fill, as one cuboid gizmo.
		if (style.box.enabled || style.fill.enabled) {
			int stroke = 0;
			float strokeWidth = 0.0F;
			if (style.box.enabled) {
				float width = style.box.width * general.lineWidthMultiplier;
				int color = ColorMath.applyHighlight(ColorMath.animated(style.box, now), highlight, highlights.blendMode);
				stroke = ColorMath.finalColor(color, style.box.opacity, general.opacityMultiplier, width);
				strokeWidth = ColorMath.drawnWidth(width);
			}
			int fill = 0;
			if (style.fill.enabled) {
				int color = ColorMath.animated(style.fill, now);
				if (highlights.applyToFill) {
					color = ColorMath.applyHighlight(color, highlight, highlights.blendMode);
				}
				fill = ColorMath.finalColor(color, style.fill.opacity, general.opacityMultiplier, 1.0F);
			}
			if (stroke != 0 || fill != 0) {
				Gizmos.cuboid(expanded(box, style.boxExpand), new GizmoStyle(stroke, strokeWidth, fill));
			}
		}

		if (style.positionDot.enabled) {
			float size = style.positionDot.width * general.lineWidthMultiplier;
			int color = color(style.positionDot, general, size, now);
			if (color != 0) {
				Gizmos.point(lerped, color, ColorMath.drawnWidth(size));
			}
		}

		Entity vehicle = entity.getVehicle();
		if (vehicle != null && style.rideSeat.enabled) {
			float width = style.rideSeat.width * general.lineWidthMultiplier;
			int color = color(style.rideSeat, general, width, now);
			if (color != 0) {
				float half = Math.min(vehicle.getBbWidth(), entity.getBbWidth()) / 2.0F;
				Vec3 seat = vehicle.getPassengerRidingPosition(entity).add(offset);
				AABB seatBox = new AABB(seat.x - half, seat.y, seat.z - half, seat.x + half, seat.y + 0.0625, seat.z + half);
				Gizmos.cuboid(seatBox, GizmoStyle.stroke(color, ColorMath.drawnWidth(width)));
			}
		}

		if (entity instanceof LivingEntity && style.eyeLine.enabled) {
			float width = style.eyeLine.width * general.lineWidthMultiplier;
			int color = color(style.eyeLine, general, width, now);
			if (color != 0) {
				double eyeY = box.minY + entity.getEyeHeight();
				AABB eyeBox = new AABB(box.minX, eyeY - 0.01F, box.minZ, box.maxX, eyeY + 0.01F, box.maxZ);
				Gizmos.cuboid(eyeBox, GizmoStyle.stroke(color, ColorMath.drawnWidth(width)));
			}
		}

		if (entity instanceof EnderDragon dragon && style.multipart.enabled) {
			float width = style.multipart.width * general.lineWidthMultiplier;
			int color = color(style.multipart, general, width, now);
			if (color != 0) {
				GizmoStyle partStyle = GizmoStyle.stroke(color, ColorMath.drawnWidth(width));
				for (EnderDragonPart part : dragon.getSubEntities()) {
					Vec3 partOffset = part.getPosition(partialTick).subtract(part.position());
					Gizmos.cuboid(part.getBoundingBox().move(partOffset), partStyle);
				}
			}
		}

		if (style.lookVector.enabled) {
			float width = style.lookVector.width * general.lineWidthMultiplier;
			int color = color(style.lookVector, general, width, now);
			if (color != 0) {
				Vec3 eye = lerped.add(0.0, entity.getEyeHeight(), 0.0);
				Vec3 end = eye.add(entity.getViewVector(partialTick).scale(style.lookLength));
				if (style.lookArrowHead) {
					Gizmos.arrow(eye, end, color, ColorMath.drawnWidth(width));
				} else {
					Gizmos.line(eye, end, color, ColorMath.drawnWidth(width));
				}
			}
		}
	}

	private static int color(PartStyle part, HitboxConfig.General general, float widthPixels, long now) {
		return ColorMath.finalColor(ColorMath.animated(part, now), part.opacity, general.opacityMultiplier, widthPixels);
	}

	/** Grows/shrinks the drawn box without ever turning it inside out. */
	private static AABB expanded(AABB box, float expand) {
		if (expand == 0.0F) {
			return box;
		}
		double minHalf = Math.min(box.getXsize(), Math.min(box.getYsize(), box.getZsize())) / 2.0;
		double amount = Math.max(expand, -minHalf + 0.005);
		return box.inflate(amount);
	}

	private static void refreshHiddenTypes(HitboxConfig config) {
		if (hiddenTypesVersion == ConfigManager.version()) {
			return;
		}
		hiddenTypesVersion = ConfigManager.version();
		Set<EntityType<?>> types = Collections.newSetFromMap(new IdentityHashMap<>());
		for (String id : config.general.hiddenEntities) {
			Identifier identifier = Identifier.tryParse(id.trim());
			if (identifier != null) {
				BuiltInRegistries.ENTITY_TYPE.getOptional(identifier).ifPresent(types::add);
			}
		}
		hiddenTypes = types;
	}
}
