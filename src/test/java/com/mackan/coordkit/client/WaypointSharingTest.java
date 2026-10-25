package com.mackan.coordkit.client;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the 1.4.0 JSON envelope and CK1 share-code formats.
 */
class WaypointSharingTest {
	private static Waypoint waypoint(String name, double x, double y, double z, String world) {
		Waypoint waypoint = new Waypoint(name, x, y, z, "minecraft:overworld", 0x123456);
		waypoint.world = world;
		return waypoint;
	}

	private static String envelope(String waypointsJson) {
		return """
				{
				  "format": "coordkit.waypoints",
				  "version": 1,
				  "waypoints": [
				%s
				  ]
				}
				""".formatted(waypointsJson);
	}

	@Test
	void jsonRoundTripKeepsEveryField() {
		Waypoint original = waypoint("Base; 100% \u2713", 1.25, 64.0, -30.5, "sp:my-world");
		original.color = 0x654321;
		original.visible = false;
		original.group = "Home";
		original.autoDeath = true;

		String json = WaypointSharing.exportJson(List.of(original));
		WaypointSharing.ImportPlan plan = WaypointSharing.importText(
				json, List.of(), "sp:test", WaypointSharing.ScopePolicy.KEEP);

		assertNull(plan.error());
		assertTrue(plan.canImport());
		assertEquals(1, plan.validCount());

		Waypoint imported = plan.valid().get(0);
		assertEquals("Base; 100% \u2713", imported.name);
		assertEquals(1.25, imported.x);
		assertEquals(64.0, imported.y);
		assertEquals(-30.5, imported.z);
		assertEquals("minecraft:overworld", imported.dimension);
		assertEquals(0x654321, imported.color);
		assertEquals("sp:my-world", imported.world);
		assertEquals("Home", imported.group);
		assertFalse(imported.visible);
		assertFalse(imported.autoDeath);
	}

	@Test
	void shareCodeRoundTripsSpecialCharacters() {
		Waypoint original = waypoint("A% B;C\u00e9", 120.5, 64.0, -30.25, "sp:my-world");
		original.visible = false;

		String code = WaypointSharing.shareCode(original);

		assertEquals(9, code.split(";", -1).length);
		assertTrue(code.startsWith("CK1;"));

		WaypointSharing.ImportPlan plan = WaypointSharing.importText(
				code, List.of(), "", WaypointSharing.ScopePolicy.KEEP);

		assertTrue(plan.canImport());
		assertEquals(1, plan.validCount());

		Waypoint imported = plan.valid().get(0);
		assertEquals("A% B;C\u00e9", imported.name);
		assertEquals(120.5, imported.x);
		assertEquals(64.0, imported.y);
		assertEquals(-30.25, imported.z);
		assertEquals("sp:my-world", imported.world);
		assertEquals("", imported.group);
		assertFalse(imported.visible);
		assertFalse(imported.autoDeath);
	}

	@Test
	void shareCodeSupportsEmptyWorldsAndDefaultPorts() {
		Waypoint global = waypoint("Spawn", 0.0, 64.0, 0.0, "");
		Waypoint server = waypoint("Portal", 10.0, 64.0, 10.0, "mp:play.example.com:25565");

		WaypointSharing.ImportPlan plan = WaypointSharing.importText(
				WaypointSharing.exportShareCodes(List.of(global, server)),
				List.of(), "", WaypointSharing.ScopePolicy.KEEP);

		assertEquals(2, plan.validCount());
		assertEquals("", plan.valid().get(0).world);
		assertEquals("mp:play.example.com", plan.valid().get(1).world);
	}

	@Test
	void invalidShareCodeLinesDoNotBlockValidOnes() {
		String valid = WaypointSharing.shareCode(waypoint("Base", 1.0, 2.0, 3.0, ""));
		String text = "hello\n" + valid + "\nCK1;too;few;fields";

		WaypointSharing.ImportPlan plan = WaypointSharing.importText(
				text, List.of(), "", WaypointSharing.ScopePolicy.KEEP);

		assertNull(plan.error());
		assertTrue(plan.canImport());
		assertEquals(1, plan.validCount());
		assertEquals(2, plan.invalidCount());
		assertEquals(1, plan.waypointsToAdd().size());
	}

	@Test
	void jsonIsAllOrNothing() {
		String json = envelope("""
				    {"name":"Good","x":1.0,"y":2.0,"z":3.0,"dimension":"minecraft:overworld"},
				    {"name":"Bad","x":1.0,"y":2.0,"z":3.0,"dimension":"not a dimension!"}
				""");

		WaypointSharing.ImportPlan plan = WaypointSharing.importText(
				json, List.of(), "", WaypointSharing.ScopePolicy.KEEP);

		assertNull(plan.error());
		assertEquals(1, plan.validCount());
		assertEquals(1, plan.invalidCount());
		assertFalse(plan.canImport());
		assertTrue(plan.waypointsToAdd().isEmpty());
	}

	@Test
	void rejectsWrongEnvelopeMetadata() {
		assertNotNull(WaypointSharing.importText("{not json", List.of(), "", WaypointSharing.ScopePolicy.KEEP).error());
		assertNotNull(WaypointSharing.importText("{}", List.of(), "", WaypointSharing.ScopePolicy.KEEP).error());
		assertNotNull(WaypointSharing.importText(
				"{\"format\":\"other\",\"version\":1,\"waypoints\":[]}",
				List.of(), "", WaypointSharing.ScopePolicy.KEEP).error());
		assertNotNull(WaypointSharing.importText(
				"{\"format\":\"coordkit.waypoints\",\"version\":2,\"waypoints\":[]}",
				List.of(), "", WaypointSharing.ScopePolicy.KEEP).error());
		assertNotNull(WaypointSharing.importText(
				"{\"format\":\"coordkit.waypoints\",\"version\":1}",
				List.of(), "", WaypointSharing.ScopePolicy.KEEP).error());
		assertNotNull(WaypointSharing.importText(
				"{\"format\":\"coordkit.waypoints\",\"version\":1,\"waypoints\":[]}",
				List.of(), "", WaypointSharing.ScopePolicy.KEEP).error());
	}

	@Test
	void rejectsMoreThanFiveHundredEntries() {
		List<Waypoint> many = new ArrayList<>();

		for (int i = 0; i < WaypointSharing.MAX_IMPORT + 1; i++) {
			many.add(waypoint("W" + i, i, 64.0, 0.0, ""));
		}

		String json = WaypointSharing.exportJson(many);
		WaypointSharing.ImportPlan plan = WaypointSharing.importText(
				json, List.of(), "", WaypointSharing.ScopePolicy.KEEP);

		assertNotNull(plan.error());
		assertTrue(plan.error().contains(Integer.toString(WaypointSharing.MAX_IMPORT)));
	}

	@Test
	void validatesCoordinatesDimensionsAndWorlds() {
		String json = envelope("""
				    {"name":"Far","x":30000001.0,"y":2.0,"z":3.0,"dimension":"minecraft:overworld"},
				    {"name":"Infinite","x":1e309,"y":2.0,"z":3.0,"dimension":"minecraft:overworld"},
				    {"name":"Dimension","x":1.0,"y":2.0,"z":3.0,"dimension":"bad dimension!"},
				    {"name":"World","x":1.0,"y":2.0,"z":3.0,"world":"wat"}
				""");

		WaypointSharing.ImportPlan plan = WaypointSharing.importText(
				json, List.of(), "", WaypointSharing.ScopePolicy.KEEP);

		assertEquals(0, plan.validCount());
		assertEquals(4, plan.invalidCount());
		assertFalse(plan.canImport());
	}

	@Test
	void normalisesNameGroupColourAndDeathFlag() {
		String longName = "N".repeat(Waypoint.MAX_NAME_LENGTH + 8);
		String longGroup = "G".repeat(Waypoint.MAX_GROUP_LENGTH + 8);
		String json = envelope("""
				    {"name":"%s","group":"%s","x":1.0,"y":2.0,"z":3.0,
				     "color":-1,"autoDeath":true}
				""".formatted(longName, longGroup));

		WaypointSharing.ImportPlan plan = WaypointSharing.importText(
				json, List.of(), "", WaypointSharing.ScopePolicy.KEEP);

		assertTrue(plan.canImport());
		Waypoint imported = plan.valid().get(0);
		assertEquals(Waypoint.MAX_NAME_LENGTH, imported.name.length());
		assertEquals(Waypoint.MAX_GROUP_LENGTH, imported.group.length());
		assertEquals(0xFFFFFF, imported.color);
		assertFalse(imported.autoDeath);
	}

	@Test
	void emptyNamesFallBackToWaypoint() {
		String json = envelope("""
				    {"name":"   ","x":1.0,"y":2.0,"z":3.0}
				""");

		WaypointSharing.ImportPlan plan = WaypointSharing.importText(
				json, List.of(), "", WaypointSharing.ScopePolicy.KEEP);

		assertEquals("Waypoint", plan.valid().get(0).name);
	}

	@Test
	void skipsDuplicatesAgainstExistingAndEarlierEntries() {
		Waypoint existing = waypoint("Base", 10.0, 64.0, 20.0, "sp:test");
		List<Waypoint> existingList = List.of(existing);
		String code = WaypointSharing.shareCode(waypoint(" base ", 10.004, 64.0, 20.0, "sp:test"));
		String other = WaypointSharing.shareCode(waypoint("Other", 1.0, 2.0, 3.0, "sp:test"));
		String duplicateOfOther = WaypointSharing.shareCode(waypoint("OTHER", 1.001, 2.0, 3.0, "sp:test"));

		WaypointSharing.ImportPlan plan = WaypointSharing.importText(
				code + "\n" + other + "\n" + duplicateOfOther,
				existingList, "sp:test", WaypointSharing.ScopePolicy.KEEP);

		assertEquals(1, plan.validCount());
		assertEquals(2, plan.duplicateCount());
		assertEquals(0, plan.invalidCount());
		assertTrue(plan.canImport());
	}

	@Test
	void appliesEachScopePolicy() {
		String code = WaypointSharing.shareCode(waypoint("Base", 1.0, 2.0, 3.0, "sp:original"));

		Waypoint kept = WaypointSharing.importText(
				code, List.of(), "sp:current", WaypointSharing.ScopePolicy.KEEP).valid().get(0);
		Waypoint current = WaypointSharing.importText(
				code, List.of(), "sp:current", WaypointSharing.ScopePolicy.CURRENT_WORLD).valid().get(0);
		Waypoint global = WaypointSharing.importText(
				code, List.of(), "sp:current", WaypointSharing.ScopePolicy.GLOBAL).valid().get(0);

		assertEquals("sp:original", kept.world);
		assertEquals("sp:current", current.world);
		assertEquals("", global.world);
	}

	@Test
	void currentWorldPolicyIsRejectedWhenTheWorldIsUnknown() {
		String code = WaypointSharing.shareCode(waypoint("Base", 1.0, 2.0, 3.0, "sp:original"));

		WaypointSharing.ImportPlan plan = WaypointSharing.importText(
				code, List.of(), "", WaypointSharing.ScopePolicy.CURRENT_WORLD);

		assertEquals(1, plan.invalidCount());
		assertFalse(plan.canImport());
		assertTrue(plan.invalid().get(0).reason().contains("unknown"));
	}

	@Test
	void duplicateDetectionUsesThePolicyScope() {
		Waypoint existing = waypoint("Base", 1.0, 2.0, 3.0, "sp:current");
		String code = WaypointSharing.shareCode(waypoint("Base", 1.0, 2.0, 3.0, "sp:original"));

		WaypointSharing.ImportPlan plan = WaypointSharing.importText(
				code, List.of(existing), "sp:current", WaypointSharing.ScopePolicy.CURRENT_WORLD);

		assertEquals(1, plan.duplicateCount());
		assertFalse(plan.canImport());
	}

	@Test
	void emptyInputIsAnError() {
		assertNotNull(WaypointSharing.importText("", List.of(), "", WaypointSharing.ScopePolicy.KEEP).error());
		assertNotNull(WaypointSharing.importText("   \n  ", List.of(), "", WaypointSharing.ScopePolicy.KEEP).error());
	}

	@Test
	void stripsAByteOrderMark() {
		String code = WaypointSharing.shareCode(waypoint("Base", 1.0, 2.0, 3.0, ""));

		WaypointSharing.ImportPlan plan = WaypointSharing.importText(
				"\uFEFF" + code, List.of(), "", WaypointSharing.ScopePolicy.KEEP);

		assertTrue(plan.canImport());
		assertEquals("Base", plan.valid().get(0).name);
	}
}
