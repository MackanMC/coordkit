package com.mackan.coordkit.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pure helpers for the flat, case-insensitive waypoint groups.
 *
 * <p>Groups are stored per waypoint, so renaming or deleting one is a bulk
 * edit over the list rather than a separate registry. These helpers keep that
 * logic testable and make sure waypoints themselves are never removed.</p>
 */
public final class WaypointGroups {
	private WaypointGroups() {
	}

	/**
	 * Distinct group names, sorted case-insensitively. The first spelling that
	 * appears in the list is kept.
	 */
	public static List<String> all(List<Waypoint> waypoints) {
		Map<String, String> groups = new LinkedHashMap<>();
		List<String> sorted = new ArrayList<>();

		if (waypoints != null) {
			for (Waypoint waypoint : waypoints) {
				if (waypoint == null) {
					continue;
				}

				String group = Waypoint.normalizeGroup(waypoint.group);

				if (!group.isEmpty()) {
					groups.putIfAbsent(lower(group), group);
				}
			}
		}

		sorted.addAll(groups.values());
		sorted.sort((first, second) -> lower(first).compareTo(lower(second)));
		return sorted;
	}

	/**
	 * Number of waypoints in a group, matched case-insensitively.
	 */
	public static int count(List<Waypoint> waypoints, String group) {
		String needle = Waypoint.normalizeGroup(group);

		if (needle.isEmpty() || waypoints == null) {
			return 0;
		}

		int count = 0;

		for (Waypoint waypoint : waypoints) {
			if (waypoint != null && needle.equalsIgnoreCase(Waypoint.normalizeGroup(waypoint.group))) {
				count++;
			}
		}

		return count;
	}

	/**
	 * Existing groups that start with {@code prefix}, excluding an exact match
	 * so the editor does not suggest the value already in the field.
	 */
	public static List<String> suggestions(List<Waypoint> waypoints, String prefix, int limit) {
		String needle = lower(Waypoint.normalizeGroup(prefix));
		List<String> result = new ArrayList<>();

		if (limit <= 0) {
			return result;
		}

		for (String group : all(waypoints)) {
			if (group.equalsIgnoreCase(needle)) {
				continue;
			}

			if (needle.isEmpty() || lower(group).startsWith(needle)) {
				result.add(group);

				if (result.size() >= limit) {
					break;
				}
			}
		}

		return result;
	}

	/**
	 * Sets every member of {@code from} to {@code to}, preserving the entered
	 * case. Returns how many waypoints changed. An empty new name is rejected
	 * because clearing a group is a separate action.
	 */
	public static int rename(List<Waypoint> waypoints, String from, String to) {
		String source = Waypoint.normalizeGroup(from);
		String target = Waypoint.normalizeGroup(to);

		if (waypoints == null || source.isEmpty() || target.isEmpty()) {
			return 0;
		}

		int changed = 0;

		for (Waypoint waypoint : waypoints) {
			if (waypoint != null && source.equalsIgnoreCase(Waypoint.normalizeGroup(waypoint.group))) {
				waypoint.group = target;
				changed++;
			}
		}

		return changed;
	}

	/**
	 * Clears the group label on every member of {@code group}. The waypoints
	 * stay in the list. Returns how many were changed.
	 */
	public static int clear(List<Waypoint> waypoints, String group) {
		String needle = Waypoint.normalizeGroup(group);

		if (waypoints == null || needle.isEmpty()) {
			return 0;
		}

		int changed = 0;

		for (Waypoint waypoint : waypoints) {
			if (waypoint != null && needle.equalsIgnoreCase(Waypoint.normalizeGroup(waypoint.group))) {
				waypoint.group = "";
				changed++;
			}
		}

		return changed;
	}

	private static String lower(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}
}
