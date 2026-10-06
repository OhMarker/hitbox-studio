package dev.hitboxstudio.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.hitboxstudio.HitboxStudio;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;

/** Loads and saves config/hitboxstudio.json. The renderer reads the live object, never the file. */
public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static HitboxConfig config = new HitboxConfig();
	private static int version;

	private ConfigManager() {
	}

	public static HitboxConfig get() {
		return config;
	}

	/** Bumped on every change so render-side caches (hidden entity set, ...) know to rebuild. */
	public static int version() {
		return version;
	}

	public static void markChanged() {
		version++;
	}

	public static void replace(HitboxConfig newConfig) {
		config = newConfig;
		markChanged();
	}

	public static HitboxConfig copy(HitboxConfig source) {
		HitboxConfig copy = GSON.fromJson(GSON.toJson(source), HitboxConfig.class);
		copy.sanitize();
		return copy;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("hitboxstudio.json");
	}

	public static void load() {
		Path path = path();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				HitboxConfig loaded = GSON.fromJson(reader, HitboxConfig.class);
				if (loaded == null) {
					loaded = new HitboxConfig();
				}
				loaded.sanitize();
				config = loaded;
			} catch (Exception e) {
				HitboxStudio.LOGGER.error("Hitbox Studio config is broken, backing it up and using defaults", e);
				try {
					Files.copy(path, path.resolveSibling("hitboxstudio.json.broken"), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException copyError) {
					HitboxStudio.LOGGER.warn("Could not back up broken config", copyError);
				}
				config = new HitboxConfig();
			}
		} else {
			config = new HitboxConfig();
		}
		markChanged();
		// Writes back any options added since the file was last saved.
		save();
	}

	public static void save() {
		Path path = path();
		Path temp = path.resolveSibling("hitboxstudio.json.tmp");
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
			try {
				Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			HitboxStudio.LOGGER.error("Could not save Hitbox Studio config", e);
		}
	}
}
