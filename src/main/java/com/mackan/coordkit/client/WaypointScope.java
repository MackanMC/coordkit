package com.mackan.coordkit.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Identifies the current singleplayer world or multiplayer server.
 *
 * <p>Waypoints created with a non-empty world key are only shown in that world.
 * An empty key means the waypoint is global and appears in every world, which
 * is also how waypoints from older configs behave.</p>
 */
public final class WaypointScope {
	private WaypointScope() {
	}

	/**
	 * Returns a stable key for the current world, or an empty string when the
	 * world is not available (for example on the main menu).
	 */
	public static String currentWorldKey(Minecraft client) {
		if (client == null || client.level == null) {
			return "";
		}

		if (client.hasSingleplayerServer()) {
			IntegratedServer server = client.getSingleplayerServer();

			if (server != null) {
				Path directory = server.getServerDirectory();

				if (directory != null) {
					Path name = directory.getFileName();
					String id = name != null ? name.toString() : directory.toString();

					if (!id.isBlank()) {
						return "sp:" + id.toLowerCase(Locale.ROOT);
					}
				}
			}
		}

		ServerData server = client.getCurrentServer();

		if (server != null) {
			if (server.ip != null && !server.ip.isBlank()) {
				return "mp:" + server.ip.toLowerCase(Locale.ROOT);
			}

			if (server.name != null && !server.name.isBlank()) {
				return "mp:" + server.name.toLowerCase(Locale.ROOT);
			}
		}

		return "";
	}

	/**
	 * The world key to store on a newly created waypoint. Returns an empty
	 * string when scoping is disabled or the current world is unknown.
	 */
	public static String keyForNewWaypoint(Minecraft client, CoordKitConfig config) {
		if (config == null || !config.scopeNewWaypointsToWorld) {
			return "";
		}

		return currentWorldKey(client);
	}

	public static boolean isScoped(Waypoint waypoint) {
		return waypoint != null && waypoint.world != null && !waypoint.world.isEmpty();
	}

	/**
	 * Turns a missing world key (from a pre-1.3.0 config) into the global key.
	 */
	public static String normalizeWorld(String world) {
		return world == null ? "" : world;
	}

	public static boolean isVisibleIn(Waypoint waypoint, String currentWorldKey) {
		return waypoint != null && isVisibleIn(waypoint.world, currentWorldKey);
	}

	/**
	 * Pure matching logic, kept separate so it can be tested without a client.
	 */
	public static boolean isVisibleIn(String waypointWorld, String currentWorldKey) {
		if (waypointWorld == null || waypointWorld.isEmpty()) {
			return true;
		}

		// If the world cannot be identified, do not hide the waypoint.
		if (currentWorldKey == null || currentWorldKey.isEmpty()) {
			return true;
		}

		return waypointWorld.equals(currentWorldKey);
	}
}
