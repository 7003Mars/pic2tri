package me.mars.triangles.shapes;

import arc.math.Mathf;
import arc.math.Rand;
import arc.util.Strings;
import me.mars.triangles.Generator;
import me.mars.triangles.MutateMap;


public class Rectangle extends Shape{
	public int x1, y1, x2, y2;

	@Override
	public void randomise(Generator context, Rand rand) {
		int w = context.original.width-1;
		int h = context.original.height-1;
		do {
			this.x1 = rand.random(w);
			this.x1 = this.x1 + rand.random(-context.maxRange, context.maxRange);
			this.y1 = rand.random(h);
			this.y2 = this.y1 + rand.random(-context.maxRange, context.maxRange);
		} while (this.invalid());
	}

	@Override
	public void mutate(Generator context, Rand rand) {
		int w = context.original.width-1;
		int h = context.original.height-1;
		do {
			float rx = rand.nextFloat()*context.maxRange - context.maxRange/2f;
			int cx = (rx > 0) ? Mathf.ceilPositive(rx) : Mathf.floor(rx);
			float ry = rand.nextFloat()*context.maxRange - context.maxRange/2f;
			int cy = (ry > 0) ? Mathf.ceilPositive(ry) : Mathf.floor(ry);
			if (rand.nextBoolean()) {
				this.x1 = Mathf.clamp(this.x1 + cx, 0, w);
				this.y1 = Mathf.clamp(this.y1 + cy, 0, w);
			} else {
				this.x2 = Mathf.clamp(this.x2 + cx, 0, h);
				this.y2 = Mathf.clamp(this.y2 + cy, 0, h);
			}
		} while (this.invalid());
	}

    @Override
    public void translate(int x, int y) {
        this.x1 += x;
        this.x2 += x;
        this.y1 += y;
        this.y2 += y;
    }

    @Override
	public boolean invalid() {
		return this.y1 == this.y2 || this.x1 == this.x2;
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
		int left, right;
		if (this.x1 > this.x2) {
			left = this.x1;
			right = this.x2-1;
		} else {
			left = this.x2;
			right = this.x1-1;
		}
		for (int y = Math.min(this.y1, this.y2)+1; y <= Math.max(this.y1, y2); y++) {
			pixmap.mark(pixmap.obtainLine().set(left, right, y));
		}
	}

	@Override
	public String toInstr() {
		int w = this.x2 - this.x1;
		int h = this.y2 - this.y1;
		return Strings.format("draw color @ @ @ @ 0 0\ndraw rect @ @ @ @ 0 0\n",
				this.r, this.g, this.b, this.a, this.x1, this.y1, w, h);
	}
}
