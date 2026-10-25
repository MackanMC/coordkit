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

	@Nullable
	private static CoordKitConfig instance;

	public int configVersion = 2;

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

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(CoordKit.MOD_ID + ".json");
	}

	private static CoordKitConfig load() {
		Path path = path();

		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				CoordKitConfig loaded = fromJson(json);

				if (loaded != null) {
					return loaded;
				}
			} catch (Exception e) {
				CoordKit.LOGGER.warn("Could not read {}, falling back to defaults", path, e);
			}
		}

		return new CoordKitConfig();
	}

	/**
	 * Applies defaults and migrations to a parsed config. Package-private so the
	 * migration rules can be tested without touching the filesystem.
	 */
	@Nullable
	static CoordKitConfig fromJson(JsonObject json) {
		CoordKitConfig loaded = GSON.fromJson(json, CoordKitConfig.class);

		if (loaded == null) {
			return null;
		}

		if (loaded.waypoints == null) {
			loaded.waypoints = new ArrayList<>();
		}

		// Config fields added after 1.2.0 must default to enabled when they are
		// missing from an older file.
		if (!json.has("scopeNewWaypointsToWorld")) {
			loaded.scopeNewWaypointsToWorld = true;
		}

		if (!json.has("createDeathWaypoint")) {
			loaded.createDeathWaypoint = true;
		}

		// Missing or zero means the file predates 1.4.0. Do not downgrade a
		// config written by a newer version.
		if (loaded.configVersion <= 0) {
			loaded.configVersion = 2;
		}

		for (Waypoint waypoint : loaded.waypoints) {
			waypoint.world = WaypointScope.normalizeWorld(waypoint.world);
			waypoint.group = Waypoint.normalizeGroup(waypoint.group);
		}

		loaded.maxWaypoints = Math.max(1, Math.min(10, loaded.maxWaypoints));
		loaded.markerMaxDistance = Math.max(32, Math.min(4096, loaded.markerMaxDistance));
		return loaded;
	}

	/**
	 * Serialises a config. Package-private for round-trip tests.
	 */
	static String toJson(CoordKitConfig config) {
		return GSON.toJson(config);
	}

	public void save() {
		Path path = path();

		try {
			Files.createDirectories(path.getParent());

			try (Writer writer = Files.newBufferedWriter(path)) {
				writer.write(toJson(this));
			}
		} catch (Exception e) {
			CoordKit.LOGGER.warn("Could not write {}", path, e);
		}
	}
}
