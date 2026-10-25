package com.mackan.coordkit.client;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the flat group listing, suggestions, rename and delete rules.
 */
class WaypointGroupsTest {
	private static Waypoint waypoint(String name, String group) {
		Waypoint waypoint = new Waypoint(name, 0.0, 64.0, 0.0, "minecraft:overworld", 0x46D0C6);
		waypoint.group = group;
		return waypoint;
	}

	@Test
	void listsDistinctGroupsCaseInsensitivelyAndSorted() {
		List<Waypoint> waypoints = List.of(
				waypoint("a", "Home"),
				waypoint("b", "mining"),
				waypoint("c", "HOME"),
				waypoint("d", ""),
				waypoint("e", "  Base  ")
		);

		assertEquals(List.of("Base", "Home", "mining"), WaypointGroups.all(waypoints));
		assertEquals(List.of(), WaypointGroups.all(null));
	}

	@Test
	void countsMembersCaseInsensitively() {
		List<Waypoint> waypoints = List.of(
				waypoint("a", "Home"),
				waypoint("b", "home"),
				waypoint("c", "Mining")
		);

		assertEquals(2, WaypointGroups.count(waypoints, "HOME"));
		assertEquals(0, WaypointGroups.count(waypoints, "  "));
	}

	@Test
	void suggestionsMatchPrefixAndRespectTheLimit() {
		List<Waypoint> waypoints = List.of(
				waypoint("a", "Home"),
				waypoint("b", "Home Base"),
				waypoint("c", "Mining"),
				waypoint("d", "Holiday")
		);

		assertEquals(List.of("Holiday", "Home", "Home Base"),
				WaypointGroups.suggestions(waypoints, "ho", 10));
		assertEquals(List.of("Home Base"),
				WaypointGroups.suggestions(waypoints, "Home", 10));
		assertEquals(List.of("Holiday", "Home"),
				WaypointGroups.suggestions(waypoints, "", 2));
		assertEquals(List.of(), WaypointGroups.suggestions(waypoints, "", 0));
	}

	@Test
	void renameUpdatesEveryMemberAndPreservesTheEnteredCase() {
		Waypoint first = waypoint("a", "home");
		Waypoint second = waypoint("b", "Home");
		Waypoint other = waypoint("c", "Mining");
		List<Waypoint> waypoints = new ArrayList<>(List.of(first, second, other));

		assertEquals(2, WaypointGroups.rename(waypoints, "HOME", "Base Camp"));
		assertEquals("Base Camp", first.group);
		assertEquals("Base Camp", second.group);
		assertEquals("Mining", other.group);
		assertEquals(3, waypoints.size());
		assertSame(first, waypoints.get(0));
	}

	@Test
	void renameIntoAnExistingGroupMergesThem() {
		List<Waypoint> waypoints = List.of(waypoint("a", "Home"), waypoint("b", "Base"));

		assertEquals(1, WaypointGroups.rename(waypoints, "home", "BASE"));
		assertEquals(2, WaypointGroups.count(waypoints, "base"));
		assertEquals(List.of("BASE"), WaypointGroups.all(waypoints));
	}

	@Test
	void deleteClearsTheGroupButKeepsEveryWaypoint() {
		Waypoint first = waypoint("a", "Home");
		Waypoint second = waypoint("b", "home");
		Waypoint other = waypoint("c", "Mining");
		List<Waypoint> waypoints = new ArrayList<>(List.of(first, second, other));

		assertEquals(2, WaypointGroups.clear(waypoints, "HOME"));
		assertEquals("", first.group);
		assertEquals("", second.group);
		assertEquals("Mining", other.group);
		assertEquals(3, waypoints.size());
	}

	@Test
	void emptyNamesAreRejectedAndNullListsAreSafe() {
		List<Waypoint> waypoints = List.of(waypoint("a", "Home"));

		assertEquals(0, WaypointGroups.rename(waypoints, "Home", "   "));
		assertEquals("Home", waypoints.get(0).group);
		assertEquals(0, WaypointGroups.rename(null, "Home", "Base"));
		assertEquals(0, WaypointGroups.clear(null, "Home"));
		assertEquals(0, WaypointGroups.clear(waypoints, ""));
		assertTrue(WaypointGroups.all(waypoints).contains("Home"));
	}
}
