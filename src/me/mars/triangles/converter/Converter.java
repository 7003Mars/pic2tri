package me.mars.triangles.converter;

import arc.files.Fi;
import arc.func.Prov;
import arc.graphics.Pixmap;
import arc.util.ArcRuntimeException;
import arc.util.OS;
import arc.util.Threads;
import me.mars.triangles.Generator;
import me.mars.triangles.PicToTri;
import me.mars.triangles.layout.Layout;
import mindustry.game.Schematic;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

public abstract class Converter {
    static final ExecutorService executor = Threads.executor("Image converter", OS.cores);

    Layout<?> layout;
    Fi filePath;
    public String name = "!NAME ME";

    public Converter(Layout<?> layout, Fi filePath) {
        this.layout = layout;
        this.filePath = filePath;
    }

    /**
    Will dispose the provided pixmap
     */
    static Prov<Pixmap> saveTmpPixmap(Pixmap pixmap) {
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


    public abstract void submit();

    public abstract float totalProgress();
    public abstract boolean complete();
    public abstract Schematic build();

    public interface GenProgress {
        Generator.GenState state();
        float progress();
    }

    public static class UnsupportedLayoutException extends RuntimeException {
        public boolean unsupportedType = false;
        public String reason;

        // TODO constructor
        public UnsupportedLayoutException(String reason) {
            this.reason = reason;
        }

        public UnsupportedLayoutException(String reason, boolean unsupportedType) {
            this.reason = reason;
            this.unsupportedType = unsupportedType;
        }
    }
}
