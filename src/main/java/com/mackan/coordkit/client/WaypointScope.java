package com.mackan.coordkit.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;

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

		// Prefer the multiplayer server data when it exists. In singleplayer it
		// is null and the integrated server is used instead.
		ServerData server = client.getCurrentServer();

		if (server != null) {
			if (server.ip != null && !server.ip.isBlank()) {
				return scopeKey("mp", server.ip);
			}

			if (server.name != null && !server.name.isBlank()) {
				return scopeKey("mp", server.name);
			}
		}

		if (client.hasSingleplayerServer()) {
			IntegratedServer integrated = client.getSingleplayerServer();

			if (integrated != null) {
				// getServerDirectory() is the instance folder (for example
				// ".minecraft"), so use the level root to get the actual save.
				return singleplayerKey(integrated.getWorldPath(LevelResource.ROOT));
			}
		}

		return "";
	}

	private static String scopeKey(String prefix, String value) {
		if (value == null || value.isBlank()) {
			return "";
		}

		return normalizeKey(prefix + ":" + value);
	}

	/**
	 * Builds the singleplayer key from the level root path. The root is the
	 * save folder with {@code "."} appended, so it has to be normalised before
	 * taking the file name. Kept package-private for tests.
	 */
	static String singleplayerKey(Path levelRoot) {
		if (levelRoot == null) {
			return "";
		}

		Path normalized = levelRoot.normalize();
		Path name = normalized.getFileName();

		if (name == null) {
			return "";
		}

		return scopeKey("sp", name.toString());
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
	 * Turns a missing or non-canonical world key (from a pre-1.3.0 config or a
	 * hand edit) into the key format used for comparisons.
	 */
	public static String normalizeWorld(String world) {
		return normalizeKey(world);
	}

	/**
	 * Canonical form of a scope key: lower case, no surrounding whitespace and
	 * no explicit default Minecraft port.
	 */
	static String normalizeKey(String key) {
		if (key == null) {
			return "";
		}

		String normalized = key.trim().toLowerCase(Locale.ROOT);

		if (normalized.startsWith("mp:") && normalized.endsWith(":25565")) {
			normalized = normalized.substring(0, normalized.length() - ":25565".length());
		}

		return normalized;
	}

	public static boolean isVisibleIn(Waypoint waypoint, String currentWorldKey) {
		return waypoint != null && isVisibleIn(waypoint.world, currentWorldKey);
	}

	/**
	 * Pure matching logic, kept separate so it can be tested without a client.
	 */
	public static boolean isVisibleIn(String waypointWorld, String currentWorldKey) {
		String waypointKey = normalizeKey(waypointWorld);

		if (waypointKey.isEmpty()) {
			return true;
		}

		String currentKey = normalizeKey(currentWorldKey);

		// Scoped waypoints cannot be matched without knowing the world, so
		// keep them hidden instead of leaking them into every world.
		if (currentKey.isEmpty()) {
			return false;
		}

		return waypointKey.equals(currentKey);
	}
}
