package me.mars.triangles;

import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.WindowedMean;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Time;
import me.mars.triangles.shapes.FillShape;
import me.mars.triangles.shapes.Shape;
import me.mars.triangles.shapes.Triangle;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

public class Generator implements Callable<Seq<Shape>> {
	private static final int Max_Age = 250, Shape_Tries = 250;

	public final int alpha;
	private GenState state = GenState.Ready;
	private WindowedMean timings = new WindowedMean(50);

	public AtomicInteger generation = new AtomicInteger();
	public volatile int maxGen;
	private volatile long curRaw = Long.MAX_VALUE;
	public final float targetAcc;

	private Triangle prevState;
	private final Rand rand;

	private @Nullable Fi loadPath;
	public Pixmap original;
	private MutateMap mutated;

	private Seq<Shape> history = new Seq<>();

	public Generator(Pixmap image, Converter parent, GenOpts options, boolean write) {
		this.alpha = options.alpha;
		this.targetAcc = options.targetAcc;
		this.maxGen = options.maxGen;
		this.rand = new Rand(parent.seed);
		// TODO: Horrible idea storing the shape for rollback but oh well
		this.prevState = new Triangle();

		if (write) {
			// TODO: Might need to lock the file?
			Fi tempFile = Fi.tempFile(Integer.toHexString(this.hashCode()));
			tempFile.writePng(image);
			this.loadPath = tempFile;
			image.dispose();
		} else {
			this.original = image;
			this.mutated = new MutateMap(image);
		}
	}

	public void prepare() {
		int r = 0, g = 0, b = 0;
		for (int x = 0; x < this.original.width; x++) {
			for (int y = 0; y < this.original.height; y++) {
				int col = original.getRaw(x, y);
				r += Color.ri(col);
				g += Color.gi(col);
				b += Color.bi(col);

			}
		}
		int size = this.original.width * this.original.height;
		r = Mathf.round((float)r/size);
		g = Mathf.round((float)g/size);
		b = Mathf.round((float)b/size);
		this.mutated.fill(Color.packRgba(r, g, b, 255));
		history.add(new FillShape(r, g, b));
		this.curRaw = this.mutated.fullDiff();
		this.generation.getAndIncrement();
	}

	public Seq<Shape> start() {
		synchronized (this) {
			if (this.state != GenState.Ready) throw new IllegalStateException("Generator either started or done");
			this.state = GenState.Start;
			this.timings.add(Time.time);
		}
		while (generation.getAndIncrement() < maxGen && this.acc() < targetAcc) {
			// Stop and cleanup if interrupted
			if (Thread.currentThread().isInterrupted()) {
				synchronized (this) {
					this.state = GenState.Done;
					this.original.dispose();
					this.mutated.dispose();
				}
				return null;
			}
			Shape shape = this.getBestShape();
			long best = this.hillClimb(shape);
			if (best == 0) {
//				Log.warn("No good shape found, skipping");
				continue;
			}
			shape.fill(this.mutated);
			mutated.apply(Color.packRgba(shape.r, shape.g, shape.b, shape.a));
			long newRaw = this.mutated.fullDiff();
			if (PicToTri.debugMode && this.curRaw-newRaw != best) {
				Log.warn("Off: @ @", curRaw-newRaw, best);
			}
			if (newRaw > this.curRaw) {
				Log.warn("Produced worse image from improvement @: @", best, newRaw-this.curRaw);
			}
			if (PicToTri.debugMode && this.generation.get() % 250 == 0) Log.info("@:@ Acc: @",
					this, this.generation.get(), newRaw);
			this.curRaw = newRaw;
			history.add(shape);
			synchronized (this) {
				this.timings.add(Time.time);
			}
		}
		synchronized (this) {
			this.state = GenState.Done;
		}

		Log.debug("Generator @ Finished with @/@ shapes", this, history.size, this.maxGen);
		// TODO: Dispose the mutated pixmap too
		this.mutated.dispose();
		this.original.dispose();
		return this.history;
	}

	private Shape getBestShape() {
		Shape shape = new Triangle();
		long best = 0;
		for (int i = 0; i < Shape_Tries; i++) {
			this.mutated.drop();
			this.prevState.set(shape);
			shape.randomise(this, this.rand);
			shape.fill(this.mutated);
			int col = this.mutated.calcColor(this.alpha);
			long score = this.mutated.score2(col);
			if (score > best) {
				best = score;
			} else {
				shape.set(this.prevState);
			}
		}
		this.mutated.drop();
		return shape;
	}

	/**
	 * Mutates shape for best score
	 * @param shape Shape to mutate
	 * @return Best score
	 */
	private long hillClimb(Shape shape) {
		long best = 0, improvement;
		int color;
		int i = 0;
		while (i < Max_Age) {
			this.mutated.drop();
			this.prevState.set(shape);
			shape.mutate(this, this.rand);
			shape.fill(this.mutated);
			color = this.mutated.calcColor(this.alpha);
			improvement = this.mutated.score2(color);
			if (improvement > best) {
				i = 0;
				best = improvement;
				shape.setColor(color);
			} else {
				// If the change is worse, undo it
				shape.set(this.prevState);
				i++;
			}
		}
		// Clear the buffer
		this.mutated.drop();
		return best;
	}

	@Override
	public Seq<Shape> call() {
//		Log.info("I have been summoned");
		if (this.original == null) {
			this.original = new Pixmap(loadPath);
			this.mutated = new MutateMap(this.original);
			this.loadPath.delete();
		}
		this.prepare();
		return this.start();
	}

	public int getMaxGen() {
		return this.maxGen;
	}

	public int cur() {
		return this.generation.get();
	}

	public synchronized float rate() {
		if (this.state != GenState.Start) return 0;
		return this.timings.getCount() / (Time.time - this.timings.oldest());
	}

	public synchronized float timeToCompletion() {
		if (this.state != GenState.Start) return -1;
		int cur = this.generation.get();
		return (float) (this.maxGen - cur) / this.rate() / Time.toSeconds;
	}

	public synchronized GenState getState() {
		return this.state;
	}

	public synchronized float acc() {
		if (this.state == GenState.Ready) return 0f;
		return 1f - Mathf.sqrt((float) this.curRaw / (this.mutated.width * this.mutated.height * 4))/255f;
	}

	public static class GenOpts {
		public final int alpha;
		public int maxGen;
		public float targetAcc = 0.99f;

		public GenOpts(int alpha, int maxGen) {
			this.alpha = Mathf.clamp(alpha, 0, 255);
			this.maxGen = maxGen;
		}
	}

	public enum GenState {
		Ready, Start, Done
	}
}
