package me.mars.triangles.converter;

import arc.files.Fi;
import arc.func.Prov;
import arc.graphics.Pixmap;
import arc.struct.Seq;
import arc.util.ArcRuntimeException;
import arc.util.OS;
import arc.util.Threads;
import me.mars.triangles.generation.Generator;
import me.mars.triangles.layout.Layout;
import me.mars.triangles.shapes.Shape;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public abstract class Converter {
    static final ExecutorService executor = Threads.executor("Image converter", OS.cores);

    public Layout<?> layout;
    Fi filePath;
    public String name = "!NAME ME";

    public Converter(Layout<?> layout, Fi filePath) {
        this.layout = layout;
        this.filePath = filePath;
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


    public abstract ConverterTask submit();
    public static abstract class ConverterTask {
        public Converter converter;
        public CompletableFuture<Seq<Seq<Shape>>> results;
        public Seq<Generator> generators;

        public ConverterTask(Converter converter, CompletableFuture<Seq<Seq<Shape>>> results, Seq<Generator> generators) {
            this.converter = converter;
            this.results = results;
            this.generators = generators;
        }


        public boolean complete() {
            return this.results.isDone();
        }
        public abstract float progress();
        public abstract Seq<GeneratorProgress> genProg();
    }

    public static class GeneratorProgress {
        public Generator.GenState genState = Generator.GenState.Ready;
        public float progress;
    }

    public static class UnsupportedLayoutException extends RuntimeException {
        public boolean unsupportedType = false;
        public String reason;

        public UnsupportedLayoutException(String reason) {
            this.reason = reason;
        }

        public UnsupportedLayoutException(String reason, boolean unsupportedType) {
            this.reason = reason;
            this.unsupportedType = unsupportedType;
        }
    }
}