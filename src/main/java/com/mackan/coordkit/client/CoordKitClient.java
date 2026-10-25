package com.mackan.coordkit.client;

import com.mackan.coordkit.CoordKit;
import com.mackan.coordkit.client.ui.CoordKitConfigScreen;import com.mackan.coordkit.client.ui.WaypointManagerScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Client entrypoint: registers the HUD layer, the keybinds and the screens.
 */
public final class CoordKitClient implements ClientModInitializer {
	public static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(CoordKit.id("main"));

	private static final KeyMapping TOGGLE_HUD = register("key.coordkit.toggle_hud", InputConstants.KEY_F6);
	private static final KeyMapping COPY_COORDS = register("key.coordkit.copy_coords", InputConstants.KEY_F7);
	private static final KeyMapping OPEN_SETTINGS = register("key.coordkit.open_settings", InputConstants.KEY_F8);
	private static final KeyMapping OPEN_WAYPOINTS = register("key.coordkit.open_waypoints", InputConstants.KEY_F9);
	private static final KeyMapping ADD_WAYPOINT = register("key.coordkit.add_waypoint", InputConstants.KEY_F10);

	private static boolean wasDead;

	private static KeyMapping register(String translationKey, int keyCode) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping(
				translationKey,
				InputConstants.Type.KEYSYM,
				keyCode,
				CATEGORY
		));
	}

	@Override
	public void onInitializeClient() {
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				CoordKit.id("coords"),
				CoordKitHud::render
		);

		ClientTickEvents.END_CLIENT_TICK.register(CoordKitClient::onEndTick);

		CoordKit.LOGGER.info("CoordKit loaded (F6 HUD, F7 copy coords, F8 settings, F9 waypoints, F10 add waypoint)");
	}

	private static void onEndTick(Minecraft client) {
		while (TOGGLE_HUD.consumeClick()) {
			CoordKitConfig config = CoordKitConfig.get();
			config.showHud = !config.showHud;
			config.save();
			ClientFeedback.actionBar(client, "CoordKit HUD " + (config.showHud ? "enabled" : "disabled"));
		}

		while (COPY_COORDS.consumeClick()) {
			if (client.player == null) {
				continue;
			}

			String command = String.format(
					java.util.Locale.ROOT,
					"/tp @s %.2f %.2f %.2f",
					client.player.getX(),
					client.player.getY(),
					client.player.getZ()
			);

			client.keyboardHandler.setClipboard(command);
			client.player.sendSystemMessage(Component.literal("Copied: " + command));
		}

		while (OPEN_SETTINGS.consumeClick()) {
			client.gui.setScreen(new CoordKitConfigScreen(null));
		}

		while (OPEN_WAYPOINTS.consumeClick()) {
			client.gui.setScreen(new WaypointManagerScreen(null));
		}

		while (ADD_WAYPOINT.consumeClick()) {
			WaypointActions.addHere(client, CoordKitConfig.get());
		}

		handleDeath(client);
	}

	private static void handleDeath(Minecraft client) {
		if (client.player == null || client.level == null) {
			wasDead = false;
			return;
		}

		boolean dead = client.player.isDeadOrDying();

		if (dead && !wasDead) {
			CoordKitConfig config = CoordKitConfig.get();

			if (config.createDeathWaypoint) {
				WaypointActions.createDeathWaypoint(client, config);
			}
		}

		wasDead = dead;
	}
}
