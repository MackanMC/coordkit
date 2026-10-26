package com.mackan.coordkit.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests distance, delta and midpoint maths for the ruler, plus the block-grid
 * path it is drawn along.
 */
class MeasureMathTest {
	private static final double EPSILON = 1.0e-9;

	@Test
	void distanceUsesAllThreeAxes() {
		MeasurePoint a = new MeasurePoint(0.0, 0.0, 0.0);
		MeasurePoint b = new MeasurePoint(3.0, 4.0, 0.0);

		assertEquals(5.0, MeasureMath.distance(a, b), EPSILON);

		MeasurePoint c = new MeasurePoint(0.0, 6.0, 8.0);

		assertEquals(10.0, MeasureMath.distance(a, c), EPSILON);
	}

	@Test
	void horizontalDistanceIgnoresHeight() {
		MeasurePoint a = new MeasurePoint(0.0, 12.0, 0.0);
		MeasurePoint b = new MeasurePoint(3.0, 112.0, 4.0);

		assertEquals(5.0, MeasureMath.horizontalDistance(a, b), EPSILON);
		assertEquals(Math.sqrt(25.0 + 100.0 * 100.0), MeasureMath.distance(a, b), EPSILON);
	}

	@Test
	void deltasAreMeasuredFromAToB() {
		MeasurePoint a = new MeasurePoint(10.0, 20.0, 30.0);
		MeasurePoint b = new MeasurePoint(-2.0, 25.0, 1.0);

		assertEquals(-12.0, MeasureMath.deltaX(a, b), EPSILON);
		assertEquals(5.0, MeasureMath.deltaY(a, b), EPSILON);
		assertEquals(-29.0, MeasureMath.deltaZ(a, b), EPSILON);
	}

	@Test
	void midpointIsHalfwayBetweenBothPoints() {
		MeasurePoint a = new MeasurePoint(0.0, 64.0, 0.0);
		MeasurePoint b = new MeasurePoint(32.0, 66.0, -28.0);

		MeasurePoint midpoint = MeasureMath.midpoint(a, b);

		assertEquals(16.0, midpoint.x(), EPSILON);
		assertEquals(65.0, midpoint.y(), EPSILON);
		assertEquals(-14.0, midpoint.z(), EPSILON);
	}

	@Test
	void samePointHasZeroDistance() {
		MeasurePoint point = new MeasurePoint(-12.5, 64.0, 300.25);

		assertEquals(0.0, MeasureMath.distance(point, point), EPSILON);
		assertEquals(0.0, MeasureMath.horizontalDistance(point, point), EPSILON);
		assertEquals(0.0, MeasureMath.deltaX(point, point), EPSILON);
		assertEquals(0.0, MeasureMath.deltaY(point, point), EPSILON);
		assertEquals(0.0, MeasureMath.deltaZ(point, point), EPSILON);
	}

	@Test
	void negativeCoordinatesAreHandled() {
		MeasurePoint a = new MeasurePoint(-10.0, 64.0, -10.0);
		MeasurePoint b = new MeasurePoint(-13.0, 68.0, -14.0);

		assertEquals(Math.sqrt(41.0), MeasureMath.distance(a, b), EPSILON);
		assertEquals(-3.0, MeasureMath.deltaX(a, b), EPSILON);
		assertEquals(4.0, MeasureMath.deltaY(a, b), EPSILON);
		assertEquals(-4.0, MeasureMath.deltaZ(a, b), EPSILON);
	}

	@Test
	void pointsSnapToBlockCentres() {
		MeasurePoint point = new MeasurePoint(-12.3, 64.7, 0.1);

		assertEquals(new MeasureMath.BlockPoint(-13, 64, 0), MeasureMath.blockOf(point));

		MeasurePoint centre = MeasureMath.centreOf(MeasureMath.blockOf(point));

		assertEquals(-12.5, centre.x(), EPSILON);
		assertEquals(64.5, centre.y(), EPSILON);
		assertEquals(0.5, centre.z(), EPSILON);
	}

	@Test
	void blockStepsCountTheStaircaseLength() {
		MeasurePoint a = new MeasurePoint(0.5, 64.5, 0.5);
		MeasurePoint b = new MeasurePoint(3.5, 66.5, 1.5);

		assertEquals(6, MeasureMath.blockSteps(a, b));
		assertEquals(0, MeasureMath.blockSteps(a, a));
	}

	@Test
	void blockPathVisitsOneBlockAtATime() {
		MeasureMath.BlockPoint from = new MeasureMath.BlockPoint(0, 64, 0);
		MeasureMath.BlockPoint to = new MeasureMath.BlockPoint(3, 66, 1);

		List<MeasureMath.BlockPoint> path = MeasureMath.blockPath(from, to);

		assertEquals(MeasureMath.blockSteps(from, to) + 1, path.size());
		assertEquals(from, path.get(0));
		assertEquals(to, path.get(path.size() - 1));
		assertUnitSteps(path);
	}

	@Test
	void blockPathInterleavesTheAxes() {
		List<MeasureMath.BlockPoint> path = MeasureMath.blockPath(
				new MeasureMath.BlockPoint(0, 0, 0),
				new MeasureMath.BlockPoint(3, 0, 1));

		assertEquals(List.of(
				new MeasureMath.BlockPoint(0, 0, 0),
				new MeasureMath.BlockPoint(1, 0, 0),
				new MeasureMath.BlockPoint(2, 0, 0),
				new MeasureMath.BlockPoint(2, 0, 1),
				new MeasureMath.BlockPoint(3, 0, 1)
		), path);
	}

	@Test
	void blockPathWithNoDistanceIsASinglePoint() {
		MeasureMath.BlockPoint point = new MeasureMath.BlockPoint(-7, 64, 12);

		assertEquals(List.of(point), MeasureMath.blockPath(point, point));
	}

	@Test
	void blockPathHandlesNegativeAxes() {
		MeasureMath.BlockPoint from = new MeasureMath.BlockPoint(5, 70, -2);
		MeasureMath.BlockPoint to = new MeasureMath.BlockPoint(-3, 64, 2);

		List<MeasureMath.BlockPoint> path = MeasureMath.blockPath(from, to);

		assertEquals(19, path.size());
		assertEquals(from, path.get(0));
		assertEquals(to, path.get(path.size() - 1));
		assertUnitSteps(path);
	}

	@Test
	void blockPathStrideKeepsTheEndsAndCapsTheDots() {
		MeasureMath.BlockPoint from = new MeasureMath.BlockPoint(0, 64, 0);
		MeasureMath.BlockPoint to = new MeasureMath.BlockPoint(100, 64, 0);

		List<MeasureMath.BlockPoint> path = MeasureMath.blockPath(from, to, 25);

		assertEquals(5, path.size());
		assertEquals(from, path.get(0));
		assertEquals(to, path.get(path.size() - 1));

		for (int i = 1; i < path.size(); i++) {
			assertEquals(25, path.get(i).x() - path.get(i - 1).x());
		}

		List<MeasureMath.BlockPoint> coarse = MeasureMath.blockPath(from, to, 1000);

		assertEquals(List.of(from, to), coarse);
	}

	private static void assertUnitSteps(List<MeasureMath.BlockPoint> path) {
		for (int i = 1; i < path.size(); i++) {
			MeasureMath.BlockPoint previous = path.get(i - 1);
			MeasureMath.BlockPoint current = path.get(i);
			int dx = Math.abs(current.x() - previous.x());
			int dy = Math.abs(current.y() - previous.y());
			int dz = Math.abs(current.z() - previous.z());

			assertEquals(1, dx + dy + dz, "each step moves exactly one block");
			assertTrue(dx <= 1 && dy <= 1 && dz <= 1, "a step moves along one axis only");
		}
	}
}
