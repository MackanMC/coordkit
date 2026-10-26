package com.mackan.coordkit.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the coordinate scale shared by the Overworld and the Nether.
 */
class DimensionMathTest {
	private static final double EPSILON = 1.0e-9;

	@Test
	void overworldDividesHorizontalCoordinates() {
		assertEquals(20.0, DimensionMath.convert(160.0, DimensionMath.OVERWORLD), EPSILON);
		assertEquals(0.0, DimensionMath.convert(0.0, DimensionMath.OVERWORLD), EPSILON);
		assertEquals(-300.0, DimensionMath.convert(-2400.0, DimensionMath.OVERWORLD), EPSILON);
		assertEquals(-0.125, DimensionMath.convert(-1.0, DimensionMath.OVERWORLD), EPSILON);
	}

	@Test
	void netherMultipliesHorizontalCoordinates() {
		assertEquals(1280.0, DimensionMath.convert(160.0, DimensionMath.NETHER), EPSILON);
		assertEquals(0.0, DimensionMath.convert(0.0, DimensionMath.NETHER), EPSILON);
		assertEquals(-19200.0, DimensionMath.convert(-2400.0, DimensionMath.NETHER), EPSILON);
	}

	@Test
	void conversionLeavesYUntouched() {
		DimensionMath.Coordinates overworld = DimensionMath.convertCoordinates(
				160.0, 64.0, -2400.0, DimensionMath.OVERWORLD);

		assertEquals(20.0, overworld.x(), EPSILON);
		assertEquals(64.0, overworld.y(), EPSILON);
		assertEquals(-300.0, overworld.z(), EPSILON);

		DimensionMath.Coordinates nether = DimensionMath.convertCoordinates(
				20.0, 64.0, -300.0, DimensionMath.NETHER);

		assertEquals(160.0, nether.x(), EPSILON);
		assertEquals(64.0, nether.y(), EPSILON);
		assertEquals(-2400.0, nether.z(), EPSILON);
	}

	@Test
	void counterpartIsBidirectional() {
		assertEquals(DimensionMath.NETHER, DimensionMath.counterpart(DimensionMath.OVERWORLD));
		assertEquals(DimensionMath.OVERWORLD, DimensionMath.counterpart(DimensionMath.NETHER));
	}

	@Test
	void labelsMatchTheDisplayedDimensionNames() {
		assertEquals("Overworld", DimensionMath.label(DimensionMath.OVERWORLD));
		assertEquals("Nether", DimensionMath.label(DimensionMath.NETHER));
		assertEquals("", DimensionMath.label("minecraft:the_end"));
	}

	@Test
	void shortDimensionNamesAreAccepted() {
		assertEquals(DimensionMath.NETHER, DimensionMath.counterpart("overworld"));
		assertEquals(DimensionMath.OVERWORLD, DimensionMath.counterpart("the_nether"));
		assertEquals(20.0, DimensionMath.convert(160.0, "overworld"), EPSILON);
	}

	@Test
	void nonConvertibleDimensionsHaveNoCounterpart() {
		assertNull(DimensionMath.counterpart("minecraft:the_end"));
		assertNull(DimensionMath.counterpart(""));
		assertNull(DimensionMath.counterpart(null));
		assertFalse(DimensionMath.isConvertible("minecraft:the_end"));
		assertEquals(42.0, DimensionMath.convert(42.0, "minecraft:the_end"), EPSILON);
		assertEquals(42.0, DimensionMath.convert(42.0, null), EPSILON);
	}

	@Test
	void convertibleDimensionsAreDetected() {
		assertTrue(DimensionMath.isConvertible(DimensionMath.OVERWORLD));
		assertTrue(DimensionMath.isConvertible(DimensionMath.NETHER));
	}
}
