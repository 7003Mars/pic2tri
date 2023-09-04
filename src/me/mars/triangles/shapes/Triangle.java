package me.mars.triangles.shapes;

import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Structs;
import me.mars.triangles.Generator;
import me.mars.triangles.MutateMap;


public class Triangle extends Shape{
	private static final int maxOut = 16;
	private static final int max = 20;

	public int x1, y1, x2, y2, x3, y3;

	public Triangle() {
	}

	@Override
	public void set(Shape other) {
		super.set(other);
		Triangle s = (Triangle)other;
		this.x1 = s.x1;
		this.x2 = s.x2;
		this.x3 = s.x3;
		this.y1 = s.y1;
		this.y2 = s.y2;
		this.y3 = s.y3;
	}

	@Override
	public void randomise(Generator context, Rand rand) {
		int w = context.original.width - 1;
		int h = context.original.height - 1;
		do {
			this.x1 = rand.random(w);
			this.x2 = Mathf.clamp(this.x1 + rand.random(-max, max), 0, w);
			this.x3 = Mathf.clamp(this.x1 + rand.random(-max, max), 0, h);
			this.y1 = rand.random(h);
			this.y2 = Mathf.clamp(this.y1 + rand.random(-max, max), 0, w);
			this.y3 = Mathf.clamp(this.y1 + rand.random(-max, max), 0, h);
		} while (invalid());

	}

	@Override
	public void mutate(Generator context, Rand rand) {
		int w = context.original.width - 1;
		int h = context.original.height - 1;
		boolean p1In = this.x1 >= 0 && this.x1 < w && this.y1 >= 0 && this.y1 < h;
		boolean p2In = this.x2 >= 0 && this.x2 < w && this.y2 >= 0 && this.y2 < h;
		boolean p3In = this.x3 >= 0 && this.x3 < w && this.y3 >= 0 && this.y3 < h;
		int bounds;
		do {
			float rx = rand.nextFloat()*max - max/2f;
			int cx = (rx > 0) ? Mathf.ceilPositive(rx) : Mathf.floor(rx);
			float ry = rand.nextFloat()*max - max/2f;
			int cy = (ry > 0) ? Mathf.ceilPositive(ry) : Mathf.floor(ry);
			switch (rand.random(2)) {
				case 0 -> {
					bounds = p2In || p3In ? maxOut : 0;
					this.x1 = Mathf.clamp(this.x1+cx, -bounds, w-1+bounds);
					this.y1 = Mathf.clamp(this.y1+cy, -bounds, h-1+bounds);
				}
				case 1 -> {
					bounds = p1In || p3In ? maxOut : 0;
					this.x2 = Mathf.clamp(this.x2+cx, -bounds, w-1+bounds);
					this.y2 = Mathf.clamp(this.y2+cy, -bounds, h-1+bounds);
				}
				case 2 -> {
					bounds = p1In || p2In ? maxOut: 0;
					this.x3 = Mathf.clamp(this.x3+cx, -bounds, w-1+bounds);
					this.y3 = Mathf.clamp(this.y3+cy, -bounds, h-1+bounds);
				}
			}
		} while (invalid());
	}

	@Override
	public boolean invalid() {
		if ((this.x1 == this.x2 && this.x2 == this.x3) || (this.y1 == this.y2 && this.y2 == this.y3)) return true;
		return (this.x1 == this.x2 && this.y1 == this.y2) || (this.x2 == this.x3 && this.y2 == this.y3) || (this.x3 == this.x1 && this.y3 == this.y1);
	}

	@Override
	public void fill(MutateMap pixmap) {
		// http://www.sunshine2k.de/coding/java/TriangleRasterization/TriangleRasterization.html
		Point2 p1 = pixmap.pointPool.obtain().set(this.x1, this.y1);
		Point2 p2 = pixmap.pointPool.obtain().set(this.x2, this.y2);
		Point2 p3 = pixmap.pointPool.obtain().set(this.x3, this.y3);
		Point2[] p = {p1, p2, p3};
		pixmap.sort.sort(p, Structs.comparingInt(point2 -> point2.y));
		if (p[0].y == p[1].y) {
			// point[2] is the highest
			int x1 = p[0].x, x2 = p[1].x, hx = p[2].x, y1 = p[0].y, hy = p[2].y;
			if (x1 > x2) {
				int tmp = x2;
				x2 = x1;
				x1 = tmp;
			}
			Vec2 left = pixmap.vecPool.obtain().set(hx - x1, hy - y1), right = pixmap.vecPool.obtain().set(x2 - hx, y1 - hy);
			fillBotFlat(pixmap, hx, hy, y1, left, right, x1, y1);
			pixmap.vecPool.free(left);
			pixmap.vecPool.free(right);
		} else if (p[1].y == p[2].y) {
			// point[0] is the lowest
			int x1 = p[1].x, x2 = p[2].x, lx = p[0].x, y1 = p[1].y, ly = p[0].y;
			if (x1 > x2) {
				int tmp = x2;
				x2 = x1;
				x1 = tmp;
			}
			Vec2 left = pixmap.vecPool.obtain().set(x1 - lx, y1 - ly), right = pixmap.vecPool.obtain().set(lx - x2, ly - y1);
			fillTopFlat(pixmap, lx, ly, y1, left, right, x2, y1);
			pixmap.vecPool.free(left);
			pixmap.vecPool.free(right);
		} else {
			// p[0] is the lowest, p[2] is the highest, p[1] and (x2,  y1) are in between
			int lx = p[0].x, ly = p[0].y, hx = p[2].x, hy = p[2].y, x1 = p[1].x, y1 = p[1].y;
			double x2 = (p[0].x + ((double)(p[1].y - p[0].y) / (double) (p[2].y - p[0].y)) * (p[2].x - p[0].x));
			if (x2 == x1) {
				// Points probably lie on a line, skip this unlucky run
//				Log.warn("Whaa " + this);
				return;
			}
			if (x2 > x1) {
				// Right side is straight
				Vec2 right = pixmap.vecPool.obtain().set(lx - hx, ly - hy);
				Vec2 botLeft = pixmap.vecPool.obtain().set(x1 - lx, y1 - ly), topLeft = pixmap.vecPool.obtain().set(hx - x1, hy - y1);
				fillBotFlat(pixmap, hx, hy, y1, topLeft, right, x1, y1);
				fillTopFlat(pixmap, lx, ly, y1, botLeft, right, hx, hy);
				pixmap.vecPool.free(right);
				pixmap.vecPool.free(botLeft);
				pixmap.vecPool.free(topLeft);
			} else {
				// Left side is straight
				Vec2 left = pixmap.vecPool.obtain().set(hx - lx, hy - ly);
				Vec2 botRight = pixmap.vecPool.obtain().set(lx - x1, ly - y1), topRight = pixmap.vecPool.obtain().set(x1 - hx, y1 - hy);
				fillBotFlat(pixmap, hx, hy, y1, left, topRight, lx, ly);
				fillTopFlat(pixmap, lx, ly, y1, left, botRight, x1, y1);
				pixmap.vecPool.free(left);
				pixmap.vecPool.free(botRight);
				pixmap.vecPool.free(topRight);
			}
		}
		pixmap.pointPool.free(p1);
		pixmap.pointPool.free(p2);
		pixmap.pointPool.free(p3);

	}

	/**
	 *
	 * @param startX Lowest point
	 * @param startY Lowest point
	 * @param endY Highest point
	 * @param rx Reference point for right vector
	 * @param ry Reference point for right vector
	 */
	private static void fillTopFlat(MutateMap pixmap, int startX, int startY, int endY, Vec2 left, Vec2 right, int rx, int ry) {
		float invSlope1 = left.x / left.y, invSlope2 = right.x / right.y;
		if (Float.isInfinite(invSlope1) || Float.isInfinite(invSlope2)) {
			throw new ArithmeticException("Stinky");
		}
		float x1 = startX, x2 = startX;
		x1+= invSlope1/2f;
		x2+= invSlope2/2f;
		for (int scanY = startY; scanY < endY; scanY++) {
			int mark1 = accurateBounds(Mathf.floor(x1), scanY, left, true, startX, startY);
			int mark2 = accurateBounds(Mathf.ceil(x2), scanY, right, false, rx, ry);
			ScanLine line = pixmap.obtainLine().set(mark1, mark2, scanY);
			pixmap.mark(line);
			x1+= invSlope1;
			x2+= invSlope2;

		}

	}

	/**
	 *
	 * @param startX Highest point
	 * @param startY Highest point
	 * @param endY Lowest point
	 * @param left Lowest point
	 * @param rx Reference point for left vector
	 * @param ry Reference point for left vector
	 */
	private static void fillBotFlat(MutateMap pixmap, int startX, int startY, int endY, Vec2 left, Vec2 right, int rx, int ry) {
		float invSlope1 = left.x / left.y, invSlope2 = right.x / right.y;
		if (Float.isInfinite(invSlope1) || Float.isInfinite(invSlope2)) {
			throw new ArithmeticException("Stinky");
		}
		float x1 = startX, x2 = startX;
		x1+= invSlope1/2f;
		x2+= invSlope2/2f;
		for (int scanY = startY; scanY >= endY; scanY--) {
			int mark1 = accurateBounds(Mathf.floor(x1), scanY, left, true, rx, ry);
			int mark2 = accurateBounds(Mathf.ceil(x2), scanY, right, false, startX, startY);
			ScanLine line = pixmap.obtainLine().set(mark1, mark2, scanY);
			pixmap.mark(line);
			x1-= invSlope1;
			x2-= invSlope2;
		}
	}

	public static int accurateBounds(int x, int y, Vec2 vec, boolean ltr, int rx, int ry) {
		int sign = ltr ? 1 : -1;
		for (int i = 0; i < 4; i++) {
			float area = -vec.crs(x+0.5f-rx, y+0.5f-ry);
			if (area == 0 ? (vec.y > 0 || (vec.y == 0 && vec.x > 0)) : area > 0) {
				return x;
			}
			x+= sign;
		}
		Log.warn("Failed to find accurate bounds for (@, @) Ltr: @", x, y, ltr);
		return Integer.MIN_VALUE;
	}


	@Override
	public String toInstr() {
		return Strings.format("draw color @ @ @ @ 0 0\ndraw triangle @ @ @ @ @ @\n",
				this.r, this.g, this.b, this.a, this.x1, this.y1, this.x2, this.y2, this.x3, this.y3);
	}

	@Override
	public String toString() {
		return "Triangle{" +
				"r=" + r +
				", g=" + g +
				", b=" + b +
				", x1=" + x1 +
				", y1=" + y1 +
				", x2=" + x2 +
				", y2=" + y2 +
				", x3=" + x3 +
				", y3=" + y3 +
				'}';
	}

//	static int roundTo(float x, float dir) {
//		if (x == 0) return 0;
//		if (dir == 0) return Math.round(x);
//		return (dir > 0) ? Mathf.ceil(x) : Mathf.floor(x);
//	}
}
