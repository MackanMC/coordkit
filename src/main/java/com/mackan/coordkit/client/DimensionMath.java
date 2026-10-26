package com.mackan.coordkit.client;

import org.jetbrains.annotations.Nullable;

/**
 * Pure coordinate conversion between the Overworld and the Nether.
 *
 * <p>Only the horizontal axes scale: the Overworld is eight times larger than
 * the Nether, so {@code x} and {@code z} are divided by eight when entering the
 * Nether and multiplied by eight when leaving it. {@code y} is unchanged.
 * Values are kept as doubles and are never rounded here; formatting is left to
 * the HUD and to command output.</p>
 */
public final class DimensionMath {
	public static final String OVERWORLD = "minecraft:overworld";
	public static final String NETHER = "minecraft:the_nether";

	private DimensionMath() {
	}

	/**
	 * True for the two dimensions that share a coordinate scale.
	 */
	public static boolean isConvertible(String dimension) {
		return counterpart(dimension) != null;
	}

	/**
	 * The dimension on the other side of a portal, or {@code null} when this
	 * dimension has no counterpart.
	 */
	@Nullable
	public static String counterpart(String dimension) {
		String normalized = normalize(dimension);

		if (OVERWORLD.equals(normalized)) {
			return NETHER;
		}

		if (NETHER.equals(normalized)) {
			return OVERWORLD;
		}

		return null;
	}

	/**
	 * Converts an {@code x} or {@code z} value from one dimension to its
	 * counterpart. Non-convertible dimensions are returned unchanged.
	 */
	public static double convert(double value, String fromDimension) {
		String normalized = normalize(fromDimension);

		if (OVERWORLD.equals(normalized)) {
			return value / 8.0;
		}

		if (NETHER.equals(normalized)) {
			return value * 8.0;
		}

		return value;
	}

	/**
	 * Converts the horizontal coordinates and leaves {@code y} untouched.
	 */
	public static Coordinates convertCoordinates(double x, double y, double z, String fromDimension) {
		return new Coordinates(convert(x, fromDimension), y, convert(z, fromDimension));
	}

	/**
	 * Short display name for a convertible dimension, or an empty string for
	 * anything else.
	 */
	public static String label(String dimension) {
		String normalized = normalize(dimension);

		if (OVERWORLD.equals(normalized)) {
			return "Overworld";
		}

		if (NETHER.equals(normalized)) {
			return "Nether";
		}

		return "";
	}

	/**
	 * Accepts both {@code minecraft:overworld} and the short {@code overworld}
	 * form, since older configs and hand-edited files use both.
	 */
	static String normalize(String dimension) {
		if (dimension == null) {
			return "";
		}

		String trimmed = dimension.trim();

		if (trimmed.isEmpty() || trimmed.indexOf(':') >= 0) {
			return trimmed;
		}

		return "minecraft:" + trimmed;
	}

	public record Coordinates(double x, double y, double z) {
	}
}
