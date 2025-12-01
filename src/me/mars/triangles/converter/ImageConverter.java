package me.mars.triangles.converter;

import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.struct.Seq;
import arc.util.Log;
import me.mars.triangles.Generator;
import me.mars.triangles.layout.Layout;
import me.mars.triangles.layout.LogicDisplayLayout;
import me.mars.triangles.layout.TiledDisplayLayout;
import me.mars.triangles.shapes.Shape;
import mindustry.game.Schematic;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static me.mars.triangles.layout.LogicDisplayLayout.procRange;

public class ImageConverter extends Converter {
    private Seq<Future<Seq<Shape>>> results = new Seq<>();

    public Seq<Generator.GenOpts> options = new Seq<>();
    public Seq<GenProgress> genProgs = new Seq<>();

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
            this.options.add(layout instanceof LogicDisplayLayout ? new Generator.GenOpts(175, totalShapes) : new Generator.GenOpts(175, totalShapes, 0, false));
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
            throw new UnsupportedLayoutException("Unsupported type", true);
        }
    }

    @Override
    public void submit() {
        Pixmap origin = new Pixmap(this.filePath);
        Pixmap flipped = origin.flipY();
        origin.dispose();
        Pixmap resized = new Pixmap(this.layout.imageWidth, this.layout.imageHeight);
        resized.draw(flipped, 0, 0, resized.width, resized.height, true);
        for (int i = 0; i < this.layout.chunks.size; i++) {
            Layout.ImageChunk<?> chunk = this.layout.chunks.get(i);
            // TODO possible precision loss here, check code again
            int iw = (int) (chunk.width * (layout.imageWidth/layout.imageBounds.width));
            int ih = (int) (chunk.height * (layout.imageHeight/layout.imageBounds.height));
            Pixmap cropped = new Pixmap(iw, ih);
            /*TODO Lazy hack to get the correct pixel position, ideally doesn't assume all layout displays start at procRange */
            int ix = (int) ((chunk.chunkX-procRange) * (layout.imageWidth/layout.imageBounds.width));
            int iy = (int) ((chunk.chunkY-procRange) * (layout.imageHeight/layout.imageBounds.height));
            Log.info("Start @, @, w@ h@", ix, iy, iw, ih);
            cropped.draw(resized, ix, iy, iw, ih, 0, 0, iw, ih);
            Generator gen = new Generator(saveTmpPixmap(cropped), options.get(i));
            final GenProgress prog = new GenProgress() {
                @Override
                public Generator.GenState state() {
                    return gen.getState();
                }

                @Override
                public float progress() {
                    return gen.cur();
                }
            };
            genProgs.add(prog);
            results.add(executor.submit(gen::start));
        }
    }

    @Override
    public float totalProgress() {
        return (float) this.genProgs.count(p -> p.state() == Generator.GenState.Done) /this.genProgs.size;
    }

    @Override
    public boolean complete() {
        return this.genProgs.allMatch((conv) -> conv.state() == Generator.GenState.Done);
    }


    @Override
    public Schematic build() {
        Seq<Seq<Shape>> shapes = new Seq<>();
        try {
            for (Future<Seq<Shape>> task : this.results) {
                shapes.add(task.get());
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return this.layout.build(shapes);
    }
}
