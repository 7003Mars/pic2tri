package me.mars.triangles;

import arc.Core;
import arc.graphics.Pixmap;
import arc.math.Mathf;
import arc.struct.*;
import arc.util.Log;
import arc.util.OS;
import arc.util.Threads;
import arc.util.Time;
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
	public LogicDisplay display;
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
		this.display = display;
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
				Pixmap cropped = new Pixmap(this.display.displaySize, this.display.displaySize);
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
		SchemBuilder builder = this.filler.rebuild();
		Seq<Stile> outTiles = new Seq<>();
		for (int i = 0; i < this.tasks.size; i++) {
			Seq<Shape> shapes;
			try {
				shapes = this.tasks.get(i).get();
			} catch (InterruptedException ignored) {
				continue;
			} catch (ExecutionException e) {
				Log.err(e);
				continue;
			}
			builder.displays.get(i).build(shapes, outTiles, this.display);
		}
		StringMap tags = new StringMap();
		tags.put("name", this.name);
		Schematic schem = new Schematic(outTiles, tags, builder.width, builder.height);
		schem.labels.add(Core.bundle.get(PicToTri.setting("mod-name")));
		Vars.schematics.add(schem);
		// TODO: Proper generator
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
		return this.display.displaySize;
	}
}
