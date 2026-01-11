package me.mars.triangles.converter;

import arc.files.Fi;
import arc.struct.Seq;
import me.mars.triangles.layout.Layout;
import me.mars.triangles.shapes.Shape;

import java.util.concurrent.CompletableFuture;

public abstract class Converter {
    public Layout<?> layout;
    Fi filePath;
    public String name = "!NAME ME";

    public Converter(Layout<?> layout, Fi filePath) {
        this.layout = layout;
        this.filePath = filePath;
    }


    public abstract ConverterTask submit();
    public static abstract class ConverterTask {
        public Converter converter;
        public Seq<ImageGenerationService.GenerationTaskResult> results;

        public ConverterTask(Converter converter, Seq<ImageGenerationService.GenerationTaskResult> results) {
            this.converter = converter;
            this.results = results;
        }


        public boolean complete() {
            return this.getChunks().isDone();
        }
        public abstract float progress();
        public abstract CompletableFuture<Seq<Seq<Shape>>> getChunks();
        // TODO Stuff is a bit confusing after the refactor, basically this returns a subset(?) of the task's TaskResults which ui will be displaying
        // I Should refactor this somehow.
        public abstract Seq<ImageGenerationService.GenerationTaskResult> taskProgView();
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