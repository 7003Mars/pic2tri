package me.mars.triangles;

import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.struct.Sort;
import arc.util.Log;
import arc.util.pooling.Pool;
import me.mars.triangles.shapes.ScanLine;

public class MutateMap extends Pixmap {
	public final Pixmap origin;
	private Seq<ScanLine> marks = new Seq<>();
	public Sort sort = new Sort();

	private Pool<ScanLine> linePool = new Pool<>() {
		@Override
		protected ScanLine newObject() {
			return new ScanLine();
		}
	};
	public Pool<Point2> pointPool = new Pool<>() {
		@Override
		protected Point2 newObject() {
			return new Point2();
		}
	};


	public MutateMap(Pixmap origin) {
		super(origin.width, origin.height);
		this.origin = origin;
	}

	public int calcColor(int alpha) {
		if (this.marks.size == 0) {
//			Log.warn("Nothing marked?");
			return 0;
		}
		// "Borrowed" code
		int sr = 0, sg = 0, sb = 0, count = 0;
		int a = (int) (257f * 255f/(float) alpha);
		for (ScanLine scanLine : this.marks) {
			int y = scanLine.y;
			count+= Math.abs(scanLine.x2 - scanLine.x1)+1;
			for (int x = scanLine.x1; x <= scanLine.x2; x++) {
				int originC = this.origin.getRaw(x, y);
				int currentC = this.getRaw(x, y);
				int cr = (currentC >>> 24) & 0xff;
				sr += (((originC >>> 24) & 0xff) - cr) * a + cr * 257;
				int cg = (currentC >>> 16) & 0xff;
				sg += (((originC >>> 16) & 0xff) - cg) * a + cg * 257;
				int cb = (currentC >>> 8) & 0xff;
				sb += (((originC >>> 8) & 0xff) - cb) * a + cb * 257;
			}
		}
		int r = Mathf.clamp((sr/count) >> 8, 0, 255);
		int g = Mathf.clamp((sg/count) >> 8, 0, 255);
		int b = Mathf.clamp((sb/count) >> 8, 0, 255);
		return Color.packRgba(r, g, b, alpha);
	}

	/**
	 * Lower is better
	 */
	public static int colorDiff2(int col1, int col2) {
		int r1 = (col1 >>> 24) & 0xff;
		int g1 = (col1 >>> 16) & 0xff;
		int b1 = (col1 >>> 8) & 0xff;
		int a1 = col1 & 0xff;

		int r2 = (col2 >>> 24) & 0xff;
		int g2 = (col2 >>> 16) & 0xff;
		int b2 = (col2 >>> 8) & 0xff;
		int a2 = col2 & 0xff;
		int r = r1 - r2;
		int g = g1 - g2;
		int b = b1 - b2;
		int a = a1 - a2;
		return r*r + b*b + g*g + a*a;
	}

	public void drop() {
		this.linePool.freeAll(this.marks);
		this.marks.clear();
	}

	public ScanLine obtainLine() {
		return this.linePool.obtain();
	}

	public void apply(int col) {
		for (ScanLine scanLine : this.marks) {
			int y = scanLine.y;
			for (int x = scanLine.x1; x <= scanLine.x2; x++) {
				this.setRaw(x, y, Pixmap.blend(col, this.getRaw(x, y)));
			}
		}
		this.drop();
	}


	/**
	 * Higher is better
	 */
	public long score2(int col) {
		if (this.marks.size == 0) return 0;
		long score = 0;
		for (ScanLine scanLine : this.marks) {
			int y = scanLine.y;
			for (int x = scanLine.x1; x <= scanLine.x2; x++) {
				int originColor = this.origin.getRaw(x, y);
				int selfCol = this.getRaw(x, y);
				score+= colorDiff2(originColor, selfCol);
				score-= colorDiff2(originColor, Pixmap.blend(col, selfCol));
			}
		}
		return score;
	}

	public long fullDiff() {
		long diff = 0;
		for (int x = 0; x < this.width; x++) {
			for (int y = 0; y < this.height; y++) {
				diff+= colorDiff2(this.origin.getRaw(x, y), this.getRaw(x, y));
			}
		}
		return diff;
	}

	public void mark(ScanLine scanLine) {
		if (scanLine.y < 0 || scanLine.y >= this.height) {
			this.linePool.free(scanLine);
			return;
		}
		if (scanLine.x2 < scanLine.x1) return;
		scanLine.x1 = Mathf.clamp(scanLine.x1, 0, this.width-1);
		scanLine.x2 = Mathf.clamp(scanLine.x2, 0, this.width-1);
		if (marks.contains(l -> l.y == scanLine.y)) Log.warn("@ already there? For line @", scanLine.y, scanLine);
		marks.add(scanLine);
	}

	@Override
	public void dispose() {
		super.dispose();
		this.pointPool.clear();
		this.linePool.clear();
	}
}
