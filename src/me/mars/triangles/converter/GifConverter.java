package me.mars.triangles.converter;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.struct.Seq;
import arc.util.Log;
import me.mars.triangles.generation.Generator;
import me.mars.triangles.layout.Layout;
import me.mars.triangles.layout.TiledGifLayout;
import me.mars.triangles.shapes.Shape;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static me.mars.triangles.PicToTri.internalName;
import static me.mars.triangles.layout.LogicDisplayLayout.procRange;

public class GifConverter extends Converter {
    Generator.GenOpts opts = new Generator.GenOpts(0/*TODO Ideally this should be configurable? idk*/, 175, TiledGifLayout.WORKER_MAX_FRAME_SHAPES).maxOut(20).retainPixmap(true); // TODO maxOut should be left to default constructor

    Seq<Fi> files;

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
            throw new UnsupportedLayoutException(Core.bundle.format(internalName+".converter.gif.errors.min-frame", minFrames));
        }
        for (Fi file : files) {
            ImageSize imageSize = ImageSize.getSize(file);
            if (imageSize.width != originalSize.width || imageSize.height != originalSize.height) {
                throw new UnsupportedLayoutException(Core.bundle.format(internalName+".converter.gif.errors.inconsistent-size",
                        originalSize.width, originalSize.height, file.absolutePath(), imageSize.width, imageSize.height));
            }
        }
    }

    @Override
    public ConverterTask submit() {
        Fi firstFrame = files.first();
        files.remove(0);
        files.add(firstFrame); // Try to get back to the first frame from the last
        ImageConverter imageConverter = new ImageConverter(this.layout, firstFrame);
        imageConverter.options.each(opt -> opt.retainPixmap(true));
        ConverterTask imageConverterTask = imageConverter.submit();
        Seq<Seq<Shape>> results = new Seq<>();
        CompletableFuture<Generator.GenerationOutput> prevBest = imageConverterTask.getChunks().thenApply(chunks -> {
            // Merge all pixmaps back into one, oh god its hard
            Pixmap merged = new Pixmap(this.layout.imageWidth, this.layout.imageHeight);
            for (int i = 0; i < this.layout.chunks.size; i++) {
                Layout.ImageChunk<?> chunk = this.layout.chunks.get(i);
                // TODO Dupe code from ImageConverter, refactor.
                int iw = (int) (chunk.width * (layout.imageWidth / layout.imageBounds.width));
                int ih = (int) (chunk.height * (layout.imageHeight / layout.imageBounds.height));
                /*TODO Lazy hack to get the correct pixel position, ideally doesn't assume all layout displays start at procRange */
                int ix = (int) ((chunk.chunkX - procRange) * (layout.imageWidth / layout.imageBounds.width));
                int iy = (int) ((chunk.chunkY - procRange) * (layout.imageHeight / layout.imageBounds.height));
                Pixmap cropped = imageConverterTask.results.get(i).output.getNow(null).result();
                merged.draw(cropped, 0, 0, iw, ih, ix, iy, iw, ih);
                cropped.dispose();
            }
            synchronized (this) {
                Log.debug("Adding @ chunks", chunks.size);
                results.add(chunks);
            }
            // TODO This is rather hacky, not sure how to refactor this tho.
            return new Generator.GenerationOutput(new Seq<>(), merged, 0, 0,0);
        });
        int i = 0;
        // Ui stuff
        AtomicInteger processedFramesCount = new AtomicInteger(results.size);
        for (Fi file : this.files) {
            Pixmap frame = new Pixmap(file);
            Pixmap flipped = frame.flipY();
            Pixmap resized = new Pixmap(this.layout.imageWidth, this.layout.imageHeight);
            resized.draw(flipped, 0, 0, resized.width, resized.height, true);
            frame.dispose();
            flipped.dispose();
            prevBest = processFrame(resized, prevBest, i++);
            prevBest.thenAccept(out -> {
                results.add(out.shapes());
                processedFramesCount.getAndIncrement();
            });
        }
        return new GifConverterTask(
                this,
                prevBest.thenApply(ignored -> results),
                processedFramesCount
        );
    }

    CompletableFuture<Generator.GenerationOutput> processFrame(Pixmap frame, CompletableFuture<Generator.GenerationOutput> prev, int frameNumber) {
        ImageGenerationService.GenerationTaskResult freshResult = ImageGenerationService.submitImage(opts, frame.copy(), null, frameNumber, this);
        CompletableFuture<Generator.GenerationOutput> fresh = freshResult.output;
        CompletableFuture<Generator.GenerationOutput> continued = prev.thenCompose(prevOutput -> {
            ImageGenerationService.GenerationTaskResult continuedRes = ImageGenerationService.submitImage(opts, frame, prevOutput.result(), 0, this);
            return continuedRes.output;
        });
        return fresh.thenCombine(continued, (freshOut, continuedOut) -> {
            if (freshOut.acc() > continuedOut.acc()) {
                continuedOut.result().dispose();
                return freshOut;
            } else {
                freshOut.result().dispose();
                return continuedOut;
            }
        });
    }

    private static class GifConverterTask extends ConverterTask {
        // Ui stuff
        private AtomicInteger frameCount;

        CompletableFuture<Seq<Seq<Shape>>> chunksFuture;
        GifConverter gifConverter;

        public GifConverterTask(Converter converter, CompletableFuture<Seq<Seq<Shape>>> chunksFuture, AtomicInteger frameCount) {
            super(converter, new Seq<>()); // We only know the GeneratorResults for fresh frames as the continued Frames are submitted lazily.
            this.chunksFuture = chunksFuture;
            this.frameCount = frameCount;

            this.gifConverter = (GifConverter) converter;
        }


        @Override
        public float progress() {
            return (float) frameCount.get() / gifConverter.files.size;
        }

        @Override
        public CompletableFuture<Seq<Seq<Shape>>> getChunks() {
            return this.chunksFuture;
        }

        @Override
        public Seq<ImageGenerationService.GenerationTaskResult> taskProgView() {
            return ImageGenerationService.getActiveTasks().retainAll(task -> task.owner == this.converter).as();
        }
    }
}
