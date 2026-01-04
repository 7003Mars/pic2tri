package me.mars.triangles.converter;

import arc.files.Fi;
import arc.func.Prov;
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
                this.options.add(new Generator.GenOpts(175, totalShapes));
            } else {
                // TODO VERY IMPT
                /*
                This is a VERY bandaid fix to avoid seams between chunks.
                Vertices with sharp edges often fail to draw, leading to the top and right sides of each chunk having a distinct area of no activity.
                The proper fix would be to implement the top-left rasterisation check for bounds checks during mutations, allowing pixels to actually fill the whole space.

                As of right now, we just extend maxout to 1.
                This will likely lead to inaccuracies between what is generated and what is actually rendered, as triangles with flat edges that exceed their chunks can draw into other chunks
                */
                this.options.add(new Generator.GenOpts(175, totalShapes, 1, true));
            }
        }
    }

    public static void validateLayout(Layout<?> layout) {
        if (layout instanceof LogicDisplayLayout logicDisplayLayout) {
            for (Layout.ImageChunk<LogicDisplayLayout.ChunkData> chunk : logicDisplayLayout.chunks) {
                if (chunk.data.procs.size >= LogicDisplayLayout.MAX_PROCS) {
                    throw new UnsupportedLayoutException("Converter only supports 29 processors per chunk");
                }
            }
        } else if (layout instanceof TiledDisplayLayout tiledDisplayLayout) {
            if (tiledDisplayLayout.chunks.sum(chunk -> chunk.data.procs) >= LogicDisplayLayout.MAX_PROCS) {
                throw new UnsupportedLayoutException("Converter only supports 29 processors per chunk");
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
        Seq<CompletableFuture<Seq<Shape>>> futures = new Seq<>();
        Seq<Generator> generators = new Seq<>();
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
            Generator gen = new Generator(options.get(i));
            Prov<Pixmap> pixmapProv = saveTmpPixmap(cropped);
            futures.add(CompletableFuture.supplyAsync(() -> gen.start(pixmapProv.get()), executor));
            generators.add(gen);
        }
        resized.dispose();
        return new ImageConverterTask(
                this,
                CompletableFuture.allOf(futures.toArray(CompletableFuture.class)).thenApply(ignored -> futures.map(CompletableFuture::join)),
                generators
        );
    }

    protected static class ImageConverterTask extends ConverterTask {
        // Technically supposed to return a fresh Seq whenever genProg() is called but we cache one for perf
        private final Seq<GeneratorProgress> genProg = new Seq<>();

        public ImageConverterTask(ImageConverter converter, CompletableFuture<Seq<Seq<Shape>>> results, Seq<Generator> generators) {
            super(converter, results, generators);
            for (int i = 0; i < this.generators.size; i++) {
                genProg.add(new GeneratorProgress());
            }
        }

        @Override
        public float progress() {
            return (float) this.generators.count(gen -> gen.getState() == Generator.GenState.Done) /this.generators.size;
        }

        @Override
        public Seq<GeneratorProgress> genProg() {
            for (int i = 0; i < this.generators.size; i++) {
                Generator gen = this.generators.get(i);
                GeneratorProgress prog = this.genProg.get(i);
                prog.genState = gen.getState();
                prog.progress = (float) gen.cur() /gen.maxGen;
            }
            return this.genProg;
        }
    }
}
