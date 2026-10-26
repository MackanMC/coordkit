package com.mackan.coordkit.client;

/**
 * A world position used by the measure tool. Kept free of Minecraft types so
 * the distance and midpoint rules can be unit-tested directly.
 */
public record MeasurePoint(double x, double y, double z) {
}
