package me.mars.triangles.shapes;

import arc.math.Rand;
import me.mars.triangles.Generator;
import me.mars.triangles.MutateMap;


public class Rectangle extends Shape{
	public int x1, y1, x2, y2;

	@Override
	public void randomise(Generator context, Rand rand) {
//		int size = context.size - 1;
//		this.x1 = Mathf.random(size);

	}

	@Override
	public void mutate(Generator context, Rand rand) {

	}

	@Override
	protected boolean invalid() {
		return false;
	}

	@Override
	public void set(Shape other) {
		super.set(other);
		Rectangle s = (Rectangle) other;
		this.x1 = s.x1;
		this.x2 = s.x2;
		this.y1 = s.y1;
		this.y2 = s.y2;
	}

	@Override
	public void fill(MutateMap pixmap) {
		for (int y = Math.min(this.y1, this.y2); y <= Math.max(this.y1, y2); y++) {
			pixmap.mark(new ScanLine(this.x1, this.x2, y));
		}
	}

	@Override
	public String toInstr() {
		return null;
	}
}
