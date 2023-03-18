package me.mars.triangles;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.struct.Bits;
import arc.struct.IntSeq;
import arc.struct.IntSet;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import me.mars.triangles.shapes.Shape;
import mindustry.content.Blocks;
import mindustry.game.Schematic.Stile;
import mindustry.logic.LExecutor;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;
import mindustry.world.blocks.logic.LogicDisplay;

public class SchemBuilder {
	static final String codeStart = """
			set i 0
			sensor e display1 @enabled
			jump 1 equal e 1
			set t @tick
			jump 4 equal @tick t
			control enabled display1 1 0 0 0
			op add i i 1
			jump 1 lessThan i $
			""";
	static final String codeEnd = """
			set i -$
			control enabled display1 0 0 0 0
			jump 1 always 0 0
			""";
	public static final int Max_Shapes;
	static {
		int freeInstructions = LExecutor.maxInstructions - (Strings.count(codeStart, "\n") + Strings.count(codeEnd, "\n"));
		// Account for draw flushes
		freeInstructions-= Mathf.ceilPositive(freeInstructions/256f);
		Max_Shapes = freeInstructions/2;
		// REMOVEME
//		Max_Shapes = 10;
	}

	public Seq<Display> displays;
	Bits occupied;
	public int width, height;
	int xChunks, yChunks;
	int procRange, dispSize;

	public SchemBuilder(int xChunks, int yChunks, int procRange, int dispSize) {
		this.xChunks = xChunks;
		this.yChunks = yChunks;
		this.procRange = procRange;
		this.dispSize = dispSize;
		this.width = xChunks * dispSize + procRange*2;
		this.height = yChunks * dispSize + procRange*2;

		this.occupied = new Bits(this.width * this.height);
		for (int y = procRange; y < this.height - procRange; y++) {
			int start = y * this.width + procRange;
			this.occupied.set(start, start + this.width - (2*procRange));
		}
		this.displays = new Seq<>();
		int offset = (dispSize - 1)/2 + this.procRange;
		for (int i = 0; i < xChunks * yChunks; i++) {
			Display display = new Display((i % xChunks) * dispSize + offset, i/xChunks * dispSize + offset);
			this.displays.add(display);
		}
		int midX = this.width/2, midY = this.height/2;
		this.displays.copy().sort(display -> Mathf.dst2(display.x, display.y, midX, midY))
				.each(display -> display.obtain(1));
	}

	public Display getDisplay(int cx, int cy) {
		if (cx < 0 || cx > this.xChunks || cy < 0 || cy > this.yChunks) {
			throw new IllegalArgumentException(Strings.format("@, @ not within 0-@, 0-@", cx, cy, this.xChunks, this.yChunks));
		}
		return this.displays.get(cy * this.xChunks + cx);
	}

	public void occupy(int x, int y, boolean occupied) {
		if (this.out(x, y)) {
			Log.err("Position @, @ not within size: @, @", this.width, this.height, x, y);
		}
		this.occupied.set(y * this.width + x, occupied);
	}

	public boolean occupied(int x, int y) {
		if (this.out(x, y)) return true;
		return this.occupied.get(y * this.width + x);
	}

	public boolean out(int x, int y) {
		return x >= this.width || x < 0 || y >= this.height || y < 0;
	}

	public class Display extends Generator.GenOpts {
		int x, y;
		IntSet pointSet = new IntSet();
		public IntSeq points = new IntSeq();

		public Color color = Color.HSVtoRGB(Mathf.random(360), 100, 100);

		public Display(int x, int y) {
			super(175, SchemBuilder.Max_Shapes -1);
			this.x = x;
			this.y = y;
		}

		private int obtain(int target) {
			int dir = 0;
			int x = this.x, y = this.y;
			int segLen = 0, maxSegLen = 1;
			int range2 = SchemBuilder.this.procRange + SchemBuilder.this.dispSize*2;
			range2 *= range2;
			for (int i = 0; i < range2; i++) {
				if (this.points.size >= target) return target;
				x+= Geometry.d4x(dir);
				y+= Geometry.d4y(dir);
				segLen++;
				if (segLen >= maxSegLen) {
					dir = (dir+1)%4;
					segLen = 0;
					if (dir == 0 || dir == 2) maxSegLen += 1;
				}
				if (!SchemBuilder.this.occupied(x, y) && !this.pointSet.contains(Point2.pack(x, y)) && this.within(x, y)) {
					SchemBuilder.this.occupy(x, y, true);
					this.pointSet.add(Point2.pack(x, y));
					this.points.add(Point2.pack(x, y));
				}
			}
			Log.warn("@ only obtained @ processors", this, this.points.size);
			return this.points.size;
		}

		private int free(int target) {
			for (int i = this.points.size-1; i > target; i--) {
				int pos = this.points.pop();
				this.pointSet.remove(pos);
				SchemBuilder.this.occupy(Point2.x(pos), Point2.y(pos), false);
			}
			return this.points.size;
		}

		public int getProcs(int target) {
			int obtained = target > this.points.size ? this.obtain(target) : this.free(target);
			this.maxGen = obtained * Max_Shapes;
			return obtained;
		}

		public boolean within(int x, int y) {
			float cx = (this.x - (float)(SchemBuilder.this.dispSize - 1)/2) + SchemBuilder.this.dispSize/2f;
			float cy = (this.y - (float)(SchemBuilder.this.dispSize - 1)/2) + SchemBuilder.this.dispSize/2f;
			int range2 = SchemBuilder.this.procRange * SchemBuilder.this.procRange;
			return Mathf.dst2(cx, cy, x, y) < range2;
		}

		public void build(Seq<Shape> shapes, Seq<Stile> tileArray, LogicDisplay display) {
			int i = 0;
			int usedProcs = Mathf.ceilPositive((float) shapes.size/ Max_Shapes);
			for (int index = 0; index < this.points.size; index++) {
				int pos = this.points.items[index];
				int x = Point2.x(pos);
				int y = Point2.y(pos);
				StringBuilder builder = new StringBuilder();
				builder.append(codeStart.replace("$", String.valueOf(index+1)));
				int j = 0;
				for (; j < Max_Shapes; j++) {
					if (i+j >= shapes.size-1) break;
					builder.append(shapes.get(i+j).toInstr());
					if (j != 0 && j % 128 == 0) {
						builder.append("drawflush display1\n");
					}
				}
				i+= j;
				if (i % 128 != 0)builder.append("drawflush display1\n");
				builder.append(codeEnd.replace("$", String.valueOf(usedProcs-index)));
				// TODO: Allow for other processors
				LogicBuild lBuild = (LogicBuild) Blocks.microProcessor.newBuilding();
				lBuild.tile = new Tile(x, y);
				lBuild.updateCode(builder.toString());
				lBuild.links.add(new LogicBlock.LogicLink(this.x, this.y, "display1", true));
				// TODO: Allow for other processors
				Stile stile = new Stile(Blocks.microProcessor, x, y, lBuild.config(), (byte) 0);
				tileArray.add(stile);
				if (i >= shapes.size-1) {
					Log.info("@ Finished with @ processors", this, index+1);
					tileArray.add(new Stile(display, this.x, this.y, null, (byte) 0));
					return;
				}
			}
			Log.warn("Failed to fit @ shapes into @ processors", shapes.size, this.points.size);
		}

		public int maxPoints() {
			// TODO
			int count = 0;
			for (int x = 0; x < SchemBuilder.this.width; x++) {
				for (int y = 0; y < SchemBuilder.this.height; y++) {
					if (within(x, y) && (!occupied(x, y) || this.points.contains(Point2.pack(x, y)))) count++;
				}
			}
			return count;
		}

		@Override
		public String toString() {
			return "Display{" +
					"x=" + x +
					", y=" + y +
					'}';
		}
	}
}
