package com.mackan.coordkit.client;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the pure world-scope matching rules and the migration of waypoints
 * saved by versions before 1.3.0.
 */
class WaypointScopeTest {
	@Test
	void globalWaypointsAreVisibleEverywhere() {
		assertTrue(WaypointScope.isVisibleIn("", "sp:my-world"));
		assertTrue(WaypointScope.isVisibleIn("", "mp:mc.example.com"));
		assertTrue(WaypointScope.isVisibleIn("", ""));
	}

	@Test
	void scopedWaypointsOnlyMatchTheirOwnWorld() {
		assertTrue(WaypointScope.isVisibleIn("sp:my-world", "sp:my-world"));
		assertFalse(WaypointScope.isVisibleIn("sp:my-world", "sp:other-world"));
		assertFalse(WaypointScope.isVisibleIn("mp:play.example.com", "sp:my-world"));
	}

	@Test
	void unknownWorldHidesScopedWaypoints() {
		assertFalse(WaypointScope.isVisibleIn("sp:my-world", ""));
		assertFalse(WaypointScope.isVisibleIn("sp:my-world", null));
		assertTrue(WaypointScope.isVisibleIn("", ""));
	}

	@Test
	void keysAreCaseInsensitiveAndIgnoreTheDefaultPort() {
		assertTrue(WaypointScope.isVisibleIn("SP:My-World", "sp:my-world"));
		assertTrue(WaypointScope.isVisibleIn("mp:play.example.com:25565", "mp:play.example.com"));
		assertTrue(WaypointScope.isVisibleIn("mp:play.example.com", "MP:PLAY.EXAMPLE.COM:25565"));
		assertFalse(WaypointScope.isVisibleIn("mp:play.example.com:25566", "mp:play.example.com:25565"));
	}

	@Test
	void missingWorldIsTreatedAsGlobal() {
		assertTrue(WaypointScope.isVisibleIn((String) null, "sp:my-world"));
		assertEquals("", WaypointScope.normalizeWorld(null));
		assertEquals("sp:my-world", WaypointScope.normalizeWorld("sp:my-world"));
	}

	@Test
	void singleplayerKeyUsesTheSaveFolderName() {
		// LevelResource.ROOT resolves "." against the save folder, so the raw
		// path ends in "Test\\." and must be normalised first.
		assertEquals("sp:test", WaypointScope.singleplayerKey(Path.of("saves", "Test", ".")));
		assertEquals("sp:test", WaypointScope.singleplayerKey(Path.of("saves", "Test")));
		assertEquals("sp:my-world", WaypointScope.singleplayerKey(Path.of("saves", "My-World", ".")));
		assertEquals("", WaypointScope.singleplayerKey(null));
		assertEquals("", WaypointScope.singleplayerKey(Path.of(".")));
	}

	@Test
	void oldWaypointJsonMigratesToGlobal() {
		String oldJson = "{\"name\":\"Base\",\"x\":1.0,\"y\":2.0,\"z\":3.0,"
				+ "\"dimension\":\"minecraft:overworld\",\"color\":4632774,\"visible\":true}";

		Waypoint waypoint = new Gson().fromJson(oldJson, Waypoint.class);

		assertEquals("Base", waypoint.name);
		assertEquals("", waypoint.world);
		assertFalse(waypoint.autoDeath);

		waypoint.world = WaypointScope.normalizeWorld(waypoint.world);

		assertEquals("", waypoint.world);
		assertTrue(WaypointScope.isVisibleIn(waypoint, "sp:any-world"));

		Waypoint nullWorld = new Gson().fromJson("{\"world\":null}", Waypoint.class);
		assertEquals("", WaypointScope.normalizeWorld(nullWorld.world));
	}
}
