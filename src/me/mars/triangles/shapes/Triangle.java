package me.mars.triangles.shapes;

import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Point2;
import arc.util.Strings;
import arc.util.Structs;
import me.mars.triangles.MutateMap;
import me.mars.triangles.Generator;


public class Triangle extends Shape{
	// REMOVEME
//	public static int a1Fail = 0, a2Fail = 0, a3Fail = 0;

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
		final int max = 25;
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
		boolean canOut = this.x1 >= 0 && this.x1 <= w && this.x2 >= 0 && this.x2 <= w && this.x3 >= 0 && this.x3 <= w &&
				this.y1 >= 0 && this.y1 <= h && this.y2 >= 0 && this.y2 <= h && this.y3 >= 0 && this.y3 <= h;
//		boolean canOut = (this.x1 >= 0 && this.x1 <= w && this.y1 >= 0 && this.y1 <= h) ||
//				(this.x2 >= 0 && this.x2 <= w && this.y2 >= 0 && this.y2 <= h) ||
//				(this.x3 >= 0 && this.x3 <= w && this.y3 >= 0 && this.y3 <= h);
		int bounds = canOut ? 16 : 0;
		do {
			float rx = rand.nextFloat()*20f - 10f;
			int cx = (rx > 0) ? Mathf.ceilPositive(rx) : Mathf.floor(rx);
			float ry = rand.nextFloat()*20f - 10f;
			int cy = (ry > 0) ? Mathf.ceilPositive(ry) : Mathf.floor(ry);
			switch (rand.random(2)) {
				case 0 -> {
					this.x1 = Mathf.clamp(this.x1+cx, -bounds, w-1+bounds);
					this.y1 = Mathf.clamp(this.y1+cy, -bounds, h-1+bounds);
				}
				case 1 -> {
					this.x2 = Mathf.clamp(this.x2+cx, -bounds, w-1+bounds);
					this.y2 = Mathf.clamp(this.y2+cy, -bounds, h-1+bounds);
				}
				case 2 -> {
					this.x3 = Mathf.clamp(this.x3+cx, -bounds, w-1+bounds);
					this.y3 = Mathf.clamp(this.y3+cy, -bounds, h-1+bounds);
				}
			}
		} while (invalid());
	}

	@Override
	protected boolean invalid() {
		// TODO: Fix this entire function
//		return false;
		return (this.x1 == this.x2 || this.x2 == this.x3 || this.x1 == this.x3) || (this.y1 == this.y2 || this.y2 == this.y3 ||this.y1 == this.y3);
	}

	@Override
	public void fill(MutateMap pixmap) {
		// http://www.sunshine2k.de/coding/java/TriangleRasterization/TriangleRasterization.html
		Point2 p1 = pixmap.pointPool.obtain().set(this.x1, this.y1);
		Point2 p2 = pixmap.pointPool.obtain().set(this.x2, this.y2);
		Point2 p3 = pixmap.pointPool.obtain().set(this.x3, this.y3);
		Point2[] p = {p1, p2, p3};
//		Point2[] p = {new Point2(this.x1, this.y1), new Point2(this.x2, this.y2), new Point2(this.x3, this.y3)};
		pixmap.sort.sort(p, Structs.comparingInt(point2 -> point2.y));
		if (p[0].y == p[1].y) {
			// point[2] is the highest
			fillBotFlat(pixmap, p[0].x, p[0].y, p[1].x, p[1].y, p[2].x, p[2].y);
		} else if (p[1].y == p[2].y) {
			// point[0] is the lowest
			fillTopFlat(pixmap, p[0].x, p[0].y, p[1].x, p[1].y, p[2].x, p[2].y, false);
		} else {
			int x4 = Mathf.round(p[0].x + ((float)(p[1].y - p[0].y) / (float)(p[2].y - p[0].y)) * (p[2].x - p[0].x));
			int y4 = p[1].y;
//			Log.info("BotFlat: @ @ @ @ @ @ \n TopFlat: @ @ @ @ @ @", p[1].x, p[1].y, x4, y4, p[2].x, p[2].y,
//					p[0].x, p[0].y, x4, y4, p[1].x, p[1].y);
			fillBotFlat(pixmap, p[1].x, p[1].y, x4, y4, p[2].x, p[2].y);
			fillTopFlat(pixmap, p[0].x, p[0].y, x4, y4, p[1].x, p[1].y, true);
		}
		pixmap.pointPool.free(p1);
		pixmap.pointPool.free(p2);
		pixmap.pointPool.free(p3);

	}

	/**
	 * Fills a flat bottom triangle with y1=y2, y1 < y2 < y3
	 */
	private static void fillBotFlat(MutateMap pixmap, int x1, int y1, int x2, int y2, int x3, int y3) {
		float invSlope1 = (x1 - x3) / (float)(y1 - y3);
		float invSlope2 = (x2 - x3) / (float)(y2 - y3);
		// REMOVEME
		if (Float.isInfinite(invSlope1) || Float.isInfinite(invSlope2)) {
			throw new ArithmeticException("Stinky");
		}
		float curX1 = x3, curX2 = x3;
		for (int scanY = y3; scanY >= y1; scanY--) {
			pixmap.mark(pixmap.obtainLine().set((int)curX1,(int) curX2, scanY));
			curX1-=invSlope1;
			curX2-=invSlope2;
		}
	}

	/**
	 * Fills a flat top triangle with y2=y3, y1 < y2 < y3
	 */
	private static void fillTopFlat(MutateMap pixmap, int x1, int y1, int x2, int y2, int x3, int y3, boolean skipTop) {
		float invSlope1 = (x1 - x2) / (float)(y1 - y2);
		float invSlope2 = (x1 - x3) / (float)(y1 - y3);
		if (Float.isInfinite(invSlope1) || Float.isInfinite(invSlope2)) {
			throw new ArithmeticException("Stinky");
		}
		float curX1 = x1, curX2 = x1;
		if (skipTop) y3--;
		for (int scanY = y1; scanY <= y3; scanY++) {
			pixmap.mark(pixmap.obtainLine().set((int)curX1,(int) curX2, scanY));
			curX1+=invSlope1;
			curX2+=invSlope2;
		}
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
}
