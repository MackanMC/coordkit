package com.mackan.coordkit.client;

import org.jetbrains.annotations.Nullable;

/**
 * Transient two-point measurement: the first press sets A, the second sets B
 * and the third clears everything.
 */
public final class MeasureState {
	public enum Result {
		SET_A,
		SET_B,
		CLEARED
	}

	@Nullable
	private MeasurePoint a;
	@Nullable
	private MeasurePoint b;
	private String dimension = "";

	public Result press(MeasurePoint point, String dimension) {
		String nextDimension = dimension == null ? "" : dimension;

		if (this.a == null || !this.dimension.equals(nextDimension)) {
			this.a = point;
			this.b = null;
			this.dimension = nextDimension;
			return Result.SET_A;
		}

		if (this.b == null) {
			this.b = point;
			return Result.SET_B;
		}

		clear();
		return Result.CLEARED;
	}

	public void clear() {
		this.a = null;
		this.b = null;
		this.dimension = "";
	}

	/**
	 * Drops a measurement that started in another dimension. Returns true when
	 * the state still belongs to the supplied dimension.
	 */
	public boolean keepInDimension(String dimension) {
		if (this.a == null) {
			return true;
		}

		String nextDimension = dimension == null ? "" : dimension;

		if (!this.dimension.equals(nextDimension)) {
			clear();
			return false;
		}

		return true;
	}

	public boolean active() {
		return this.a != null;
	}

	public boolean complete() {
		return this.a != null && this.b != null;
	}

	@Nullable
	public MeasurePoint a() {
		return this.a;
	}

	@Nullable
	public MeasurePoint b() {
		return this.b;
	}

	public String dimension() {
		return this.dimension;
	}
}
