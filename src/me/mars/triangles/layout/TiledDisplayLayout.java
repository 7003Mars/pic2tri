package me.mars.triangles.layout;

import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.math.geom.Rect;
import arc.struct.*;
import arc.util.Log;
import arc.util.Tmp;
import me.mars.triangles.shapes.Shape;
import mindustry.content.Blocks;
import mindustry.game.Schematic;
import static arc.util.Tmp.v1;
import static arc.util.Tmp.v2;
import static me.mars.triangles.layout.LogicDisplayLayout.*;

import mindustry.logic.LExecutor;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.TileableLogicDisplay;

import java.util.Comparator;

public class TiledDisplayLayout extends Layout<TiledDisplayLayout.ChunkData> {
    // Controller
    public static final String[] controllerStart = """
            set c @this
            set d display1
            sensor op display1 @operations
            jump 7 equal op 0
            sensor e switch1 @enabled
            jump 7 equal e 1
            end
            control enabled switch1 0 0 0 0
            wait 1e-4
            """.split("\n");
    public static final String[] controllerEnd = """
            set i null
            wait 2
            """.split("\n"); // TODO Determine this from the longest possible time to run 1k instructions
    // Workers
    public static final String[] workerStart = """
            read c processor1 "c"
            read d c "d"
            jump 0 equal d null
            read i c "i"
            jump $-1 notEqual i @
            wait 1e-4
            set j 0
            """.split("\n");
    public static int WORKER_MAX_SHAPES;
    static {
        int freeInstructions = LExecutor.maxInstructions - workerStart.length - drawDelay.length;
        freeInstructions-= Mathf.ceilPositive(freeInstructions/256f);
        WORKER_MAX_SHAPES = freeInstructions/2;
    }




    private static Point2 p1 = new Point2(), p2 = new Point2(), p3 = new Point2(), p4 = new Point2();

    // Maps each processor position to its corresponding link(Either a display directly or a proc containing the display)
    public Seq<Point2> procs = new Seq<>();

    public TiledDisplayLayout(TileableLogicDisplay tiledDisplay, int imageWidth, int imageHeight, int chunkSize) {
        super(tiledDisplay, imageWidth, imageHeight);
        assert tiledDisplay.size == 1; // Other sizes unsupported
        // TODO IMPT The display actually loses 12px of width and height(check TileableLogicDisplay#draw()), we need to update the image snap size calculations, as well as for this
        int sizeX = Mathf.ceil((float) (imageWidth+12) /tiledDisplay.displaySize), sizeY = Mathf.ceil((float) (imageHeight+12) /tiledDisplay.displaySize);
        // ^ We add 12 to calculate the fake width and height before dividing
        this.displayWidth = sizeX;
        this.displayHeight = sizeY;
        // Ensure chunk can't be bigger than the total size of the displays
        chunkSize = Math.min(sizeY, Math.min(chunkSize, sizeX));
        assert chunkSize != 0; // Prevent getting Infinite xChunks or yChunks
        this.xChunks = Math.max(1, Mathf.floor((float) sizeX/chunkSize));
        this.yChunks = Math.max(1, Mathf.floor((float) sizeY/chunkSize));
        // TODO We can't have >4 processors drawing simultaneously to the display as its buffer only supports 1024 commands max(256*4)
        this.imageBounds = new Rect(procRange, procRange, ((float) imageWidth/tiledDisplay.displaySize)*tiledDisplay.size, ((float) imageHeight/tiledDisplay.displaySize)*tiledDisplay.size);
        this.width = this.preview.width = sizeX + procRange * 2;
        this.height = this.preview.height = sizeY + procRange * 2;

        for (int i = 0; i < xChunks * yChunks; i++) {
            ImageChunk<ChunkData> chunk = new ImageChunk<>(procRange+(i % xChunks)*chunkSize, procRange+(i / xChunks)*chunkSize, chunkSize, chunkSize);
            chunk.data = new ChunkData();
            this.chunks.add(chunk);
        }
        // Pad the last row/column of chunks to fit in the leftover space
        sizeX -= this.xChunks * chunkSize;
        sizeY -= this.yChunks * chunkSize;
        if (sizeX > 0) {
            for (int y = 0; y < yChunks; y++) {
                this.chunks.get(y * this.xChunks + xChunks-1).width += sizeX;
            }
        }
        if (sizeY > 0) {
            for (int x = 0; x < this.xChunks; x++) {
                this.chunks.get((this.yChunks-1)*this.xChunks + x).height += sizeY;
            }
        }
    }

    // REGION UI METHODS

    @Override
    public ImageChunk<TiledDisplayLayout.ChunkData> getChunk(int x, int y) {
        // TODO Inefficient impl
        for (ImageChunk<ChunkData> chunk : this.chunks) {
            if (x >= chunk.chunkX && x < chunk.chunkX + chunk.width && y >= chunk.chunkY && y < chunk.chunkY + chunk.height) return chunk;
        }
        return null;
    }

    public int requestProcs(int target) {
        if (target == this.procs.size) return target;
        if (this.procs.size > target) {
            while (this.procs.size > target) {
                Point2 point = this.procs.pop();
                this.preview.tiles.remove(t -> t.x == point.x && t.y == point.y);
            }
            this.redistributeProcs();
            return target;
        }
        // TODO Dupe code, could prob refactor this logic into another class
        this.procs.clear();
        this.preview.tiles.clear();
        // Spiral generator skips the very first point so we add it manually
//        this.procs.add(new Pair<>(new Point2(procRange, procRange-1), new Point2(procRange, procRange))); // TODO Somehow the generator does not skip the first point???? idk
        int dir = 0;
        int x = procRange, y = procRange-1;
        // Max length of current segment, for both width and height
        int maxX = this.displayWidth/* Width of displays */, maxY = this.displayHeight + 1/* Height of displays + 1 */;
        int len = 0;
        int count = 0;
        while (this.procs.size < target && this.procs.size < MAX_PROCS) {
            if (count >= 2) { // Skip first 2 blocks.
                procs.add(new Point2(x, y));
                this.preview.tiles.add(new Schematic.Stile(Blocks.microProcessor, x, y, null, (byte) 0));
            }
            x += Geometry.d4x(dir);
            y += Geometry.d4y(dir);
            len += 1;
            count++;
            if (dir == 0 || dir == 2) {
                if (len >= maxX) {
                    len = 0;
                    maxX += 1;
                    dir = (dir + 1) % 4;
                }
            } else {
                if (len >= maxY) {
                    len = 0;
                    maxY += 1;
                    dir = (dir + 1) % 4;
                }
            }

        }
        this.redistributeProcs();
        return this.procs.size;
    }

    public static int totalShapes(int procs) {
        return WORKER_MAX_SHAPES * procs;
    }

    // ENDREGION

    public static int procsRequired(int shapes) {
        int procs = Mathf.ceilPositive((float) shapes /WORKER_MAX_SHAPES);
        return procs <= MAX_PROCS ? procs : -1;
    }

    public Point2 closestProc(int x, int y) {
        // https://stackoverflow.com/questions/74827925/find-closest-edge-to-a-point-of-a-rectangle
        p1.set(procRange-1, procRange-1);
        p2.set(procRange-1, this.height-procRange);
        p3.set(this.width-procRange, this.height-procRange);
        p4.set(this.width-procRange, procRange-1);
        Point2 point = Tmp.p3.set(x, y);
        Point2[] points = new Point2[] {
                proj(p1, p2, point),
                proj(p2, p3, point),
                proj(p3, p4, point),
                proj(p4, p1, point)
        };
        Sort.instance().sort(points, Comparator.comparing(p -> p.dst2(x, y)));
        return points[0];
    }

    // TODO I set this to public for testing, remove later?
    public static Point2 proj(Point2 s1, Point2 s2, Point2 point) {
        v1.set(s2.x - s1.x, s2.y - s1.y);
        float originalLen = v1.len();
        v1.nor();
        v2.set(point.x - s1.x, point.y - s1.y);
        v1.scl(Mathf.clamp(v1.dot(v2), 0f, originalLen));
        // TODO Should this be rounded or truncated??
        return new Point2(Mathf.round(v1.x+s1.x) , Mathf.round(v1.y+s1.y));
    }

    void redistributeProcs() {
        int displayArea = this.displayWidth * this.displayHeight;
        for (ImageChunk<ChunkData> chunk : this.chunks) {
            // TODO IMPT This code does not evenly distribute the procs, as we round down and there might be remainders
            chunk.data.procs = (int) ((float) chunk.width*chunk.height/displayArea * this.procs.size);
        }
    }

    @Override
    public Schematic build(Seq<Seq<Shape>> shapes) {
        Schematic schem = new Schematic(new Seq<>(), new StringMap(), this.width, this.height);
        // Fill in displays
        for (int x = procRange; x < this.width-procRange; x++) {
            for (int y = procRange; y < this.height-procRange; y++) {
                schem.tiles.add(new Schematic.Stile(this.display, x, y, null, (byte) 0));
            }
        }
        // Calculate the number of processors needed.
        int procsRequired = procsRequired(shapes.sum(seq -> seq.size));
        assert procsRequired <= this.procs.size;
        // Place the switch
        schem.tiles.add(new Schematic.Stile(Blocks.switchBlock, procRange, procRange-1, null, (byte) 0));
        // Place the controller
        int controllerX = procRange+1, controllerY = procRange-1;
        LogicBlock.LogicBuild controller = (LogicBlock.LogicBuild) Blocks.microProcessor.newBuilding();
        controller.tile = new Tile(controllerX, controllerY);
        CodeBuilder controllerCode = new CodeBuilder();
        controllerCode.extendLines(controllerStart);
        for (int i = -procsRequired; i < 0; i++) {
            controllerCode.appendLine("set i " + i);
            controllerCode.appendLine("wait 1e-4");
        }
        controllerCode.extendLines(controllerEnd);
        controller.updateCode(controllerCode.toString());
        controller.links.add(new LogicBlock.LogicLink(controllerX-1, controllerY, "switch1" ,true));
        controller.links.add(new LogicBlock.LogicLink(controllerX, controllerY+1, "display1", true));
        schem.tiles.add(new Schematic.Stile(Blocks.microProcessor, controllerX, controllerY, controller.config(), (byte) 0));
        // Image processors
        // Translate chunks then flatten results
        Seq<Shape> allShapes = new Seq<>();
        for (int i = 0; i < this.chunks.size; i++) {
            ImageChunk<ChunkData> chunk = this.chunks.get(i);
            int ix = (int) ((chunk.chunkX-procRange) * (this.imageWidth/this.imageBounds.width));
            int iy = (int) ((chunk.chunkY-procRange) * (this.imageHeight/this.imageBounds.height));
            shapes.get(i).each(shape -> shape.translate(ix, iy));
            allShapes.add(shapes.get(i));
        }
        Seq<CodeBuilder> code = generateProcessorCode(procsRequired, allShapes);
        for (int i = 0; i < procsRequired; i++) {
            Point2 proc = this.procs.get(i);
            Point2 closestProc = closestProc(proc.x, proc.y);
            LogicBlock.LogicLink controllerLink;
            // Innermost ring, we sequentially link each proc to the controller
            // Also consider the edge case where the closest processor is actually the switch block. If it is, we fallback to linking to the last linked processor
            if (proc.dst2(closestProc) == 0 || (closestProc.x == procRange && closestProc.y == procRange-1 && proc.dst2(closestProc) <= procRange*procRange)) {
                controllerLink = new LogicBlock.LogicLink(controllerX, controllerY, "processor1", true);
            } else {
                // Outer rings, we just link as far as possible into the inner rings
                v1.set(closestProc.x, closestProc.y);
                v1.sub(proc.x, proc.y);
                v1.limit(procRange);
                // We want to round towards the origin(relative to the processor) to ensure the point is within range
                v1.x = (int) v1.x;
                v1.y = (int) v1.y;
                controllerLink = new LogicBlock.LogicLink((int) (proc.x + v1.x), (int) (proc.y + v1.y), "processor1", true);
            }
            LogicBlock.LogicBuild lbuild = (LogicBlock.LogicBuild) Blocks.microProcessor.newBuilding();
            lbuild.tile = new Tile(proc.x, proc.y);
            lbuild.links.add(controllerLink);
            lbuild.updateCode(code.get(i).toString()); // TODO Actual code bs here
            schem.tiles.add(new Schematic.Stile(Blocks.microProcessor, proc.x, proc.y, lbuild.config(), (byte) 0));
            controllerX = proc.x;
            controllerY = proc.y;

        }
        return schem;
    }

    // TODO REFACTOR THIS, ITS ALMOST IDENTICAL CODE
    public static Seq<CodeBuilder> generateProcessorCode(int processors, Seq<Shape> shapes) {
        IntSeq shapeCounter = new IntSeq(processors);
        Seq<CodeBuilder> code = new Seq<>();
        for (int i = 0; i < processors; i++) {
            CodeBuilder builder = new CodeBuilder();
            // Append init code to each processor
            for (String line : workerStart) {
                builder.appendLine(line.replace("@", String.valueOf(i-processors)));
            }
            code.add(builder);
            shapeCounter.add(0);
        }

        int procIndex = 0;
        // TMP TODO Removeme
        int tmp = -1;
        // ENDTMP
        for (Shape shape : shapes) {
            tmp += 1;
            CodeBuilder builder = code.get(procIndex);
            builder.appendShape(shape);
            shapeCounter.incr(procIndex, 1);
            int count = shapeCounter.get(procIndex);
            boolean advance = count % 128 == 0 || count >= WORKER_MAX_SHAPES;
            if (advance) {
                if (count % 128 == 0) {
                    builder.appendLine("drawflush d");
                }
                procIndex = (procIndex+1)%processors;
            }
        }
        // Make the ending draws also take up 128 ticks.
        for (int i = 0; i < shapeCounter.size; i++) {
            if (shapeCounter.get(i) % 128 == 0) continue; // Perfectly aligned, no need for this delayed flush.
            CodeBuilder builder = code.get(i);
            int remaining = 128-(shapeCounter.get(i) % 128) - 1 /*Just entering the loop already takes 2 instructions*/;
            for (String line : drawDelay) {
                builder.appendLine(line.replace("@", String.valueOf(remaining)));
            }
            builder.appendLine("drawflush d");
        }

        for (int i = 0; i < processors; i++) {
            Log.info("Proc @, @/@", i, shapeCounter.get(i), WORKER_MAX_SHAPES);
        }

        return code;
    }

    public static class ChunkData {
        public int procs;
    }

}
