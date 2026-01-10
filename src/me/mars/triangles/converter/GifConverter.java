package me.mars.triangles.converter;

import arc.files.Fi;
import arc.func.Prov;
import arc.graphics.Pixmap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import me.mars.triangles.generation.Generator;
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
        String ext = "." + filePath.extension();
        this.files = Seq.with(dir.list(f -> f.getName().endsWith(ext)));
        files.sortComparing(Fi::nameWithoutExtension);
    }

    public static void validateLayout(Layout<?> layout, Fi path) {
        if (!(layout instanceof TiledGifLayout)) {
            throw new UnsupportedLayoutException("Unsupported layout", true);
        }
        ImageSize originalSize = ImageSize.getSize(path);
        Fi dir = path.parent();
        String ext = "." + path.extension();
        Seq<Fi> files = Seq.with(dir.list(f -> f.getName().endsWith(ext)));
        int minFrames = ((TiledGifLayout)layout).minFrameCount();
        if (files.size < minFrames) {
            throw new UnsupportedLayoutException(Strings.format("At least @ frames are required for this frame interval", minFrames));
        }
        for (Fi file : files) {
            ImageSize imageSize = ImageSize.getSize(file);
            if (imageSize.width != originalSize.width || imageSize.height != originalSize.height) {
                throw new UnsupportedLayoutException(Strings.format("The image @ has size (@, @) but target has size (@, @)",
                        imageSize.width, imageSize.height, originalSize.width, originalSize.height));
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
                Pixmap cropped = imageConverterTask.generators.get(i++).getResult();
                merged.draw(cropped, 0, 0, iw, ih, ix, iy, iw, ih);
                cropped.dispose();
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
            Pixmap resized = new Pixmap(this.layout.imageWidth, this.layout.imageHeight);
            resized.draw(flipped, 0, 0, resized.width, resized.height, true);
            frame.dispose();
            flipped.dispose();
            Prov<Pixmap> pixmapProv = Converter.saveTmpPixmap(resized);
            result = result.thenCompose(res -> {
                synchronized (this) {
                    this.results.add(res.shapes);
                }
                // TODO REMOVEME
//                new Fi("gif/prev-"+ file.name()).writePng(res.result);
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

    CompletableFuture<GeneratorResult> processFrame(Prov<Pixmap> frameProv, Pixmap prev) {
        CompletableFuture<GeneratorResult> fresh = CompletableFuture.supplyAsync(() -> {
            Generator gen = new Generator(opts);
            Seq<Shape> shapes = gen.start(frameProv.get());
            return new GeneratorResult(shapes, gen.acc(), gen.getResult());
        }, Converter.executor);
        CompletableFuture<GeneratorResult> continued = CompletableFuture.supplyAsync(() -> {
            Generator gen = new Generator(opts);
            Seq<Shape> shapes = gen.start(frameProv.get(), prev);
            return new GeneratorResult(shapes, gen.acc(), gen.getResult());
        }, Converter.executor);
        return fresh.thenCombine(continued, (first, second) -> {
            // Prefer the continued frame if both results tie. Dispose the worse performing result's pixmap.
            if (second.acc > first.acc) {
                Log.debug("Continued won: Cont: @ Fresh: @", second.acc, first.acc);
                first.result.dispose();
                return second;
            } else {
                Log.debug("Fresh won: Cont: @ Fresh: @", second.acc, first.acc);
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
