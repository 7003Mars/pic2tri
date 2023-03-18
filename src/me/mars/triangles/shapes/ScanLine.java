package me.mars.triangles.shapes;

public class ScanLine {
	public int y;
	public int x1, x2;

	public ScanLine(int x1, int x2, int y) {
		this.y = y;
		this.x1 = x1;
		this.x2 = x2;
	}
}
