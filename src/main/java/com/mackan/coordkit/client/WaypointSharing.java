package com.mackan.coordkit.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Converts waypoints to and from the 1.4.0 export formats.
 *
 * <p>This class is deliberately independent of screens and the clipboard so the
 * format rules can be tested on their own. Two formats are supported:</p>
 *
 * <ul>
 *   <li>a JSON envelope for files and full-list sharing;</li>
 *   <li>single-line {@code CK1;...} codes for one waypoint at a time.</li>
 * </ul>
 */
public final class WaypointSharing {
	public static final String FORMAT = "coordkit.waypoints";
	public static final int FORMAT_VERSION = 1;
	public static final int MAX_IMPORT = 500;
	public static final double MAX_COORDINATE = 30_000_000.0;

	private static final String SHARE_PREFIX = "CK1";
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();

	private WaypointSharing() {
	}

	/**
	 * How an import should treat the stored world key.
	 */
	public enum ScopePolicy {
		KEEP,
		CURRENT_WORLD,
		GLOBAL
	}

	/**
	 * One rejected entry. {@code location} is a user-facing label such as
	 * "Line 2" or "Entry 4".
	 */
	public record InvalidEntry(String location, String reason) {
	}

	/**
	 * The result of parsing import text. Nothing is applied until the caller
	 * uses {@link #waypointsToAdd()}.
	 */
	public static final class ImportPlan {
		private final List<Waypoint> valid;
		private final List<Waypoint> duplicates;
		private final List<InvalidEntry> invalid;
		private final String error;
		private final boolean allOrNothing;

		private ImportPlan(List<Waypoint> valid, List<Waypoint> duplicates, List<InvalidEntry> invalid,
				String error, boolean allOrNothing) {
			this.valid = List.copyOf(valid);
			this.duplicates = List.copyOf(duplicates);
			this.invalid = List.copyOf(invalid);
			this.error = error;
			this.allOrNothing = allOrNothing;
		}

		private static ImportPlan error(String message) {
			return new ImportPlan(List.of(), List.of(), List.of(), message, true);
		}

		/**
		 * A file-level failure, or {@code null} when parsing produced entries.
		 */
		public String error() {
			return error;
		}

		public List<Waypoint> valid() {
			return valid;
		}

		public List<Waypoint> duplicates() {
			return duplicates;
		}

		public List<InvalidEntry> invalid() {
			return invalid;
		}

		public int validCount() {
			return valid.size();
		}

		public int duplicateCount() {
			return duplicates.size();
		}

		public int invalidCount() {
			return invalid.size();
		}

		/**
		 * True when a single invalid entry blocks the whole import.
		 */
		public boolean allOrNothing() {
			return allOrNothing;
		}

		/**
		 * True when the caller may add something. JSON imports are
		 * all-or-nothing, so any invalid entry blocks them.
		 */
		public boolean canImport() {
			return error == null && !valid.isEmpty() && (!allOrNothing || invalid.isEmpty());
		}

		/**
		 * The waypoints that should be appended to the config.
		 */
		public List<Waypoint> waypointsToAdd() {
			return canImport() ? valid : List.of();
		}
	}

	public static String exportJson(List<Waypoint> waypoints) {
		JsonObject root = new JsonObject();
		root.addProperty("format", FORMAT);
		root.addProperty("version", FORMAT_VERSION);
		root.addProperty("exportedAt", Instant.now().toString());

		JsonArray array = new JsonArray();

		if (waypoints != null) {
			for (Waypoint waypoint : waypoints) {
				if (waypoint != null) {
					array.add(GSON.toJsonTree(waypoint));
				}
			}
		}

		root.add("waypoints", array);
		return GSON.toJson(root);
	}

	public static String exportShareCodes(List<Waypoint> waypoints) {
		StringBuilder result = new StringBuilder();

		if (waypoints != null) {
			for (Waypoint waypoint : waypoints) {
				if (waypoint == null) {
					continue;
				}

				if (!result.isEmpty()) {
					result.append('\n');
				}

				result.append(shareCode(waypoint));
			}
		}

		return result.toString();
	}

	public static String shareCode(Waypoint waypoint) {
		String world = WaypointScope.normalizeWorld(waypoint.world);
		String dimension = waypoint.dimensionId().toString();

		return String.join(";",
				SHARE_PREFIX,
				encode(Waypoint.normalizeName(waypoint.name)),
				Double.toString(waypoint.x),
				Double.toString(waypoint.y),
				Double.toString(waypoint.z),
				dimension,
				Integer.toString(waypoint.color & 0xFFFFFF),
				encode(world),
				waypoint.visible ? "1" : "0"
		);
	}

	/**
	 * Parses JSON or one-or-more share codes. The existing list is only used
	 * for duplicate detection; it is never modified.
	 */
	public static ImportPlan importText(String text, List<Waypoint> existing, String currentWorldKey, ScopePolicy policy) {
		if (text == null || text.isBlank()) {
			return ImportPlan.error("Nothing to import.");
		}

		String cleaned = stripBom(text).trim();

		if (cleaned.startsWith("{")) {
			return importJson(cleaned, existing, currentWorldKey, policy);
		}

		return importShareCodes(cleaned, existing, currentWorldKey, policy);
	}

	private static ImportPlan importJson(String text, List<Waypoint> existing, String currentWorldKey, ScopePolicy policy) {
		JsonObject root;

		try {
			JsonElement element = JsonParser.parseString(text);

			if (!element.isJsonObject()) {
				return ImportPlan.error("This is not a CoordKit waypoint file.");
			}

			root = element.getAsJsonObject();
		} catch (RuntimeException e) {
			return ImportPlan.error("This is not valid JSON.");
		}

		try {
			if (!root.has("format") || !FORMAT.equals(root.get("format").getAsString())) {
				return ImportPlan.error("This is not a CoordKit waypoint file.");
			}

			if (!root.has("version") || root.get("version").getAsInt() != FORMAT_VERSION) {
				return ImportPlan.error("Unsupported waypoint file version.");
			}
		} catch (RuntimeException e) {
			return ImportPlan.error("This is not a CoordKit waypoint file.");
		}

		if (!root.has("waypoints") || !root.get("waypoints").isJsonArray()) {
			return ImportPlan.error("The file does not contain a waypoint list.");
		}

		JsonArray array = root.getAsJsonArray("waypoints");

		if (array.size() > MAX_IMPORT) {
			return ImportPlan.error("The file contains " + array.size()
					+ " waypoints; the limit is " + MAX_IMPORT + ".");
		}

		if (array.isEmpty()) {
			return ImportPlan.error("The file contains no waypoints.");
		}

		List<Waypoint> valid = new ArrayList<>();
		List<Waypoint> duplicates = new ArrayList<>();
		List<InvalidEntry> invalid = new ArrayList<>();
		Set<String> seen = existingKeys(existing);

		for (int i = 0; i < array.size(); i++) {
			String location = "Entry " + (i + 1);
			Waypoint waypoint = parseJsonEntry(array.get(i), location, invalid);

			if (waypoint == null) {
				continue;
			}

			applyScopeAndDuplicate(waypoint, location, policy, currentWorldKey, seen, valid, duplicates, invalid);
		}

		return new ImportPlan(valid, duplicates, invalid, null, true);
	}

	private static ImportPlan importShareCodes(String text, List<Waypoint> existing, String currentWorldKey, ScopePolicy policy) {
		String[] lines = text.split("\\R", -1);
		List<Waypoint> valid = new ArrayList<>();
		List<Waypoint> duplicates = new ArrayList<>();
		List<InvalidEntry> invalid = new ArrayList<>();
		Set<String> seen = existingKeys(existing);
		boolean found = false;

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i].trim();

			if (line.isEmpty()) {
				continue;
			}

			found = true;
			String location = "Line " + (i + 1);
			Waypoint waypoint = parseShareCode(line, location, invalid);

			if (waypoint == null) {
				continue;
			}

			applyScopeAndDuplicate(waypoint, location, policy, currentWorldKey, seen, valid, duplicates, invalid);
		}

		if (!found) {
			return ImportPlan.error("Nothing to import.");
		}

		return new ImportPlan(valid, duplicates, invalid, null, false);
	}

	private static Waypoint parseJsonEntry(JsonElement element, String location, List<InvalidEntry> invalid) {
		if (element == null || !element.isJsonObject()) {
			invalid.add(new InvalidEntry(location, "Expected a waypoint object."));
			return null;
		}

		Waypoint waypoint;

		try {
			waypoint = GSON.fromJson(element, Waypoint.class);
		} catch (RuntimeException e) {
			invalid.add(new InvalidEntry(location, "Fields have the wrong type."));
			return null;
		}

		if (waypoint == null) {
			invalid.add(new InvalidEntry(location, "Expected a waypoint object."));
			return null;
		}

		return validate(waypoint, location, invalid);
	}

	private static Waypoint parseShareCode(String line, String location, List<InvalidEntry> invalid) {
		String[] parts = line.split(";", -1);

		if (parts.length != 9 || !SHARE_PREFIX.equals(parts[0])) {
			invalid.add(new InvalidEntry(location, "Expected a CK1 waypoint code with 9 fields."));
			return null;
		}

		try {
			String name = decode(parts[1]);
			double x = Double.parseDouble(parts[2]);
			double y = Double.parseDouble(parts[3]);
			double z = Double.parseDouble(parts[4]);
			String dimension = parts[5];
			int color = Integer.parseInt(parts[6]);
			String world = decode(parts[7]);
			boolean visible = switch (parts[8]) {
				case "1" -> true;
				case "0" -> false;
				default -> throw new IllegalArgumentException("Visibility must be 0 or 1.");
			};

			Waypoint waypoint = new Waypoint(name, x, y, z, dimension, color);
			waypoint.world = world;
			waypoint.visible = visible;
			return validate(waypoint, location, invalid);
		} catch (IllegalArgumentException e) {
			invalid.add(new InvalidEntry(location, e.getMessage() == null ? "Malformed waypoint code." : e.getMessage()));
			return null;
		}
	}

	private static Waypoint validate(Waypoint waypoint, String location, List<InvalidEntry> invalid) {
		if (!Double.isFinite(waypoint.x) || !Double.isFinite(waypoint.y) || !Double.isFinite(waypoint.z)) {
			invalid.add(new InvalidEntry(location, "Coordinates must be finite."));
			return null;
		}

		if (Math.abs(waypoint.x) > MAX_COORDINATE
				|| Math.abs(waypoint.y) > MAX_COORDINATE
				|| Math.abs(waypoint.z) > MAX_COORDINATE) {
			invalid.add(new InvalidEntry(location, "Coordinates are out of range."));
			return null;
		}

		Identifier dimension = Identifier.tryParse(waypoint.dimension == null ? "" : waypoint.dimension);

		if (dimension == null) {
			invalid.add(new InvalidEntry(location, "Invalid dimension."));
			return null;
		}

		String world = WaypointScope.normalizeWorld(waypoint.world);

		if (!isValidWorld(world)) {
			invalid.add(new InvalidEntry(location, "Invalid world scope."));
			return null;
		}

		waypoint.name = Waypoint.normalizeName(waypoint.name);
		waypoint.group = Waypoint.normalizeGroup(waypoint.group);
		waypoint.dimension = dimension.toString();
		waypoint.world = world;
		waypoint.color &= 0xFFFFFF;
		waypoint.autoDeath = false;
		return waypoint;
	}

	private static void applyScopeAndDuplicate(Waypoint waypoint, String location, ScopePolicy policy,
			String currentWorldKey, Set<String> seen, List<Waypoint> valid, List<Waypoint> duplicates,
			List<InvalidEntry> invalid) {
		ScopePolicy effective = policy == null ? ScopePolicy.KEEP : policy;
		String world = applyPolicy(waypoint.world, effective, currentWorldKey);

		if (world == null) {
			invalid.add(new InvalidEntry(location, "The current world is unknown."));
			return;
		}

		waypoint.world = world;
		String key = duplicateKey(waypoint);

		if (!seen.add(key)) {
			duplicates.add(waypoint);
			return;
		}

		valid.add(waypoint);
	}

	private static String applyPolicy(String world, ScopePolicy policy, String currentWorldKey) {
		return switch (policy) {
			case KEEP -> world;
			case GLOBAL -> "";
			case CURRENT_WORLD -> {
				String key = WaypointScope.normalizeWorld(currentWorldKey);
				yield key.isEmpty() ? null : key;
			}
		};
	}

	private static boolean isValidWorld(String world) {
		if (world.isEmpty()) {
			return true;
		}

		return (world.startsWith("sp:") && world.length() > 3)
				|| (world.startsWith("mp:") && world.length() > 3);
	}

	private static Set<String> existingKeys(List<Waypoint> existing) {
		Set<String> keys = new HashSet<>();

		if (existing != null) {
			for (Waypoint waypoint : existing) {
				if (waypoint != null) {
					keys.add(duplicateKey(waypoint));
				}
			}
		}

		return keys;
	}

	/**
	 * Identity used for additive imports: same name, dimension, scope and
	 * position rounded to two decimals.
	 */
	static String duplicateKey(Waypoint waypoint) {
		String dimension = waypoint.dimension == null ? "" : waypoint.dimension.trim().toLowerCase(Locale.ROOT);
		long x = Math.round(waypoint.x * 100.0);
		long y = Math.round(waypoint.y * 100.0);
		long z = Math.round(waypoint.z * 100.0);

		return Waypoint.normalizeName(waypoint.name).toLowerCase(Locale.ROOT)
				+ '\u0000' + dimension
				+ '\u0000' + WaypointScope.normalizeWorld(waypoint.world)
				+ '\u0000' + x
				+ '\u0000' + y
				+ '\u0000' + z;
	}

	private static String encode(String value) {
		return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
	}

	private static String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	private static String stripBom(String text) {
		return text.startsWith("\uFEFF") ? text.substring(1) : text;
	}
}
