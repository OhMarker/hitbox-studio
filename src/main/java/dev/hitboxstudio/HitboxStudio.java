package dev.hitboxstudio;

import dev.hitboxstudio.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HitboxStudio implements ClientModInitializer {
	public static final String MOD_ID = "hitboxstudio";
	public static final Logger LOGGER = LoggerFactory.getLogger("Hitbox Studio");

	@Override
	public void onInitializeClient() {
		ConfigManager.load();
	}
}
