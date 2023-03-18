package me.mars.triangles.shapes;

import arc.math.Rand;
import me.mars.triangles.Generator;
import me.mars.triangles.MutateMap;


public class Rectangle extends Shape{
	int x1, y1, x2, y2;

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
	}

	@Override
	public void setColor(int col) {
		super.setColor(col);
	}

	@Override
	public void fill(MutateMap pixmap) {

	}

	@Override
	public String toInstr() {
		return null;
	}
}
