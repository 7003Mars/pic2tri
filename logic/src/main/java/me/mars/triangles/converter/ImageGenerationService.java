package me.mars.triangles.converter;

import arc.files.Fi;
import arc.func.Prov;
import arc.graphics.Pixmap;
import arc.struct.Seq;
import arc.util.ArcRuntimeException;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.OS;
import me.mars.triangles.generation.Generator;
import me.mars.triangles.utils.PriorityExecutor;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;

import static me.mars.triangles.utils.PriorityExecutor.priorityCallable;

public class ImageGenerationService {
    public static int MAX_THREADS = OS.cores;
    public static final ExecutorService executor = PriorityExecutor.getExecutor(MAX_THREADS, "Image generator");
    private static final Seq<ComputedImageGenerationTask> activeTasks = new Seq<>();

    public static ImageGenerationTask submitImage(Generator.GenOpts opts, Pixmap image, @Nullable Pixmap continuation, int priority, @Nullable Object owner) {
        byte[] imageHash = GeneratorCache.imageHash(image), continuationHash = GeneratorCache.imageHash(continuation);
        ImageGenerationTask cachedResult = GeneratorCache.getCache(opts, imageHash, continuationHash);
        if (cachedResult != null) {
            image.dispose();
            if (continuation != null) continuation.dispose();
            return cachedResult;
        }
        Generator generator = opts.build();
        Prov<Pixmap> imageProv = saveTmpPixmap(image);
        @Nullable Prov<Pixmap> continuationProv = continuation != null ? saveTmpPixmap(continuation) : null;
        CompletableFuture<Generator.GenerationOutput> future = new CompletableFuture<>();
        ComputedImageGenerationTask taskResult = new ComputedImageGenerationTask(future, generator, owner);
        executor.submit(priorityCallable(priority, () -> {
            try {
                synchronized (activeTasks) {
                    activeTasks.add(taskResult);
                }
                Generator.GenerationOutput output = generator.start(imageProv.get(), continuationProv != null ? continuationProv.get() : null);
                GeneratorCache.setCache(opts, imageHash, continuationHash, output);
                future.complete(output);
            } catch (Throwable t) {
                Log.err("Exception while generating image:", t);
                future.completeExceptionally(t);
            } finally {
                synchronized (activeTasks) {
                    activeTasks.remove(taskResult, true);
                }
            }
            return null; // Lazy hack since I don't wanna create a PriorityRunnable too
        }));
        return taskResult;
    }

    public static Seq<ComputedImageGenerationTask> getActiveTasks() {
        synchronized (activeTasks) {
            return new Seq<>(activeTasks);
        }
    }

    /**
    Will dispose the provided pixmap
     */
    public static Prov<Pixmap> saveTmpPixmap(Pixmap pixmap) {
        Fi tmpFile;
        try {
            tmpFile = new Fi(File.createTempFile("pic2tri", ".png"));
        } catch (IOException e) {
            throw new ArcRuntimeException(e);
        }
        tmpFile.writePng(pixmap);
        int width = pixmap.width, height = pixmap.height;
        tmpFile.file().deleteOnExit();
        pixmap.dispose();
        return () -> {
            Pixmap pix = new Pixmap(tmpFile);
            assert pix.width == width && pix.height == height;
            return pix;
        };
    }

    public static abstract class ImageGenerationTask {
        public CompletableFuture<Generator.GenerationOutput> output;

        public ImageGenerationTask(CompletableFuture<Generator.GenerationOutput> output) {
            this.output = output;
        }

        public abstract Generator.GenState state();
        public abstract float accuracy();
        public abstract int cur();
        public abstract int maxGen();
    }

    public static class ComputedImageGenerationTask extends ImageGenerationTask {
        private final Generator generator;
        public @Nullable Object owner;

        public ComputedImageGenerationTask(CompletableFuture<Generator.GenerationOutput> output, Generator generator, @Nullable Object owner) {
            super(output);
            this.generator = generator;
            this.owner = owner;
        }


        @Override
        public Generator.GenState state() {
            return this.generator.getState();
        }

        @Override
        public float accuracy() {
            return this.generator.acc();
        }

        @Override
        public int cur() {
            return this.generator.cur();
        }

        @Override
        public int maxGen() {
            return this.generator.getMaxGen();
        }
    }
}
