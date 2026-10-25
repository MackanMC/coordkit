package com.mackan.coordkit.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Pure search, filter and sort rules for the waypoint manager.
 *
 * <p>Kept independent of screens and the config so the combinations can be
 * tested without a running client. The manager only supplies its in-memory
 * filter state.</p>
 */
public final class WaypointQuery {
	/**
	 * How the world scope is treated.
	 */
	public enum Scope {
		/** Only waypoints visible in the current world, including global ones. */
		VISIBLE_HERE,
		/** Waypoints from every world. */
		ALL_WORLDS
	}

	/**
	 * How the flat group field is filtered.
	 */
	public enum GroupFilter {
		ALL,
		UNGROUPED,
		SPECIFIC
	}

	public enum Sort {
		NAME,
		GROUP,
		DISTANCE
	}

	/**
	 * The waypoint sets offered by the export screen.
	 */
	public enum ExportSet {
		/** Every waypoint, including hidden entries and other worlds. */
		ALL,
		/** Waypoints visible in the current world, including global ones. */
		VISIBLE_HERE,
		/** Waypoints with their own visibility toggle enabled. */
		VISIBLE_ONLY
	}

	/**
	 * The player position used for distance sorting. {@code null} means the
	 * position is unknown, so distance sorting falls back to name order.
	 */
	public record Position(double x, double y, double z) {
	}

	private WaypointQuery() {
	}

	/**
	 * Applies the query to a copy of the list. Entries are never modified, so
	 * filtering cannot lose or duplicate config entries.
	 */
	public static List<Waypoint> filter(List<Waypoint> waypoints, String query, Scope scope,
			String currentWorldKey, GroupFilter groupFilter, String groupValue, Sort sort,
			Position position) {
		List<Waypoint> result = new ArrayList<>();
		String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		Scope scopeMode = scope == null ? Scope.ALL_WORLDS : scope;
		GroupFilter groupMode = groupFilter == null ? GroupFilter.ALL : groupFilter;
		String groupNeedle = groupValue == null ? "" : groupValue.trim();

		if (waypoints != null) {
			for (Waypoint waypoint : waypoints) {
				if (waypoint == null) {
					continue;
				}

				if (scopeMode == Scope.VISIBLE_HERE
						&& !WaypointScope.isVisibleIn(waypoint, currentWorldKey)) {
					continue;
				}

				String group = Waypoint.normalizeGroup(waypoint.group);

				if (!needle.isEmpty()
						&& !lower(waypoint.name).contains(needle)
						&& !lower(group).contains(needle)) {
					continue;
				}

				if (groupMode == GroupFilter.UNGROUPED && !group.isEmpty()) {
					continue;
				}

				if (groupMode == GroupFilter.SPECIFIC && !group.equalsIgnoreCase(groupNeedle)) {
					continue;
				}

				result.add(waypoint);
			}
		}

		result.sort(comparator(sort, position));
		return result;
	}

	/**
	 * Selects one of the export sets without modifying the source list.
	 */
	public static List<Waypoint> exportSet(List<Waypoint> waypoints, ExportSet set, String currentWorldKey) {
		List<Waypoint> result = new ArrayList<>();
		ExportSet mode = set == null ? ExportSet.ALL : set;

		if (waypoints == null) {
			return result;
		}

		for (Waypoint waypoint : waypoints) {
			if (waypoint == null) {
				continue;
			}

			boolean include = switch (mode) {
				case ALL -> true;
				case VISIBLE_HERE -> WaypointScope.isVisibleIn(waypoint, currentWorldKey);
				case VISIBLE_ONLY -> waypoint.visible;
			};

			if (include) {
				result.add(waypoint);
			}
		}

		return result;
	}

	static Comparator<Waypoint> comparator(Sort sort, Position position) {
		Comparator<Waypoint> byName = Comparator
				.comparing((Waypoint waypoint) -> lower(waypoint.name))
				.thenComparing(waypoint -> lower(waypoint.group))
				.thenComparingDouble(waypoint -> waypoint.x)
				.thenComparingDouble(waypoint -> waypoint.y)
				.thenComparingDouble(waypoint -> waypoint.z);

		if (sort == Sort.GROUP) {
			return Comparator
					.comparing((Waypoint waypoint) -> lower(waypoint.group))
					.thenComparing(waypoint -> lower(waypoint.name))
					.thenComparingDouble(waypoint -> waypoint.x)
					.thenComparingDouble(waypoint -> waypoint.y)
					.thenComparingDouble(waypoint -> waypoint.z);
		}

		if (sort == Sort.DISTANCE && position != null) {
			return Comparator
					.comparingDouble((Waypoint waypoint) -> waypoint.distanceTo(position.x(), position.y(), position.z()))
					.thenComparing(byName);
		}

		return byName;
	}

	private static String lower(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}
}
