package me.mars.triangles.shapes;

import arc.util.pooling.Pool;

/**
 * Inclusive of points x1 and x2
 */
public class ScanLine implements Pool.Poolable {
	public int y;
	public int x1, x2;

	public ScanLine set(int x1, int x2, int y) {
		this.y = y;
//		if (x2 < x1) {
//			Log.warn("Scanline is useless");
//		}
		this.x1 = x1;
		this.x2 = x2;
		return this;
	}

	@Override
	public String toString() {
		return "ScanLine{" +
				"y=" + y +
				", x1=" + x1 +
				", x2=" + x2 +
				'}';
	}

	@Override
	public void reset() {
		this.x1 = this.x2 = this.y = 0;
	}
}
