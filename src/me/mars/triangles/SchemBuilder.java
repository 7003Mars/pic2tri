package me.mars.triangles;

import arc.Core;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.struct.*;
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
	private static final String codeStart = """
			sensor e display1 @enabled
			jump 0 equal e 1
			set t @tick
			jump $ equal t @tick
			control enabled display1 1 0 0 0
			""";
	private static final String[] codeStartArray = Seq.with(codeStart.split("\n"))
			.map(line -> line+"\n").toArray(String.class);

	private static final String repeat = """
			set t @tick
			jump $ equal t @tick
			""";
	private static final String[] repeatArray = Seq.with(repeat.split("\n"))
			.map(line -> line+"\n").toArray(String.class);

	static final String codeStartSingle = """
			sensor e display1 @enabled
			jump 0 equal e 1
			control enabled display1 1 0 0 0
			""";

	public static final int startSize, repeatSize;
	public static final int Max_Shapes_Single;
	static {
		startSize = codeStartArray.length;
		repeatSize = repeatArray.length;
		int freeInstructions = LExecutor.maxInstructions - Strings.count(codeStartSingle, "\n");
		freeInstructions-= Mathf.ceilPositive(freeInstructions/256f);
		Max_Shapes_Single = freeInstructions/2;
//		Max_Shapes_Single = 128;
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
		float midX = (this.width+1) /2f, midY = (this.height+1) /2f;
		this.displays.copy().sort(display -> Mathf.dst2(display.x, display.y, midX, midY))
				.each(display -> {
					display.obtain(1);
				});
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

	public SchemBuilder rebuild() {
		SchemBuilder replacement = new SchemBuilder(this.xChunks, this.yChunks,
				this.procRange, this.dispSize);
		float midX = (this.width+1) /2f, midY = (this.height+1) /2f;
		boolean failed = replacement.displays.copy().sort(display -> Mathf.dst2(display.x, display.y, midX, midY))
				.contains(display -> {
					Display selfDisplay = this.displays.find(d -> d.x == display.x && d.y == display.y);
					 display.obtain(selfDisplay.points.size);
					 return display.points.size != selfDisplay.points.size;
				});
		Log.info("Rebuild " + (failed ? "failed" : "succeeded"));
		return failed ? this : replacement;
	}

	public static int maxShapes(int procIndex) {
		int free = (LExecutor.maxInstructions-startSize) - (procIndex*repeatSize*2);
		int maxShapes = Mathf.floorPositive(free/2f);
		maxShapes-= Mathf.ceilPositive(maxShapes/128f);
		return maxShapes;
	}

	public static int totalShapes(int procCount) {
		if (procCount == 0) return 0;
		return maxShapes(procCount-1)*procCount;
//		int sum = 0;
//		for (int i = 0; i < procCount; i++) {
//			sum+= maxShapes(i);
//		}
//		return sum;
	}

	public static int fitProcs(int shapes) {
		// lazy solution.
		for (int i = 1; i < 30; i++) {
			if (totalShapes(i) >= shapes) return i;
		}
		return 29;
	}

	public class Display extends Generator.GenOpts {
		int x, y;
		IntSet pointSet = new IntSet();
		public IntSeq points = new IntSeq();

		public Color color = Color.HSVtoRGB(Mathf.random(360), 100, 100);

		public Display(int x, int y) {
			super(175, SchemBuilder.Max_Shapes_Single);
			this.x = x;
			this.y = y;
		}

		private int obtain(int target) {
			int dir = 0;
			int x = this.x, y = this.y;
			int segLen = 0, maxSegLen = 1;
			int range2 = SchemBuilder.this.procRange*2 + SchemBuilder.this.dispSize;
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
			target = Math.min(target, 29);
			int obtained = target > this.points.size ? this.obtain(target) : this.free(target);
			this.maxGen = obtained == 1 ? Max_Shapes_Single : totalShapes(obtained);
			return obtained;
		}

		public boolean within(int x, int y) {
			float cx = this.x + ((SchemBuilder.this.dispSize + 1) %2) /2f;
			float cy = this.y + ((SchemBuilder.this.dispSize + 1) %2) /2f;

			float range2 = SchemBuilder.this.procRange + SchemBuilder.this.dispSize/2f;
			range2*= range2;
			return Mathf.dst2(cx, cy, x, y) < range2;
		}

		public void build(Seq<Shape> shapes, Seq<Stile> tileArray, LogicDisplay display) {
			if (shapes.size <= Max_Shapes_Single) {
				this.buildSingle(shapes, tileArray, display);
				return;
			}
			if (this.points.size > 29) {
				Log.err("Logic for @ won't work with >29 processors", this);
				return;
			}
			if (totalShapes(this.points.size) < shapes.size) {
				Log.info("Not enough processors: @/@", totalShapes(this.points.size), shapes.size);
			}
			// Maps a position to a Seq of shapes
			OrderedMap<Integer,Seq<Shape>> shapeMapping = new OrderedMap<>();
			for (int i = 0; i < this.points.size; i++) {
				shapeMapping.put(this.points.get(i), new Seq<>());
			}
			int shapeCount = 0;
			done:
			while (true) {
				int remaining = shapes.size - shapeCount;
				int maxObtain = Mathf.ceilPositive((float)remaining/this.points.size);
				maxObtain = Math.min(maxObtain, 128);
				for (Seq<Shape> procShapes: shapeMapping.values()) {
					int c = 0;
					// TODO: horrible code everywhere, refactor pls
					while (c < maxObtain) {
						procShapes.add(shapes.get(shapeCount));
						shapeCount++;
						if (shapeCount >= shapes.size) break done;
						c++;
					}
				}
			}
			// Calculate the maximum number of shapes for each processor
			int maxFilled = shapeMapping.values().toSeq().max(seq -> seq.size).size;
			if (maxFilled > maxShapes(this.points.size-1)) {
				Log.err("Some processor(s) have > max shapes @/@", maxShapes(this.points.size-1), maxFilled);
				for (Seq<Shape> seq : shapeMapping.values()) {
					Log.warn("Size: @", seq.size);
				}
				return;
			}
			Seq<String> stringArray = new Seq<>();
			int procIndex = 0;
			for (var entry : shapeMapping) {
				int pos = entry.key;
				Seq<Shape> procShapes = entry.value;
				stringArray.clear();
				// Add initial code
				stringArray.addAll(codeStartArray);
				for (int repeat = 0; repeat < procIndex; repeat++) {
					stringArray.addAll(repeatArray);
					stringArray.addAll(repeatArray);
				}
				// Actual shapes
				int count = 0;
				for (; count < procShapes.size; count++) {
					stringArray.add(procShapes.get(count).toInstr());
					if ((count+1) % 128 == 0 && count != 0 && count != procShapes.size-1) stringArray.add("drawflush display1\n");
				}
				// Add padding
				for (int padCount = 0; padCount < maxFilled-count; padCount++) {
					stringArray.add("print \"Padding\"\n");
					stringArray.add("print \"Padding\"\n");
				}
				// Then do the final flush if needed
				if (count == 1 || count == procShapes.size || count % 128 != 0) stringArray.add("drawflush display1\n");
				// Fill in the jump index
				for (int i = 0; i < stringArray.size; i++) {
					stringArray.set(i, stringArray.get(i).replace("$", String.valueOf(i)));
				}
				// Build tiles
//				Log.info("building with size @", stringArray.size);
				tileArray.add(this.fillCode(pos, String.join("", stringArray)));
				Core.app.setClipboardText(String.join("", stringArray));
				// End, increment index
				procIndex++;
			}
			tileArray.add(new Stile(display, this.x, this.y, null, (byte) 0));
		}

		private void buildSingle(Seq<Shape> shapes, Seq<Stile> tileArray, LogicDisplay display) {
			StringBuilder builder = new StringBuilder();
			builder.append(codeStartSingle);
			int i = 0;
			for (; i < shapes.size; i++) {
				builder.append(shapes.get(i).toInstr());
				if (i != 0 && (i+1) % 128 == 0) builder.append("drawflush display1\n");
			}
			if (i % 128 != 0 || i == 0) builder.append("drawflush display1\n");
			tileArray.add(this.fillCode(this.points.first(), builder));
			tileArray.add(new Stile(display, this.x, this.y, null, (byte) 0));
		}

		public Stile fillCode(int pos, CharSequence code) {
			// TODO: Allow for other processors
			int x = Point2.x(pos);
			int y  = Point2.y(pos);
			LogicBuild lBuild = (LogicBuild) Blocks.microProcessor.newBuilding();
			lBuild.tile = new Tile(x, y);
			lBuild.updateCode(code.toString());
			lBuild.links.add(new LogicBlock.LogicLink(this.x, this.y, "display1", true));
			return new Stile(Blocks.microProcessor, x, y, lBuild.config(), (byte) 0);
		}

		public int maxPoints() {
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
