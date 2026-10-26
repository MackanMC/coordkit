package com.mackan.coordkit.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests config defaults and the migration from the pre-1.4.0 format.
 */
class CoordKitConfigTest {
	@Test
	void legacyConfigIsUpgradedToVersionTwo() {
		JsonObject json = JsonParser.parseString("""
				{
				  "showHud": false,
				  "waypoints": [
				    {
				      "name": "Base",
				      "x": 1.0,
				      "y": 2.0,
				      "z": 3.0,
				      "dimension": "minecraft:overworld",
				      "visible": true,
				      "world": "SP:My-World"
				    }
				  ]
				}
				""").getAsJsonObject();

		CoordKitConfig config = CoordKitConfig.fromJson(json);

		assertNotNull(config);
		assertEquals(2, config.configVersion);
		assertFalse(config.showHud);
		assertTrue(config.scopeNewWaypointsToWorld);
		assertTrue(config.createDeathWaypoint);
		assertEquals("sp:my-world", config.waypoints.get(0).world);
		assertEquals("", config.waypoints.get(0).group);
	}

	@Test
	void zeroVersionIsTreatedAsLegacy() {
		JsonObject json = JsonParser.parseString("{\"configVersion\":0}").getAsJsonObject();

		CoordKitConfig config = CoordKitConfig.fromJson(json);

		assertNotNull(config);
		assertEquals(2, config.configVersion);
	}

	@Test
	void futureConfigVersionIsPreserved() {
		JsonObject json = JsonParser.parseString("{\"configVersion\":3}").getAsJsonObject();

		CoordKitConfig config = CoordKitConfig.fromJson(json);

		assertNotNull(config);
		assertEquals(3, config.configVersion);
	}

	@Test
	void versionTwoConfigRoundTripsGroups() {
		JsonObject json = JsonParser.parseString("""
				{
				  "configVersion": 2,
				  "waypoints": [
				    {
				      "name": "Base",
				      "x": 1.0,
				      "y": 2.0,
				      "z": 3.0,
				      "dimension": "minecraft:overworld",
				      "group": "  Home  "
				    },
				    {
				      "name": "Shaft",
				      "group": null
				    }
				  ]
				}
				""").getAsJsonObject();

		CoordKitConfig config = CoordKitConfig.fromJson(json);

		assertNotNull(config);
		assertEquals("Home", config.waypoints.get(0).group);
		assertEquals("", config.waypoints.get(1).group);

		JsonObject saved = JsonParser.parseString(CoordKitConfig.toJson(config)).getAsJsonObject();

		assertEquals(2, saved.get("configVersion").getAsInt());
		assertEquals("Home", saved.getAsJsonArray("waypoints").get(0).getAsJsonObject().get("group").getAsString());
		assertEquals("", saved.getAsJsonArray("waypoints").get(1).getAsJsonObject().get("group").getAsString());
	}

	@Test
	void hudTogglesDefaultToVisibleForOlderConfigs() {
		JsonObject json = JsonParser.parseString("{\"configVersion\":2}").getAsJsonObject();

		CoordKitConfig config = CoordKitConfig.fromJson(json);

		assertNotNull(config);
		assertTrue(config.showCoordinates);
		assertTrue(config.showDimension);
		assertFalse(config.showSkyLight);
		assertFalse(config.showBlockLight);
	}

	@Test
	void legacyCombinedLightSettingSplitsIntoBothLayers() {
		JsonObject json = JsonParser.parseString("{\"configVersion\":2,\"showLight\":true}").getAsJsonObject();

		CoordKitConfig config = CoordKitConfig.fromJson(json);

		assertNotNull(config);
		assertTrue(config.showSkyLight);
		assertTrue(config.showBlockLight);
	}

	@Test
	void splitLightSettingsArePreserved() {
		JsonObject json = JsonParser.parseString("""
				{
				  "configVersion": 2,
				  "showLight": true,
				  "showSkyLight": true,
				  "showBlockLight": false
				}
				""").getAsJsonObject();

		CoordKitConfig config = CoordKitConfig.fromJson(json);

		assertNotNull(config);
		assertTrue(config.showSkyLight);
		assertFalse(config.showBlockLight);

		JsonObject saved = JsonParser.parseString(CoordKitConfig.toJson(config)).getAsJsonObject();

		assertTrue(saved.get("showSkyLight").getAsBoolean());
		assertFalse(saved.get("showBlockLight").getAsBoolean());
	}

	@Test
	void missingWaypointsListBecomesEmpty() {
		JsonObject json = JsonParser.parseString("{\"configVersion\":2}").getAsJsonObject();

		CoordKitConfig config = CoordKitConfig.fromJson(json);

		assertNotNull(config);
		assertTrue(config.waypoints.isEmpty());
	}
}
