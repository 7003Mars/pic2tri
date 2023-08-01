package me.mars.triangles.shapes;

import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Point2;
import arc.util.Strings;
import arc.util.Structs;
import me.mars.triangles.Generator;
import me.mars.triangles.MutateMap;


public class Triangle extends Shape{
	// REMOVEME
//	public static int a1Fail = 0, a2Fail = 0, a3Fail = 0;
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
		pixmap.sort.sort(p, Structs.comparingInt(point2 -> point2.y));
		if (p[0].y == p[1].y) {
			// point[2] is the highest
			fillBotFlat(pixmap, p[0].x, p[0].y, p[1].x, p[1].y, p[2].x, p[2].y);
		} else if (p[1].y == p[2].y) {
			// point[0] is the lowest
			fillTopFlat(pixmap, p[0].x, p[0].y, p[1].x, p[1].y, p[2].x, p[2].y);
		} else {
			int x4 = Mathf.round(p[0].x + ((float)(p[1].y - p[0].y) / (float)(p[2].y - p[0].y)) * (p[2].x - p[0].x));
			int y4 = p[1].y;
			// REMOVEME: Uncomment now
//			Log.info("split to (@, @)", x4, y4);
//			Log.info("BotFlat: @ @ @ @ @ @ \n TopFlat: @ @ @ @ @ @", p[1].x, p[1].y, x4, y4, p[2].x, p[2].y,
//					p[0].x, p[0].y, x4, y4, p[1].x, p[1].y);
			// REMOVEME: Map.apply should not be here
			fillBotFlat(pixmap, p[1].x, p[1].y, x4, y4, p[2].x, p[2].y);
//			pixmap.apply(Color.red.rgba());
			fillTopFlat(pixmap, p[0].x, p[0].y, x4, y4, p[1].x, p[1].y);
		}
		pixmap.pointPool.free(p1);
		pixmap.pointPool.free(p2);
		pixmap.pointPool.free(p3);

	}

	/**
	 * Fills a flat bottom triangle with y1=y2, y1 < y2 < y3
	 */
	private static void fillBotFlat(MutateMap pixmap, int x1, int y1, int x2, int y2, int x3, int y3) {
		if(x1 > x2) {
			int tmp = x1;
			x1 = x2;
			x2 = tmp;
		}
//		Log.info("Bottom: (@, @) (@, @) (@, @)", x1, y1, x2, y2, x3, y3);
		double invSlope1 = (x3 - x1) / (float)(y3 - y1);
		double invSlope2 = (x3 - x2) / (float)(y3 - y2);
		if (Double.isInfinite(invSlope1) || Double.isInfinite(invSlope2)) {
			throw new ArithmeticException("Stinky");
		}
		float curX1 = x3, curX2 = x3;
		for (int scanY = y3; scanY >= y1; scanY--) {
			// If x2 > 0.5, include the pixel, else discard (-1)
			// if x1 <= 0.5, include the pixel, else discard (+1)
			// TODO: Round or cast to int
			double mark1 = accurateBounds(x1, y1, x3, y3, (int) curX1, scanY, true);

			double mark2 = accurateBounds(x3, y3, x2, y2, (int) curX2, scanY, false);
//			if (scanY == 27) Log.info("Y @: @-@, marking @-@", scanY, curX1, curX2, mark1, mark2);
			pixmap.mark(pixmap.obtainLine().set((int) mark1, (int) mark2, scanY));
			curX1-=invSlope1;
			curX2-=invSlope2;
		}
	}

	/**
	 * Fills a flat top triangle with y2=y3, y1 < y2 < y3
	 */
	private static void fillTopFlat(MutateMap pixmap, int x1, int y1, int x2, int y2, int x3, int y3) {
		if(x2 > x3) {
			int tmp = x2;
			x2 = x3;
			x3 = tmp;
		}

//		Log.info("Top: (@ ,@), (@, @), (@, @)",x1,y1,x2,y2,x3,y3);
		float invSlope1 = (x1 - x2) / (float)(y1 - y2);
		float invSlope2 = (x1 - x3) / (float)(y1 - y3);
		if (Float.isInfinite(invSlope1) || Float.isInfinite(invSlope2)) {
			throw new ArithmeticException("Stinky");
		}
		float curX1 = x1, curX2 = x1;
		for (int scanY = y1; scanY <= y3-1; scanY++) {
			// if x1 <= 0.5, include the pixel, else discard (+1)
			// If x2 > 0.5, include the pixel, else discard (-1)
			int mark1 = accurateBounds(x1, y1, x2, y2, (int) curX1, scanY, true);
			int mark2 = accurateBounds(x3, y3, x1, y1, (int) curX2, scanY, false);
			pixmap.mark(pixmap.obtainLine().set(mark1, mark2, scanY));
			curX1+=invSlope1;
			curX2+=invSlope2;
		}
	}

	/**
	 * P1 is the tail, P2 is the head. Clockwise winding.
	 * Provided test coordinate will be translated to pixel coordinates
	 * @return
	 */

	public static int accurateBounds(int x1, int y1, int x2, int y2, int px, int py, boolean ltr) {
		/*
		Basically, the inverse slope is inaccurate. Using the inaccurate x coordinates,
		we test x-0.5, x+0.5, x+1.5 and pray the more accurate test returns a valid value.
		If the inaccurate coordinate is >0.5 off, we are dead.
		Use a for(3) loop, to find how far we can edge the test before it fails. (Depends on ltr/rtl)
		 */
		int sign = ltr ? 1 : -1;
		int x = px - sign;
		int ax = x2-x1, ay = y2-y1;
//		Log.info("-> (@, @)", ax, ay);
		for (int i = 0; i < 3; i++) {
			float area = ax * (y1-(py+0.5f)) - ay * (x1-(x+0.5f));
			// x will be 1.5, 2.5, 3.5,...
			// To get the actual coordinates, just truncate
//			if (py == 27) Log.info("(@, @) area: @", x+0.5f, py+0.5f, area);
			if (area == 0 ? (ay > 0 || (ay == 0 && ax < 1)) : area > 0) return x;
			x+= sign;
		}
//		Log.warn("Failed to find x coordinates");
		return -1;
//		int sign, rx = -1;
//		float startX, endX;
//		if (ltr) {
//			sign = 1;
//			startX = px - 0.5f;
//			endX = px + 0.5f;
//		} else {
//			sign = -1;
//			startX = px + 0.5f;
//			endX = px - 0.5f;
//		}
//		int ax = x2-x1, ay = y2-y1;
//		for (float x = startX; ltr ? x <= endX : x >= endX; x+=sign) {
//			float area = ax * (py+0.5f-y1) - ay * (x-x1);
//			if (area == 0 ? (ay > 0 || (ay == 0 && ax < 1)) : area > 0) rx = (int) (x-0.5f);
//		}
//		return rx;

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
