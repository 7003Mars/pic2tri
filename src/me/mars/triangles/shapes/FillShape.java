package me.mars.triangles.shapes;

import arc.math.Rand;
import arc.util.Strings;
import me.mars.triangles.MutateMap;
import me.mars.triangles.Generator;

public class FillShape extends Shape{
	public FillShape(int r, int g, int b) {
		this.r = (short) r;
		this.g = (short) g;
		this.b = (short) b;
	}

	@Override
	public void randomise(Generator context, Rand rand) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void mutate(Generator context, Rand rand) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean invalid() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void fill(MutateMap pixmap) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toInstr() {
		// Instruction padding
		return Strings.format("print \"Made with PicToTri\"\ndraw clear @ @ @ 0 0 0\n", this.r, this.g, this.b);
	}
}
