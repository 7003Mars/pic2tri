package me.mars.triangles.utils;

import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;

public class PixmapUtils {
    public static Pixmap loadPixmap(Fi path) {
        return Prefs.javaPixmapLoader ? PixmapIO.readPNG(path) : new Pixmap(path);
    }
}
