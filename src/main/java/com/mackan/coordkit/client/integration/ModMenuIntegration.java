package com.mackan.coordkit.client.integration;

import com.mackan.coordkit.client.ui.CoordKitConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Optional Mod Menu hook. This class is only loaded by Mod Menu, so the mod
 * still works when Mod Menu is not installed.
 */
public final class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return CoordKitConfigScreen::new;
	}
}
