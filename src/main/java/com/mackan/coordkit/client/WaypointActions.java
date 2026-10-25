package com.mackan.coordkit.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Shared creation logic for waypoints from the manager, keybinds and death.
 */
public final class WaypointActions {
	public static final String DEATH_NAME = "Death";
	public static final int DEATH_COLOR = 0xE05A5A;
	private static final double CROSSHAIR_RANGE = 128.0;
	private static final double DUPLICATE_RADIUS_SQR = 1.0;

	private WaypointActions() {
	}

	public static Waypoint create(Minecraft client, CoordKitConfig config, Vec3 position, String name, int color) {
		Waypoint waypoint = new Waypoint(
				name,
				position.x,
				position.y,
				position.z,
				client.level.dimension().identifier().toString(),
				color
		);
		waypoint.world = WaypointScope.keyForNewWaypoint(client, config);
		return waypoint;
	}

	public static String suggestedName(CoordKitConfig config) {
		Set<String> names = new HashSet<>();

		for (Waypoint waypoint : config.waypoints) {
			names.add(waypoint.name);
		}

		int index = config.waypoints.size() + 1;

		while (names.contains("Waypoint " + index)) {
			index++;
		}

		return "Waypoint " + index;
	}

	/**
	 * F10 quick-add: saves the player's current position immediately.
	 */
	public static void addHere(Minecraft client, CoordKitConfig config) {
		if (client.player == null || client.level == null) {
			return;
		}

		Vec3 position = client.player.position();
		Waypoint existing = findNearby(config, client, position);

		if (existing != null) {
			ClientFeedback.actionBar(client, "Waypoint already here: " + existing.name);
			return;
		}

		Waypoint waypoint = create(
				client,
				config,
				position,
				suggestedName(config),
				WaypointStyle.colorAt(config.waypoints.size())
		);
		config.waypoints.add(waypoint);
		config.save();
		ClientFeedback.actionBar(client, "Added waypoint: " + waypoint.name);
	}

	/**
	 * Returns a new (unsaved) waypoint at the block the player is looking at,
	 * or {@code null} when no block is within range.
	 */
	public static Waypoint createAtCrosshair(Minecraft client, CoordKitConfig config) {
		if (client.player == null || client.level == null) {
			return null;
		}

		HitResult hit = client.player.pick(CROSSHAIR_RANGE, 1.0f, false);

		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
			return null;
		}

		return create(
				client,
				config,
				Vec3.atCenterOf(blockHit.getBlockPos()),
				suggestedName(config),
				WaypointStyle.colorAt(config.waypoints.size())
		);
	}

	/**
	 * Creates or refreshes one automatic death waypoint per current world and
	 * dimension. Older auto death waypoints for the same scope are removed.
	 */
	public static void createDeathWaypoint(Minecraft client, CoordKitConfig config) {
		if (client.player == null || client.level == null) {
			return;
		}

		Vec3 position = client.player.position();
		String dimension = client.level.dimension().identifier().toString();
		String world = WaypointScope.keyForNewWaypoint(client, config);
		Waypoint death = null;

		for (Waypoint waypoint : config.waypoints) {
			if (waypoint.autoDeath
					&& waypoint.dimensionId().toString().equals(dimension)
					&& worldsEqual(waypoint.world, world)) {
				death = waypoint;
				break;
			}
		}

		if (death == null) {
			death = create(client, config, position, DEATH_NAME, DEATH_COLOR);
			death.autoDeath = true;
			config.waypoints.add(death);
		} else {
			death.name = DEATH_NAME;
			death.x = position.x;
			death.y = position.y;
			death.z = position.z;
			death.color = DEATH_COLOR;
			death.visible = true;
			death.world = world;
		}

		Waypoint kept = death;

		config.waypoints.removeIf(waypoint -> waypoint != kept
				&& waypoint.autoDeath
				&& waypoint.dimensionId().toString().equals(dimension)
				&& worldsEqual(waypoint.world, world));

		config.save();
		ClientFeedback.chat(client, "CoordKit saved a death waypoint. Press F9 to see it.");
	}

	private static Waypoint findNearby(CoordKitConfig config, Minecraft client, Vec3 position) {
		String dimension = client.level.dimension().identifier().toString();
		String currentWorld = WaypointScope.currentWorldKey(client);

		for (Waypoint waypoint : config.waypoints) {
			if (!waypoint.dimensionId().toString().equals(dimension)
					|| !WaypointScope.isVisibleIn(waypoint, currentWorld)) {
				continue;
			}

			double dx = waypoint.x - position.x;
			double dy = waypoint.y - position.y;
			double dz = waypoint.z - position.z;

			if (dx * dx + dy * dy + dz * dz <= DUPLICATE_RADIUS_SQR) {
				return waypoint;
			}
		}

		return null;
	}

	private static boolean worldsEqual(String first, String second) {
		return (first == null ? "" : first).equals(second == null ? "" : second);
	}
}
