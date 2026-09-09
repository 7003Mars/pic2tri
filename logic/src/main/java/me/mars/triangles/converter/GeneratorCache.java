package me.mars.triangles.converter;

import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.serialization.SerializationException;
import me.mars.triangles.generation.Generator;
import me.mars.triangles.shapes.Shape;
import me.mars.triangles.shapes.ShapeIO;
import me.mars.triangles.utils.Prefs;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class GeneratorCache {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static @Nullable CachedImageGenerationTask getCache(Generator.GenOpts opts, byte[] imageHash, byte[] continuationHash) {
        String cacheKey = generateCacheKey(opts, imageHash, continuationHash);
        Fi dir = Prefs.genCacheDir.child(cacheKey.substring(0, 2));
        Fi cacheFile = dir.child(cacheKey+".bin");
        if (!cacheFile.exists()) return null;
        try (Reads read = cacheFile.reads()) {
            byte version = read.b();
            if (version != 1) {
                throw new RuntimeException("Unknown cache version: " + version);
            }
            // Stats
            int cur = read.i();
            int maxGen = read.i();
            float acc = read.f();
            // Shapes
            Seq<Shape> shapes = new Seq<>();
            for (int i = read.i(); i > 0; i--) {
                shapes.add(ShapeIO.readShape(read));
            }
            // Generated image
            boolean hasImage = read.bool();
            Pixmap generatedImage = null;
            if (hasImage) {
                byte[] generatedImageHash = read.b(16);
                generatedImage = new Pixmap(dir.child(cacheKey+".png"));
                if (!Arrays.equals(generatedImageHash, imageHash(generatedImage))) {
                    throw new SerializationException(Strings.format("Cached image hash does not match, should be @ but file is @", Arrays.toString(generatedImageHash), Arrays.toString(imageHash(generatedImage))));
                }
            }
            return new CachedImageGenerationTask(new Generator.GenerationOutput(shapes, generatedImage, cur, maxGen, acc));

        } catch (Exception e) {
            Log.err("Failed to read shape cache at " + cacheFile.absolutePath(), e);
            return null;
        }
    }

    public static void setCache(Generator.GenOpts opts, byte[] imageHash, byte[] continuationHash, Generator.GenerationOutput output) {
        String cacheKey = generateCacheKey(opts, imageHash, continuationHash);
        Fi dir = Prefs.genCacheDir.child(cacheKey.substring(0, 2));
        dir.mkdirs();
        Fi cacheFile = dir.child(cacheKey+".bin");
        try (Writes write = cacheFile.writes()) {
            // Version number
            write.b(1);
            // Generated image stats
            write.i(output.iterations());
            write.i(output.maxIterations());
            write.f(output.acc());
            // Shapes
            write.i(output.shapes().size);
            for (Shape shape : output.shapes()) {
                ShapeIO.writeShape(write, shape);
            }
            // Generated image
            if (output.result() == null) {
                write.bool(false);
            } else {
                write.bool(true);
                byte[] generatedImageHash = imageHash(output.result());
                write.b(generatedImageHash);
                dir.child(cacheKey+".png").writePng(output.result());
            }
        }
    }

    private static String generateCacheKey(Generator.GenOpts opts, byte[] imageHash, byte[] continuationHash) {
        try {
            MessageDigest md = MessageDigest.getInstance("md5");
            md.update(opts.toString().getBytes());
            md.update(imageHash);
            md.update(continuationHash);
            byte[] digest = md.digest();
            // Source - https://stackoverflow.com/a/9855338
            // Posted by maybeWeCouldStealAVan, modified by community. See post 'Timeline' for change history
            // Retrieved 2026-01-10, License - CC BY-SA 4.0
            char[] hexChars = new char[digest.length * 2];
            for (int j = 0; j < digest.length; j++) {
                int v = digest[j] & 0xFF;
                hexChars[j * 2] = HEX_ARRAY[v >>> 4];
                hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
            }
            return new String(hexChars);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // Wtf?
        }
    }

    public static byte[] imageHash(@Nullable Pixmap pixmap) {
        if (pixmap == null) {
            return new byte[16]; // Digests are 16 bytes (according to Gemini)
        }
        ByteBuffer buffer = pixmap.pixels;
        buffer.rewind();
        try {
            MessageDigest md = MessageDigest.getInstance("md5");
            md.update(pixmap.pixels);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // Wtf?
        }
    }


    public static class CachedImageGenerationTask extends ImageGenerationService.ImageGenerationTask {
        Generator.GenerationOutput generationOutput;

        public CachedImageGenerationTask(Generator.GenerationOutput generationOutput) {
            super(CompletableFuture.completedFuture(generationOutput));
            this.generationOutput = generationOutput;
        }


        @Override
        public Generator.GenState state() {
            return Generator.GenState.Done;
        }

        @Override
        public float accuracy() {
            return this.generationOutput.acc();
        }

        @Override
        public int cur() {
            return this.generationOutput.iterations();
        }

        @Override
        public int maxGen() {
            return this.generationOutput.maxIterations();
        }
    }

}
