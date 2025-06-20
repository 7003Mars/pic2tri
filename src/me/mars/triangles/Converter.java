package me.mars.triangles;

import arc.Core;
import arc.graphics.Pixmap;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.*;
import arc.util.Log;
import arc.util.OS;
import arc.util.Threads;
import me.mars.triangles.schematics.SchematicHandler;
import me.mars.triangles.shapes.Shape;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.game.Schematic.Stile;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.LogicDisplay;

import java.util.concurrent.*;

public class Converter {
	private static final ExecutorService executor = Threads.executor("Image converter", OS.cores);

	public LogicBlock block;
	public LogicDisplay displayType;
	public String name;
	public final int seed;
	public int xChunks, yChunks;
	private Pixmap source;
	private SchemBuilder filler;

	public Seq<Generator> generators = new Seq<>();
	private Seq<Future<Seq<Shape>>> tasks = new Seq<>();

	public Converter(Pixmap img, int width, int height, SchemBuilder filler, LogicBlock block, LogicDisplay display, int seed, String name) {

		this.xChunks = Mathf.ceilPositive((float) width/display.displaySize);
		this.yChunks = Mathf.ceilPositive((float) height/display.displaySize);
		this.seed = seed;

		this.name = name.isEmpty() ? "!!name me" : name;

		this.block = block;
		this.displayType = display;
		this.filler = filler;
		// Scale pixmap down
		Pixmap resized = new Pixmap(width, height);
		resized.draw(img, 0, 0, width, height, true);
		img.dispose();
		this.source = resized.flipY();
		resized.dispose();
	}

	/**
	 * Warning, disposes the source
	 * @param options
	 */
	public Converter submit(Seq<Generator.GenOpts> options) {
		if (options.size != this.xChunks * this.yChunks) {
			throw new IllegalArgumentException("Option array does not match size: " + options.size + "!=" + this.xChunks*this.yChunks);
		}
		for (int y = 0; y < this.yChunks; y++) {
			for (int x = 0; x < this.xChunks; x++) {
				Pixmap cropped = new Pixmap(this.displayRes(), this.displayRes());
				int xOffset = this.displayRes() * x;
				int yOffset = this.displayRes() * y;
				cropped.draw(this.source, xOffset, yOffset, displayRes(), displayRes(), 0, 0, displayRes(), displayRes(), true);
				Generator generator = new Generator(cropped, this, options.get(y * this.xChunks + x), true);
				Log.debug("Submitted generator @", generator);
				this.tasks.add(executor.submit(generator));
				this.generators.add(generator);
			}
		}
		this.source.dispose();
		this.source = null;
		return this;
	}

	public void cancel() {
		this.tasks.each(task -> task.cancel(true));
	}

	public boolean complete() {
		return !this.generators.contains(generator -> generator.getState() != Generator.GenState.Done);
	}

	public void build() {
		if (this.tasks.contains(task -> !task.isDone())) {
			Log.err("Generators not finished");
			return;
		}
		// TODO: Ugly
		Seq<Seq<Shape>> shapesSeq = new Seq<>();
		shapesSeq.addAll(this.tasks.map(task -> {
			try {
				return task.get();
			} catch (InterruptedException ignored) {
			} catch (ExecutionException e) {
				Log.err(e);
			}
			return new Seq<>();
		}));
		// Reduce points required (if possible)
		for (int index = 0; index < this.generators.size; index++) {
			this.filler.displays.get(index).getProcs(SchemBuilder.fitProcs(shapesSeq.get(index).size));
		}
		SchemBuilder builder = this.filler.rebuild();
		// Build schematic
		Seq<Stile> outTiles = new Seq<>();
		Seq<Point2> displayPositions = new Seq<>();
		for (int i = 0; i < shapesSeq.size; i++) {
			Seq<Shape> shapes = shapesSeq.get(i);
			SchemBuilder.Display display = builder.displays.get(i);
			display.build(shapes, outTiles, this.displayType);
			displayPositions.add(new Point2(display.x, display.y));
		}
		SchemBuilder.Display first = builder.displays.first();
		displayPositions.each(p -> p.sub(first.x, first.y));
		int offset = this.displayType.sizeOffset;
		if (Core.settings.getBool(PicToTri.setting("add-metadata"))) {
			outTiles.add(SchematicHandler.anchorBlock.generateStile(first.x+offset, first.y+offset,this.displayType, displayPositions));
		}
		StringMap tags = new StringMap();
		tags.put("name", this.name);
		Schematic schem = new Schematic(outTiles, tags, builder.width, builder.height);
		schem.labels.add(Core.bundle.get(PicToTri.setting("mod-name")));
		Vars.schematics.add(schem);
	}

	public float totalProgress() {
		int cur = 0, total = 0;
		for (Generator gen : this.generators) {
			cur+= gen.getState() == Generator.GenState.Done ? gen.getMaxGen() : gen.cur();
			total+= gen.getMaxGen();
		}
		return (float) cur/total;
	}


	int displayRes() {
		return this.displayType.displaySize;
	}
}
