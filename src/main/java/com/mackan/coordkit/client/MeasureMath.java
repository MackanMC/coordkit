package com.mackan.coordkit.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Distance, delta and midpoint helpers for the measure tool, plus the
 * block-grid staircase the ruler is drawn along.
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

	/**
	 * Number of one-block steps on the staircase between two points.
	 */
	public static int blockSteps(MeasurePoint a, MeasurePoint b) {
		BlockPoint from = blockOf(a);
		BlockPoint to = blockOf(b);
		return blockSteps(from, to);
	}

	public static int blockSteps(BlockPoint from, BlockPoint to) {
		return Math.abs(to.x() - from.x())
				+ Math.abs(to.y() - from.y())
				+ Math.abs(to.z() - from.z());
	}

	/**
	 * The block containing a world position.
	 */
	public static BlockPoint blockOf(MeasurePoint point) {
		return new BlockPoint(
				(int) Math.floor(point.x()),
				(int) Math.floor(point.y()),
				(int) Math.floor(point.z())
		);
	}

	/**
	 * The centre of a block, which is where the ruler places its dots.
	 */
	public static MeasurePoint centreOf(BlockPoint block) {
		return new MeasurePoint(block.x() + 0.5, block.y() + 0.5, block.z() + 0.5);
	}

	public static List<BlockPoint> blockPath(BlockPoint from, BlockPoint to) {
		return blockPath(from, to, 1);
	}

	/**
	 * Block centres visited by a staircase from {@code from} to {@code to},
	 * including both ends. Every step moves exactly one block along one axis, and
	 * the steps are interleaved so the path stays close to the direct line
	 * instead of running along one axis before the others.
	 *
	 * <p>{@code stride} keeps only every n-th node, which bounds the cost of
	 * drawing a very long measurement. Both ends are always included.</p>
	 */
	public static List<BlockPoint> blockPath(BlockPoint from, BlockPoint to, int stride) {
		int dx = to.x() - from.x();
		int dy = to.y() - from.y();
		int dz = to.z() - from.z();
		int nx = Math.abs(dx);
		int ny = Math.abs(dy);
		int nz = Math.abs(dz);
		int total = nx + ny + nz;
		int keepEvery = Math.max(1, stride);
		List<BlockPoint> path = new ArrayList<>(Math.min(total + 1, 512));

		path.add(from);

		if (total == 0) {
			return path;
		}

		int x = from.x();
		int y = from.y();
		int z = from.z();
		int sx = Integer.signum(dx);
		int sy = Integer.signum(dy);
		int sz = Integer.signum(dz);

		// How far the direct line has advanced along each axis. Whichever axis
		// is furthest ahead takes the next step, which spreads the steps evenly.
		double ax = 0.0;
		double ay = 0.0;
		double az = 0.0;

		for (int i = 1; i <= total; i++) {
			ax += (double) nx / total;
			ay += (double) ny / total;
			az += (double) nz / total;

			if (ax >= ay && ax >= az) {
				x += sx;
				ax -= 1.0;
			} else if (ay >= az) {
				y += sy;
				ay -= 1.0;
			} else {
				z += sz;
				az -= 1.0;
			}

			if (i % keepEvery == 0 || i == total) {
				BlockPoint node = new BlockPoint(x, y, z);

				if (!node.equals(path.get(path.size() - 1))) {
					path.add(node);
				}
			}
		}

		return path;
	}

	public record BlockPoint(int x, int y, int z) {
	}
}
