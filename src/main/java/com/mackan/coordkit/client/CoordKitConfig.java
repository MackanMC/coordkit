package com.mackan.coordkit.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mackan.coordkit.CoordKit;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tiny JSON config, stored at {@code config/coordkit.json}.
 */
public final class CoordKitConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(CoordKit.MOD_ID + ".json");

	@Nullable
	private static CoordKitConfig instance;

	public boolean showHud = true;
	public boolean showFps = true;
	public boolean showClock = true;
	public boolean showChunk = true;

	public boolean showWaypoints = true;
	public int maxWaypoints = 5;

	public boolean showFacing = true;
	public boolean showBiome = true;
	public boolean showGameTime = true;
	public boolean showLight = false;
	public boolean showSpeed = false;
	public boolean showDurability = true;

	public boolean showMarkers = true;
	public boolean markerOffscreenArrows = true;
	public int markerMaxDistance = 512;

	public boolean scopeNewWaypointsToWorld = true;
	public boolean createDeathWaypoint = true;

	public List<Waypoint> waypoints = new ArrayList<>();

	public static CoordKitConfig get() {
		if (instance == null) {
			instance = load();
		}

		return instance;
	}

	private static CoordKitConfig load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH)) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				CoordKitConfig loaded = GSON.fromJson(json, CoordKitConfig.class);

				if (loaded != null) {
					if (loaded.waypoints == null) {
						loaded.waypoints = new ArrayList<>();
					}

					// Config fields added after 1.2.0 must default to enabled
					// when they are missing from an older file.
					if (!json.has("scopeNewWaypointsToWorld")) {
						loaded.scopeNewWaypointsToWorld = true;
					}

					if (!json.has("createDeathWaypoint")) {
						loaded.createDeathWaypoint = true;
					}

					for (Waypoint waypoint : loaded.waypoints) {
						waypoint.world = WaypointScope.normalizeWorld(waypoint.world);
					}

					loaded.maxWaypoints = Math.max(1, Math.min(10, loaded.maxWaypoints));
					loaded.markerMaxDistance = Math.max(32, Math.min(4096, loaded.markerMaxDistance));
					return loaded;
				}
			} catch (Exception e) {
				CoordKit.LOGGER.warn("Could not read {}, falling back to defaults", PATH, e);
			}
		}

		return new CoordKitConfig();
	}

	public void save() {
		try {
			Files.createDirectories(PATH.getParent());

			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(this, writer);
			}
		} catch (Exception e) {
			CoordKit.LOGGER.warn("Could not write {}", PATH, e);
		}
	}
}
