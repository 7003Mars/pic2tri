package me.mars.triangles.converter;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.struct.Seq;
import me.mars.triangles.generation.Generator;
import me.mars.triangles.layout.Layout;
import me.mars.triangles.layout.TiledGifLayout;

import java.util.concurrent.CompletableFuture;

import static me.mars.triangles.utils.Prefs.internalName;

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
            throw new UnsupportedLayoutException("Unsupported layout");
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
        CompletableFuture<Pixmap> imageFuture = new CompletableFuture<>();
        ConverterTask task = new ConverterTask(this, this.files.size - 1 + this.layout.chunks.size, imageFuture);
        imageConverter.options.each(opt -> opt.retainPixmap(true));
        ConverterTask imageConverterTask = imageConverter.submit();
        CompletableFuture<Generator.GenerationOutput> prevBest = imageConverterTask.result.thenApply(res -> {
            for (int i = 0; i < res.chunks.size; i++) {
                task.completeChunk(i, res.chunks.get(i));
            }
            return new Generator.GenerationOutput(new Seq<>(), res.processedImage, 0, 0, 0);
        });
        int i = 0;
        for (Fi file : this.files) {
            Pixmap frame = new Pixmap(file);
            Pixmap flipped = frame.flipY();
            Pixmap resized = new Pixmap(this.layout.imageWidth, this.layout.imageHeight);
            resized.draw(flipped, 0, 0, resized.width, resized.height, true);
            frame.dispose();
            flipped.dispose();
            prevBest = processFrame(resized, prevBest, i);
            int finalI = i + this.layout.chunks.size;
            prevBest.thenAccept(out -> {
                task.completeChunk(finalI, out.shapes());
                if (finalI == this.files.size-1) {
                    imageFuture.complete(out.result());
                } else {
                    out.result().dispose();
                }
            });
            i++;
        }
        return task;
    }

    CompletableFuture<Generator.GenerationOutput> processFrame(Pixmap frame, CompletableFuture<Generator.GenerationOutput> prev, int frameNumber) {
        ImageGenerationService.ImageGenerationTask freshResult = ImageGenerationService.submitImage(opts, frame.copy(), null, frameNumber, this);
        CompletableFuture<Generator.GenerationOutput> fresh = freshResult.output;
        CompletableFuture<Generator.GenerationOutput> continued = prev.thenCompose(prevOutput -> {
            ImageGenerationService.ImageGenerationTask continuedRes = ImageGenerationService.submitImage(opts, frame, prevOutput.result(), 0, this);
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
}
