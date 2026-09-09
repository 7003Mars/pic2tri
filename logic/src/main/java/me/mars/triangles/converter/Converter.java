package me.mars.triangles.converter;

import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.struct.Seq;
import arc.util.ArcRuntimeException;
import arc.util.Log;
import me.mars.triangles.layout.Layout;
import me.mars.triangles.shapes.Shape;

import java.util.concurrent.CompletableFuture;

public abstract class Converter {
    public Layout<?> layout;
    Fi filePath;

    public Converter(Layout<?> layout, Fi filePath) {
        this.layout = layout;
        this.filePath = filePath;
    }


    public abstract ConverterTask submit();


    public static class ConverterTask {
        public Converter converter;
        private int completedChunks = 0;
        private Seq<Seq<Shape>> chunks;
        private CompletableFuture<Seq<Seq<Shape>>> chunkFuture = new CompletableFuture<>();
        public CompletableFuture<ConverterResult> result;

        public ConverterTask(Converter converter, int totalChunks, CompletableFuture<Pixmap> processedImage) {
            this.converter = converter;
            this.chunks = new Seq<>(totalChunks);
            this.chunks.size = totalChunks;
            this.result = this.chunkFuture.thenCombine(processedImage, ConverterResult::new)
                    .whenComplete((r, e) -> {
                        if (e != null) Log.err("Exception in ConverterTask", e);
                    });
        }

        public synchronized void completeChunk(int index, Seq<Shape> chunk) {
            if (this.chunks.get(index) != null) throw new ArcRuntimeException("Chunk already submitted?"); // TODO Better exception or error message
            this.chunks.set(index, chunk);
            this.completedChunks++;
            if (this.completedChunks >= this.chunks.size) {
                this.chunkFuture.complete(this.chunks);
            }
        }

        public int getCompletedChunks() {
            return this.completedChunks;
        }

        public int getTotalChunks() {
            return this.chunks.size;
        }
    }

    public static class ConverterResult {
        public Seq<Seq<Shape>> chunks;
        public Pixmap processedImage;

        public ConverterResult(Seq<Seq<Shape>> chunks, Pixmap processedImage) {
            this.chunks = chunks;
            this.processedImage = processedImage;
        }
    }

    public static class UnsupportedLayoutException extends RuntimeException {
        public String reason;

        public UnsupportedLayoutException(String reason) {
            this.reason = reason;
        }
    }
}