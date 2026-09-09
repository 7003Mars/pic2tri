package me.mars.triangles.converter;

import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.struct.ObjectMap;
import kotlin.Pair;

import java.util.Objects;

public class ImageSize {
    private static final ObjectMap<String, Pair<Long, ImageSize>> cache = new ObjectMap<>();

    public int width, height;

    public ImageSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public static ImageSize getSize(Fi file) {
        String path = file.absolutePath();
        boolean isCached = cache.containsKey(path) && cache.get(path).component1() == file.lastModified();
        if (isCached) {
            return cache.get(path).component2();
        }
        Pixmap pixmap = new Pixmap(file);
        Pair<Long, ImageSize> entry = new Pair<>(file.lastModified(), new ImageSize(pixmap.width, pixmap.height));
        pixmap.dispose();
        cache.put(file.absolutePath(), entry);
        return entry.component2();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ImageSize imageSize = (ImageSize) o;
        return width == imageSize.width && height == imageSize.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }
}
