package me.mars.triangles.converter;

import arc.files.Fi;
import arc.func.Prov;
import arc.graphics.Pixmap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import me.mars.triangles.Generator;
import me.mars.triangles.layout.Layout;
import me.mars.triangles.layout.TiledGifLayout;
import me.mars.triangles.shapes.Shape;

import java.util.concurrent.CompletableFuture;

import static me.mars.triangles.layout.LogicDisplayLayout.procRange;

public class GifConverter extends Converter {
    Generator.GenOpts opts = new Generator.GenOpts(175, 128, 20, true); // TODO maxOut should be left to default constructor

    Seq<Fi> files;
    Seq<Seq<Shape>> results = new Seq<>();

    public GifConverter(Layout<?> layout, Fi filePath) {
        super(layout, filePath);
        validateLayout(layout, filePath);
        Fi dir = filePath.parent();
        this.files = dir.findAll(f -> f.extension().equals(filePath.extension()));
    }

    public static void validateLayout(Layout<?> layout, Fi path) {
        if (!(layout instanceof TiledGifLayout)) {
            throw new UnsupportedLayoutException("Unsupported layout", true);
        }
        Pixmap pixmap = new Pixmap(path);
        int originalWidth = pixmap.width, originalHeight = pixmap.height;
        pixmap.dispose();
        Fi dir = path.parent();
        Seq<Fi> files = dir.findAll(f -> f.extension().equals(path.extension()));
        for (Fi file : files) {
            pixmap = new Pixmap(file);
            int width = pixmap.width, height = pixmap.height;
            pixmap.dispose();
            if (width != originalWidth || height != originalHeight) {
                throw new UnsupportedLayoutException(Strings.format("The image @ has size (@, @) but target has size (@, @)",
                        width, height, originalWidth, originalHeight));
            }
        }
    }

    @Override
    public ConverterTask submit() {
        files.sortComparing(Fi::nameWithoutExtension);
        Fi firstFrame = files.first();
        files.add(firstFrame); // Try to get back to the first frame from the last
        ImageConverter imageConverter = new ImageConverter(this.layout, firstFrame);
        files.remove(firstFrame);
        imageConverter.options.each(opt -> opt.retainPixmap = true);
        ConverterTask imageConverterTask = imageConverter.submit();
        CompletableFuture<GeneratorResult> result = imageConverterTask.results.thenApply(results -> {
            // Merge all pixmaps back into one, oh god its hard
            Pixmap merged = new Pixmap(this.layout.imageWidth, this.layout.imageHeight);
            int i = 0;
            for (Layout.ImageChunk<?> chunk : this.layout.chunks) {
                // TODO Dupe code from ImageConverter, refactor.
                int iw = (int) (chunk.width * (layout.imageWidth / layout.imageBounds.width));
                int ih = (int) (chunk.height * (layout.imageHeight / layout.imageBounds.height));
                /*TODO Lazy hack to get the correct pixel position, ideally doesn't assume all layout displays start at procRange */
                int ix = (int) ((chunk.chunkX - procRange) * (layout.imageWidth / layout.imageBounds.width));
                int iy = (int) ((chunk.chunkY - procRange) * (layout.imageHeight / layout.imageBounds.height));
                merged.draw(imageConverterTask.generators.get(i++).getResult(), 0, 0, iw, ih, ix, iy, iw, ih);
            }
            synchronized (this) {
                Log.info("Adding @ chunks", results.size);
                this.results.add(results);
            }
            return new GeneratorResult(new Seq<>(), 0, merged);
        });
        for (Fi file : this.files) {
            Pixmap frame = new Pixmap(file);
            Pixmap flipped = frame.flipY();
            frame.dispose();
            Pixmap resized = new Pixmap(this.layout.imageWidth, this.layout.imageHeight);
            resized.draw(flipped, 0, 0, resized.width, resized.height, true);
            Prov<Pixmap> pixmapProv = Converter.saveTmpPixmap(resized);
            result = result.thenCompose(res -> {
                synchronized (this) {
                    this.results.add(res.shapes);
                }
                // TODO REMOVEME
                new Fi("gif/prev-"+ file.name()).writePng(res.result);
                return processFrame(pixmapProv, res.result);
            });
        }
        return new GifConverterTask(
                this,
                result.thenApply(ignored -> {
                    Seq<Seq<Shape>> res = new Seq<>();
                    res.add(this.results);
                    return res;
                }),
                imageConverterTask.generators

        );
    }

    CompletableFuture<GeneratorResult> processFrame(Prov<Pixmap> frame, Pixmap prev) {
        CompletableFuture<GeneratorResult> fresh = CompletableFuture.supplyAsync(() -> {
            Generator gen = new Generator(frame, opts);
            Seq<Shape> shapes = gen.start();
            return new GeneratorResult(shapes, gen.acc(), gen.getResult());
        }, Converter.executor);
        CompletableFuture<GeneratorResult> continued = CompletableFuture.supplyAsync(() -> {
            Generator gen = new Generator(frame, () -> prev,opts);
            Seq<Shape> shapes = gen.start();
            return new GeneratorResult(shapes, gen.acc(), gen.getResult());
        }, Converter.executor);
        return fresh.thenCombine(continued, (first, second) -> {
            // Prefer the continued frame if both results tie
            if (second.acc > first.acc) {
                Log.info("Continued won: Cont: @ Fresh: @", second.acc, first.acc);
                first.result.dispose();
                return second;
            } else {
                Log.info("Fresh won: Cont: @ Fresh: @", second.acc, first.acc);
                second.result.dispose();
                return first;
            }
        });
    }


    private static class GeneratorResult {
        Seq<Shape> shapes;
        float acc;
        Pixmap result;

        public GeneratorResult(Seq<Shape> shapes, float acc, Pixmap result) {
            this.shapes = shapes;
            this.acc = acc;
            this.result = result;
        }
    }

    private static class GifConverterTask extends ConverterTask {
        private Seq<GeneratorProgress> genProg = new Seq<>();

        public GifConverterTask(Converter converter, CompletableFuture<Seq<Seq<Shape>>> results, Seq<Generator> generators) {
            super(converter, results, generators);
            for (int i = 0; i < 2; i++) {
                this.genProg.add(new GeneratorProgress());
            }

        }

        @Override
        public float progress() {
            return (float) ((GifConverter) this.converter).results.size / ((GifConverter) this.converter).files.size;
        }

        @Override
        public Seq<GeneratorProgress> genProg() {
            return new Seq<>(); // TODO
        }
    }
}
