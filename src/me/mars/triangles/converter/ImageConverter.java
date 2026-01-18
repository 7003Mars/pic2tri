package me.mars.triangles.converter;

import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.struct.Seq;
import me.mars.triangles.generation.Generator;
import me.mars.triangles.layout.Layout;
import me.mars.triangles.layout.LogicDisplayLayout;
import me.mars.triangles.layout.TiledDisplayLayout;
import me.mars.triangles.shapes.Shape;

import java.util.concurrent.CompletableFuture;

import static me.mars.triangles.layout.LogicDisplayLayout.procRange;

public class ImageConverter extends Converter {
    public Seq<Generator.GenOpts> options = new Seq<>();

    public ImageConverter(Layout<?> layout, Fi filePath) {
        super(layout, filePath);
        validateLayout(layout);
        for (Layout.ImageChunk<?> chunk : this.layout.chunks) {
            int totalShapes = -1;
            if (chunk.data instanceof LogicDisplayLayout.ChunkData data) {
                totalShapes = LogicDisplayLayout.totalShapes(data.procs.size);
            } else if (chunk.data instanceof TiledDisplayLayout.ChunkData data) {
                totalShapes = TiledDisplayLayout.totalShapes(data.procs);
            }
            if (layout instanceof LogicDisplayLayout) {
                this.options.add(new Generator.GenOpts(0/*TODO*/, 175, totalShapes));
            } else {
                // TODO VERY IMPT
                /*
                This is a VERY bandaid fix to avoid seams between chunks.
                Vertices with sharp edges often fail to draw, leading to the top and right sides of each chunk having a distinct area of no activity.
                The proper fix would be to implement the top-left rasterisation check for bounds checks during mutations, allowing pixels to actually fill the whole space.

                As of right now, we just extend maxout to 1.
                This will likely lead to inaccuracies between what is generated and what is actually rendered, as triangles with flat edges that exceed their chunks can draw into other chunks
                */
                this.options.add(new Generator.GenOpts(0/*TODO*/, 175, totalShapes).maxOut(1));
            }
        }
    }

    public static void validateLayout(Layout<?> layout) {
        if (layout instanceof LogicDisplayLayout logicDisplayLayout) {
            for (Layout.ImageChunk<LogicDisplayLayout.ChunkData> chunk : logicDisplayLayout.chunks) {
                if (chunk.data.procs.size > LogicDisplayLayout.MAX_PROCS) {
                    throw new UnsupportedLayoutException("Converter only supports" + LogicDisplayLayout.MAX_PROCS +  "processors per chunk");
                }
            }
        } else if (layout instanceof TiledDisplayLayout tiledDisplayLayout) {
            if (tiledDisplayLayout.chunks.sum(chunk -> chunk.data.procs) > LogicDisplayLayout.MAX_PROCS) {
                throw new UnsupportedLayoutException("Converter only supports" + LogicDisplayLayout.MAX_PROCS + "processors in total");
            }
        } else {
            throw new UnsupportedLayoutException("Unsupported layout", true);
        }
    }

    @Override
    public ConverterTask submit() {
        // Resize and flip pixmap
        Pixmap origin = new Pixmap(this.filePath);
        Pixmap flipped = origin.flipY();
        Pixmap resized = new Pixmap(this.layout.imageWidth, this.layout.imageHeight);
        resized.draw(flipped, 0, 0, resized.width, resized.height, true);
        origin.dispose();
        flipped.dispose();
        Seq<ImageGenerationService.GenerationTaskResult> taskResults = new Seq<>();
        for (int i = 0; i < this.layout.chunks.size; i++) {
            Layout.ImageChunk<?> chunk = this.layout.chunks.get(i);
            // TODO possible precision loss here, check code again
            int iw = (int) (chunk.width * (layout.imageWidth/layout.imageBounds.width));
            int ih = (int) (chunk.height * (layout.imageHeight/layout.imageBounds.height));
            Pixmap cropped = new Pixmap(iw, ih);
            /*TODO Lazy hack to get the correct pixel position, ideally doesn't assume all layout displays start at procRange */
            int ix = (int) ((chunk.chunkX-procRange) * (layout.imageWidth/layout.imageBounds.width));
            int iy = (int) ((chunk.chunkY-procRange) * (layout.imageHeight/layout.imageBounds.height));
//            Log.info("Start @, @, w@ h@", ix, iy, iw, ih);
            cropped.draw(resized, ix, iy, iw, ih, 0, 0, iw, ih);
            ImageGenerationService.GenerationTaskResult genResult = ImageGenerationService.submitImage(options.get(i), cropped, null, 0, this);
            taskResults.add(genResult);
        }
        resized.dispose();
        return new ImageConverterTask(
                this, taskResults
        );
    }

    protected static class ImageConverterTask extends ConverterTask {
        public ImageConverterTask(ImageConverter converter, Seq<ImageGenerationService.GenerationTaskResult> taskResults) {
            super(converter, taskResults);
        }

        @Override
        public CompletableFuture<Seq<Seq<Shape>>> getChunks() {
            return CompletableFuture.allOf(this.results.map(res -> res.output).toArray(CompletableFuture.class))
                    .thenApply(ignored -> this.results.map(res -> res.output.join().shapes()));
        }

        @Override
        public float progress() {
            return (float) this.results.count(gen -> gen.state() == Generator.GenState.Done) /this.results.size;
        }

        @Override
        public Seq<ImageGenerationService.GenerationTaskResult> taskProgView() {
            return this.results;
        }
    }
}
