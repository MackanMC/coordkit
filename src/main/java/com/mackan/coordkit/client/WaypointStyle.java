package com.mackan.coordkit.client;

/**
 * Shared palette for waypoint markers.
 */
public final class WaypointStyle {
	public static final int[] COLORS = {
			0x46D0C6, // cyan
			0x5BD75B, // lime
			0xF2D64B, // yellow
			0xF29E4C, // orange
			0xE5544B, // red
			0xE86FB0, // pink
			0x9B6BE8, // purple
			0xFFFFFF  // white
	};

	public static final String[] NAMES = {
			"Cyan", "Lime", "Yellow", "Orange", "Red", "Pink", "Purple", "White"
	};

	private WaypointStyle() {
	}

	public static int colorAt(int index) {
		return COLORS[Math.floorMod(index, COLORS.length)];
	}

	public static String nameAt(int index) {
		return NAMES[Math.floorMod(index, NAMES.length)];
	}

	public static int indexOf(int color) {
		for (int i = 0; i < COLORS.length; i++) {
			if (COLORS[i] == (color & 0xFFFFFF)) {
				return i;
			}
		}

		return 0;
	}
}
