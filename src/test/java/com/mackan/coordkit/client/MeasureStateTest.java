package com.mackan.coordkit.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the measure tool's A -> B -> clear state machine.
 */
class MeasureStateTest {
	private static final MeasurePoint ORIGIN = new MeasurePoint(0.0, 64.0, 0.0);
	private static final MeasurePoint TARGET = new MeasurePoint(30.0, 65.0, 40.0);

	@Test
	void startsEmpty() {
		MeasureState state = new MeasureState();

		assertFalse(state.active());
		assertFalse(state.complete());
		assertNull(state.a());
		assertNull(state.b());
		assertEquals("", state.dimension());
	}

	@Test
	void firstPressSetsPointA() {
		MeasureState state = new MeasureState();

		assertEquals(MeasureState.Result.SET_A,
				state.press(ORIGIN, DimensionMath.OVERWORLD));
		assertTrue(state.active());
		assertFalse(state.complete());
		assertEquals(ORIGIN, state.a());
		assertNull(state.b());
		assertEquals(DimensionMath.OVERWORLD, state.dimension());
	}

	@Test
	void secondPressSetsPointB() {
		MeasureState state = new MeasureState();
		state.press(ORIGIN, DimensionMath.OVERWORLD);

		assertEquals(MeasureState.Result.SET_B,
				state.press(TARGET, DimensionMath.OVERWORLD));
		assertTrue(state.active());
		assertTrue(state.complete());
		assertEquals(ORIGIN, state.a());
		assertEquals(TARGET, state.b());
	}

	@Test
	void thirdPressClearsTheMeasurement() {
		MeasureState state = new MeasureState();
		state.press(ORIGIN, DimensionMath.OVERWORLD);
		state.press(TARGET, DimensionMath.OVERWORLD);

		assertEquals(MeasureState.Result.CLEARED,
				state.press(TARGET, DimensionMath.OVERWORLD));
		assertFalse(state.active());
		assertFalse(state.complete());
		assertNull(state.a());
		assertNull(state.b());
		assertEquals("", state.dimension());
	}

	@Test
	void pressingAgainAfterClearStartsAtA() {
		MeasureState state = new MeasureState();
		state.press(ORIGIN, DimensionMath.OVERWORLD);
		state.press(TARGET, DimensionMath.OVERWORLD);
		state.press(TARGET, DimensionMath.OVERWORLD);

		assertEquals(MeasureState.Result.SET_A,
				state.press(TARGET, DimensionMath.OVERWORLD));
		assertEquals(TARGET, state.a());
		assertNull(state.b());
	}

	@Test
	void dimensionChangeWhilePressingStartsANewA() {
		MeasureState state = new MeasureState();
		state.press(ORIGIN, DimensionMath.OVERWORLD);

		assertEquals(MeasureState.Result.SET_A,
				state.press(TARGET, DimensionMath.NETHER));
		assertEquals(TARGET, state.a());
		assertNull(state.b());
		assertEquals(DimensionMath.NETHER, state.dimension());
	}

	@Test
	void keepInDimensionClearsOnChange() {
		MeasureState state = new MeasureState();
		state.press(ORIGIN, DimensionMath.OVERWORLD);
		state.press(TARGET, DimensionMath.OVERWORLD);

		assertFalse(state.keepInDimension(DimensionMath.NETHER));
		assertFalse(state.active());
		assertNull(state.a());
		assertNull(state.b());
		assertEquals("", state.dimension());
	}

	@Test
	void keepInDimensionPreservesTheSameDimension() {
		MeasureState state = new MeasureState();
		state.press(ORIGIN, DimensionMath.OVERWORLD);
		state.press(TARGET, DimensionMath.OVERWORLD);

		assertTrue(state.keepInDimension(DimensionMath.OVERWORLD));
		assertTrue(state.complete());
		assertEquals(ORIGIN, state.a());
		assertEquals(TARGET, state.b());
	}

	@Test
	void keepInDimensionIsANoOpWhenInactive() {
		MeasureState state = new MeasureState();

		assertTrue(state.keepInDimension(DimensionMath.NETHER));
		assertFalse(state.active());
	}
}
