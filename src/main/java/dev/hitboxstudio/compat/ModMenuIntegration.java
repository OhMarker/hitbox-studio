package dev.hitboxstudio.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.hitboxstudio.gui.HitboxStudioScreen;

/** The only way into the settings: Mod Menu's config button. */
public final class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return HitboxStudioScreen::new;
	}
}
