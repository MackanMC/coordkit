package com.mackan.coordkit.client;

import com.mackan.coordkit.client.WaypointQuery.ExportSet;
import com.mackan.coordkit.client.WaypointQuery.GroupFilter;
import com.mackan.coordkit.client.WaypointQuery.Position;
import com.mackan.coordkit.client.WaypointQuery.Scope;
import com.mackan.coordkit.client.WaypointQuery.Sort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the manager's in-memory search, filter and sort combinations.
 */
class WaypointQueryTest {
	private static Waypoint waypoint(String name, String group, String world, double x, double z) {
		Waypoint waypoint = new Waypoint(name, x, 64.0, z, "minecraft:overworld", 0x46D0C6);
		waypoint.group = group;
		waypoint.world = world;
		return waypoint;
	}

	private static List<Waypoint> sample() {
		return List.of(
				waypoint("Base Camp", "Home", "sp:test", 10.0, 0.0),
				waypoint("Shaft", "Mining", "sp:test", 100.0, 0.0),
				waypoint("Portal", "", "sp:test", 50.0, 0.0),
				waypoint("Trading Hall", "Home", "", 0.0, 20.0),
				waypoint("Old Base", "Home", "sp:other", 0.0, 0.0)
		);
	}

	private static List<Waypoint> filter(List<Waypoint> waypoints, String query, Scope scope,
			GroupFilter groupFilter, String group, Sort sort, Position position) {
		return WaypointQuery.filter(waypoints, query, scope, "sp:test", groupFilter, group, sort, position);
	}

	private static List<String> names(List<Waypoint> waypoints) {
		return waypoints.stream().map(waypoint -> waypoint.name).toList();
	}

	@Test
	void searchMatchesNameAndGroupCaseInsensitively() {
		List<Waypoint> sample = sample();

		assertEquals(List.of("Base Camp", "Old Base"), names(filter(sample, "BASE", Scope.ALL_WORLDS,
				GroupFilter.ALL, "", Sort.NAME, null)));
		assertEquals(List.of("Base Camp", "Old Base", "Trading Hall"),
				names(filter(sample, "home", Scope.ALL_WORLDS, GroupFilter.ALL, "", Sort.NAME, null)));
		assertEquals(5, filter(sample, "   ", Scope.ALL_WORLDS, GroupFilter.ALL, "", Sort.NAME, null).size());
	}

	@Test
	void visibleHereIncludesGlobalAndExcludesOtherWorlds() {
		List<Waypoint> sample = sample();

		assertEquals(List.of("Base Camp", "Portal", "Shaft", "Trading Hall"),
				names(filter(sample, "", Scope.VISIBLE_HERE, GroupFilter.ALL, "", Sort.NAME, null)));

		assertEquals(5, filter(sample, "", Scope.ALL_WORLDS, GroupFilter.ALL, "", Sort.NAME, null).size());
	}

	@Test
	void visibleHereOnlyMatchesGlobalWhenTheWorldIsUnknown() {
		List<Waypoint> sample = sample();

		List<Waypoint> visible = WaypointQuery.filter(sample, "", Scope.VISIBLE_HERE, "",
				GroupFilter.ALL, "", Sort.NAME, null);

		assertEquals(List.of("Trading Hall"), names(visible));
	}

	@Test
	void groupFiltersCoverAllUngroupedAndSpecificGroups() {
		List<Waypoint> sample = sample();

		assertEquals(List.of("Portal"), names(filter(sample, "", Scope.ALL_WORLDS,
				GroupFilter.UNGROUPED, "", Sort.NAME, null)));
		assertEquals(List.of("Base Camp", "Old Base", "Trading Hall"),
				names(filter(sample, "", Scope.ALL_WORLDS, GroupFilter.SPECIFIC, "HOME", Sort.NAME, null)));
	}

	@Test
	void sortsNamesCaseInsensitively() {
		List<Waypoint> waypoints = List.of(
				waypoint("beta", "", "", 0.0, 0.0),
				waypoint("Alpha", "", "", 0.0, 0.0),
				waypoint("gamma", "", "", 0.0, 0.0)
		);

		assertEquals(List.of("Alpha", "beta", "gamma"), names(filter(waypoints, "", Scope.ALL_WORLDS,
				GroupFilter.ALL, "", Sort.NAME, null)));
	}

	@Test
	void sortsGroupThenName() {
		List<Waypoint> waypoints = List.of(
				waypoint("Zeta", "", "", 0.0, 0.0),
				waypoint("Beta", "Home", "", 0.0, 0.0),
				waypoint("Alpha", "Home", "", 0.0, 0.0),
				waypoint("Gamma", "Mining", "", 0.0, 0.0)
		);

		assertEquals(List.of("Zeta", "Alpha", "Beta", "Gamma"), names(filter(waypoints, "", Scope.ALL_WORLDS,
				GroupFilter.ALL, "", Sort.GROUP, null)));
	}

	@Test
	void sortsByDistanceAndFallsBackToNameWithoutAPlayer() {
		List<Waypoint> waypoints = List.of(
				waypoint("Far", "", "", 100.0, 0.0),
				waypoint("Near", "", "", 10.0, 0.0),
				waypoint("Middle", "", "", 50.0, 0.0)
		);

		assertEquals(List.of("Near", "Middle", "Far"), names(filter(waypoints, "", Scope.ALL_WORLDS,
				GroupFilter.ALL, "", Sort.DISTANCE, new Position(0.0, 64.0, 0.0))));
		assertEquals(List.of("Far", "Middle", "Near"), names(filter(waypoints, "", Scope.ALL_WORLDS,
				GroupFilter.ALL, "", Sort.DISTANCE, null)));
	}

	@Test
	void combinedFiltersNeverLoseOrDuplicateEntries() {
		List<Waypoint> sample = sample();

		List<Waypoint> result = filter(sample, "home", Scope.VISIBLE_HERE,
				GroupFilter.SPECIFIC, "home", Sort.GROUP, null);

		assertEquals(List.of("Base Camp", "Trading Hall"), names(result));
		assertEquals(result.size(), result.stream().distinct().count());
	}

	@Test
	void exportSetsSelectAllVisibleHereOrEnabledOnly() {
		Waypoint global = waypoint("Global", "", "", 0.0, 0.0);
		Waypoint local = waypoint("Local", "", "sp:test", 0.0, 0.0);
		Waypoint localHidden = waypoint("Hidden", "", "sp:test", 0.0, 0.0);
		Waypoint other = waypoint("Other", "", "sp:other", 0.0, 0.0);
		Waypoint otherHidden = waypoint("Other Hidden", "", "sp:other", 0.0, 0.0);
		localHidden.visible = false;
		otherHidden.visible = false;
		List<Waypoint> all = List.of(global, local, localHidden, other, otherHidden);

		assertEquals(5, WaypointQuery.exportSet(all, ExportSet.ALL, "sp:test").size());
		assertEquals(List.of("Global", "Local", "Hidden"),
				names(WaypointQuery.exportSet(all, ExportSet.VISIBLE_HERE, "sp:test")));
		assertEquals(List.of("Global"),
				names(WaypointQuery.exportSet(all, ExportSet.VISIBLE_HERE, "")));
		assertEquals(List.of("Global", "Local", "Other"),
				names(WaypointQuery.exportSet(all, ExportSet.VISIBLE_ONLY, "sp:test")));
		assertTrue(WaypointQuery.exportSet(null, ExportSet.ALL, "sp:test").isEmpty());
	}

	@Test
	void filteringDoesNotMutateTheInputOrReturnNullEntries() {
		List<Waypoint> original = new ArrayList<>(sample());
		original.add(null);
		List<Waypoint> snapshot = new ArrayList<>(original);

		List<Waypoint> result = WaypointQuery.filter(original, "", Scope.ALL_WORLDS, "sp:test",
				GroupFilter.ALL, "", Sort.NAME, null);

		assertEquals(snapshot, original);
		assertTrue(result.stream().allMatch(waypoint -> waypoint != null));
		assertEquals(5, result.size());
		assertSame(snapshot.get(0), result.get(0));
	}
}
