package me.mars.triangles.shapes;

import arc.math.Rand;
import me.mars.triangles.MutateMap;
import me.mars.triangles.Generator;

public abstract class Shape {
	public short r, g, b, a;

	public abstract void randomise(Generator context, Rand rand);
	public abstract void mutate(Generator context, Rand rand);
	public abstract boolean invalid();
	public void set(Shape other) {
		this.r = other.r;
		this.g = other.g;
		this.b = other.b;
		this.a = other.a;
	}
	public void setColor(int col) {
		this.r = (short) ((col >>> 24) & 0xff);
		this.g = (short) ((col >>> 16) & 0xff);
		this.b = (short) ((col >>> 8) & 0xff);
		this.a = (short) (col & 0xff);
	}
	public abstract void fill(MutateMap pixmap);
	public abstract String toInstr();


}
