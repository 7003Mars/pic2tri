package me.mars.triangles.layout;

import arc.math.Mathf;
import arc.math.geom.Rect;
import arc.struct.Seq;
import arc.struct.StringMap;
import me.mars.triangles.shapes.Shape;
import mindustry.game.Schematic;
import mindustry.world.blocks.logic.LogicDisplay;

// TODO assume Layout is specialised for logic processors for now
// TODO Remove above comment
public abstract class Layout<T> {
    public int xChunks;
    public int yChunks;
    LogicDisplay display;
    /** Size of the entire display, in world units */
    public int displayWidth, displayHeight;
    public Seq<ImageChunk<T>> chunks = new Seq<>();
    /** Calculated width and height of image, in pixels*/
    public int imageWidth, imageHeight;
    /** Total size of layout*/
    public int width, height;

    public Schematic preview;
    /* Where the image will be drawn, in world units */
    public Rect imageBounds;


    public Layout(LogicDisplay display, int imageWidth, int imageHeight) {
        this.display = display;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.preview = new Schematic(new Seq<>(), new StringMap(), 0, 0);

    }

    public abstract ImageChunk<T> getChunk(int x, int y);

    public abstract Schematic build(Seq<Seq<Shape>> shapes); // TODO List<Shapes> should prob be a param

    public static class ImageChunk<T> {
        /** Chunk position */
        public int chunkX, chunkY;
        /** Chunk width and height, in world units */
        public int width, height;

        public T data;

        public ImageChunk(int chunkX, int chunkY, int width, int height) {
            this.chunkX = chunkX;
            this.chunkY = chunkY;
            this.width = width;
            this.height = height;
        }
    }

    public float displayDensity(LogicDisplay display) {
        return (float) display.displaySize /display.size;
    }
}