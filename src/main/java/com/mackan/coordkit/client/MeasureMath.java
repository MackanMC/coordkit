package com.mackan.coordkit.client;

/**
 * Distance, delta and midpoint helpers for the measure tool.
 */
public final class MeasureMath {
	private MeasureMath() {
	}

	public static double distance(MeasurePoint a, MeasurePoint b) {
		double dx = b.x() - a.x();
		double dy = b.y() - a.y();
		double dz = b.z() - a.z();
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/**
	 * Distance on the X/Z plane, ignoring height.
	 */
	public static double horizontalDistance(MeasurePoint a, MeasurePoint b) {
		double dx = b.x() - a.x();
		double dz = b.z() - a.z();
		return Math.sqrt(dx * dx + dz * dz);
	}

	public static double deltaX(MeasurePoint a, MeasurePoint b) {
		return b.x() - a.x();
	}

	public static double deltaY(MeasurePoint a, MeasurePoint b) {
		return b.y() - a.y();
	}

	public static double deltaZ(MeasurePoint a, MeasurePoint b) {
		return b.z() - a.z();
	}

	public static MeasurePoint midpoint(MeasurePoint a, MeasurePoint b) {
		return new MeasurePoint(
				(a.x() + b.x()) / 2.0,
				(a.y() + b.y()) / 2.0,
				(a.z() + b.z()) / 2.0
		);
	}
}
