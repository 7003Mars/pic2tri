package me.mars.triangles;

import arc.func.Prov;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.WindowedMean;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import me.mars.triangles.shapes.FillShape;
import me.mars.triangles.shapes.Shape;
import me.mars.triangles.shapes.Triangle;

import java.util.concurrent.atomic.AtomicInteger;

public class Generator {
	private static final int Max_Age = 250, Shape_Tries = 250;

	public final int alpha;
	private GenState state = GenState.Ready;
	private WindowedMean timings = new WindowedMean(50);

	public AtomicInteger generation = new AtomicInteger();
	public volatile int maxGen;
	private volatile long curRaw = Long.MAX_VALUE;
	public final float targetAcc;

    private Triangle prevState = new Triangle();
	private final Rand rand;
    public final int maxRange;
    public final int maxOut;

    private Prov<Pixmap> pixmapProv;
	public Pixmap original;
    private Prov<Pixmap> continueProv;
    private boolean retainPixmap;
	private MutateMap mutated;

	private Seq<Shape> history = new Seq<>();

    private Generator(GenOpts options) {
        this.alpha = options.alpha;
        this.targetAcc = options.targetAcc;
        this.maxGen = options.maxGen;
        this.rand = new Rand(options.seed);
        this.maxRange = options.maxRange;
        this.maxOut = options.maxOut;
        this.retainPixmap = options.retainPixmap;
    }

	public Generator(Pixmap image, GenOpts options) {
        this(options);
        this.original = image;
        this.mutated = new MutateMap(image);
	}

    public Generator(Prov<Pixmap> pixmapProv, GenOpts options) {
        this(options);
        this.pixmapProv = pixmapProv;
    }

    // TODO Constructor for non-prov version too

    public Generator(Prov<Pixmap> pixmapProv, Prov<Pixmap> continueProv, GenOpts options) {
        this(pixmapProv, options);
        this.continueProv = continueProv;
    }

	private void prepare() {
        if (this.pixmapProv != null) {
            this.original = this.pixmapProv.get();
            this.mutated = new MutateMap(this.original);
        }
        if (this.continueProv != null) {
            this.mutated.draw(this.continueProv.get());
        } else {
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
            FillShape fill = new FillShape(r, g, b);
            fill.mutate(this, this.rand);
            history.add(fill);
        }
        this.curRaw = this.mutated.fullDiff();
		this.generation.getAndIncrement();
	}

	public Seq<Shape> start() {
		synchronized (this) {
			if (this.state != GenState.Ready) throw new IllegalStateException("Generator either started or done");
			this.state = GenState.Started;
			this.timings.add(Time.time);
		}
        this.prepare();
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
		Log.debug("Generator @ Finished with @/@ shapes", this, history.size, this.maxGen);
		// TODO: Dispose the mutated pixmap too
//		new Fi("gen-"+Mathf.random(1000)+".png").writePng(this.mutated);
        if (!this.retainPixmap) {
            this.mutated.dispose();
        }
		this.original.dispose();
        synchronized (this) {
            this.state = GenState.Done;
        }
		return this.history;
	}

    public Pixmap getResult() {
        synchronized (this) {
            if (this.state != GenState.Done) throw new IllegalStateException("Generator not done");
        }
        if (this.mutated.isDisposed()) return null;
        Pixmap copy = this.mutated.copy();
        this.mutated.dispose();
        return copy;
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

	public int getMaxGen() {
		return this.maxGen;
	}

	public int cur() {
		return this.generation.get();
	}

	public synchronized float rate() {
		if (this.state != GenState.Started) return 0;
		return this.timings.getCount() / (Time.time - this.timings.oldest());
	}

	public synchronized float timeToCompletion() {
		if (this.state != GenState.Started) return -1;
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
        public int seed;
        public int maxGen;
        public boolean retainPixmap = false;
        public float targetAcc = 0.99f;
        public final int alpha;
        public int maxRange = 20;
        public int maxOut = 16;

		public GenOpts(int alpha, int maxGen) {
			this.alpha = Mathf.clamp(alpha, 0, 255);
			this.maxGen = maxGen;
		}

        public GenOpts(int alpha, int maxGen, int maxOut, boolean retainPixmap) {
            this(alpha, maxGen);
            this.maxOut = maxOut;
            this.retainPixmap = retainPixmap;
        }
	}

	public enum GenState {
        Ready, Started, Done
	}
}
