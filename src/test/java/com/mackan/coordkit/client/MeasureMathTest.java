package com.mackan.coordkit.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests distance, delta and midpoint maths for the ruler.
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
}
