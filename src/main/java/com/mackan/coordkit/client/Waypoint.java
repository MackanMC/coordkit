package com.mackan.coordkit.client;

import net.minecraft.resources.Identifier;

import java.util.Locale;

/**
 * A saved position in a specific dimension.
 *
 * <p>Fields are public and non-final so Gson can (de)serialize them without
 * custom adapters. The dimension is stored as a string rather than an
 * {@link Identifier} for the same reason.</p>
 */
public final class Waypoint {
	public String name = "Waypoint";
	public double x;
	public double y;
	public double z;
	public String dimension = "minecraft:overworld";
	public int color = 0x46D0C6;
	public boolean visible = true;

	/**
	 * World/server identifier from {@link WaypointScope}. Empty means the
	 * waypoint is global and appears in every world.
	 */
	public String world = "";

	/**
	 * True for the waypoint managed automatically after death.
	 */
	public boolean autoDeath = false;

	public Waypoint() {
	}

	public Waypoint(String name, double x, double y, double z, String dimension, int color) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.dimension = dimension;
		this.color = color;
	}

	public Identifier dimensionId() {
		Identifier id = Identifier.tryParse(dimension);
		return id != null ? id : Identifier.fromNamespaceAndPath("minecraft", "overworld");
	}

	public double distanceTo(double px, double py, double pz) {
		double dx = x - px;
		double dy = y - py;
		double dz = z - pz;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public String command() {
		return String.format(Locale.ROOT, "/tp @s %.2f %.2f %.2f", x, y, z);
	}

	public String coordsText() {
		return String.format(Locale.ROOT, "%.1f / %.1f / %.1f", x, y, z);
	}

	public String dimensionShortName() {
		String id = dimensionId().toString();
		int colon = id.indexOf(':');
		return colon >= 0 ? id.substring(colon + 1) : id;
	}
}
