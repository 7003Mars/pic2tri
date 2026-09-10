package me.mars.triangles.layout;

import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.math.geom.Rect;
import arc.struct.*;
import arc.util.Tmp;
import kotlin.Pair;
import me.mars.triangles.shapes.Shape;
import mindustry.content.Blocks;
import mindustry.game.Schematic;
import static arc.util.Tmp.v1;
import static arc.util.Tmp.v2;
import static me.mars.triangles.layout.CodeGenUtil.drawDelay;
import static me.mars.triangles.layout.LogicDisplayLayout.*;

import mindustry.logic.LExecutor;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.TileableLogicDisplay;

import java.util.Comparator;

public class TiledDisplayLayout extends Layout<TiledDisplayLayout.ChunkData> {
    public static int DRAW_TRANSLATE_OFFSET = CodeBuilder.MAX_SAFE_COORDINATE/2;


    // Controller
    public static final String[] controllerStart = """
            set controller @this
            set display display1
            sensor op display1 @operations
            jump start equal op 0
            sensor enabled switch1 @enabled
            jump start equal enabled 1
            end
            start:
            control enabled switch1 0 0 0 0
            draw reset 0 0 0 0 0 0
            draw translate _ _ 0 0 0 0
            drawflush display1
            wait 1e-4
            """.replace("_", String.valueOf(DRAW_TRANSLATE_OFFSET)).split("\n");
    /** The longest possible time required for an executor to complete one whole cycle(assuming no jumps)*/
    public static final float executorCycleTime = (float) LExecutor.maxInstructions /((LogicBlock) Blocks.microProcessor).instructionsPerTick/60f;
    public static final String[] controllerEnd = """
            set i null
            wait _
            """.replace("_", String.valueOf(Math.ceil(executorCycleTime))).split("\n");
    // Workers
    public static final String[] workerStart = """
            read controller processor1 "controller"
            read display controller "display"
            sensor valid controller @dead
            jump 0 equal valid 1
            read i controller "i"
            jump $-1 notEqual i _
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

    // Positions of all worker processors
    public Seq<Point2> workerPos = new Seq<>();
    private static int controllerTilesCount = 2;
    Point2 switchPos, controllerPos;

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
        this.requestProcs(this.chunks.size);
    }


    protected Seq<Point2> generateSpiral(int count) {
        Seq<Point2> points = new Seq<>();
        int dir = 0;
        int x = procRange, y = procRange-1;
        // Max length of current segment, for both width and height
        int maxX = this.displayWidth/* Width of displays */, maxY = this.displayHeight + 1/* Height of displays + 1 */;
        int len = 0;
        while (points.size < count) {
            points.add(new Point2(x, y));
            x += Geometry.d4x(dir);
            y += Geometry.d4y(dir);
            len += 1;
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
        return points;
    }

    // region Ui methods

    @Override
    public ImageChunk<TiledDisplayLayout.ChunkData> getChunk(int x, int y) {
        // Thanks, gemini!
        int chunkSize = this.chunks.first().width; // TODO Correct yet disgusting way to get the size of normal(non-expanded) chunks
        int translatedX = x - procRange;
        int translatedY = y - procRange;
        int chunkX = translatedX / chunkSize;
        int chunkY = translatedY / chunkSize;
        // If the coordinate is beyond the last standard boundary but still
        // within the total bounds, force it into the last chunk index.
        if (chunkX >= this.xChunks && translatedX < this.displayWidth) {
            chunkX = this.xChunks - 1;
        }
        if (chunkY >= this.yChunks && translatedY < this.displayHeight) {
            chunkY = this.yChunks - 1;
        }

        if (chunkX < 0 || chunkX >= this.xChunks || chunkY < 0 || chunkY >= this.yChunks) {
            return null;
        }
        return this.chunks.get(chunkY * this.xChunks + chunkX);
    }

    public int requestProcs(int target) {
        target = Math.max(this.chunks.size, target);
        if (target == this.workerPos.size) return target;
        if (this.workerPos.size > target) {
            while (this.workerPos.size > target) {
                Point2 point = this.workerPos.pop();
                this.preview.tiles.remove(t -> t.x == point.x && t.y == point.y);
            }
            this.redistributeProcs();
            return target;
        }
        // TODO Dupe code, could prob refactor this logic into another class
        this.preview.tiles.clear();
        this.workerPos.clear();
        // First 2 blocks are the switch and controller.
        Seq<Point2> points = generateSpiral(target + controllerTilesCount);
        switchPos = points.get(0);
        preview.tiles.add(new Schematic.Stile(Blocks.switchBlock, switchPos.x, switchPos.y, null, (byte) 0));
        controllerPos = points.get(1);
        preview.tiles.add(new Schematic.Stile(Blocks.microProcessor, controllerPos.x, controllerPos.y, null, (byte) 0));
        for (int i = 2; i < points.size; i++) {
            Point2 pos = points.get(i);
            this.workerPos.add(pos);
            this.preview.tiles.add(new Schematic.Stile(Blocks.microProcessor, pos.x, pos.y, null, (byte) 0));
        }
        this.redistributeProcs();
        return this.workerPos.size;
    }

    public static int totalShapes(int procs) {
        return WORKER_MAX_SHAPES * procs;
    }

    // endregion

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
        return new Point2(Mathf.round(v1.x+s1.x) , Mathf.round(v1.y+s1.y));
    }

    void redistributeProcs() {
        int displayArea = this.displayWidth * this.displayHeight;
        int totalProcs = this.workerPos.size, assignedProcs = 0;
        Seq<Pair<ImageChunk<ChunkData>, Float>> exactProcs = new Seq<>();
        for (ImageChunk<ChunkData> chunk : this.chunks) {
            float exact = ((float) chunk.width*chunk.height/displayArea * totalProcs);
            int truncated = Mathf.floorPositive(exact);
            chunk.data.procs = truncated;
            assignedProcs += truncated;
            exactProcs.add(new Pair<>(chunk, -(exact-truncated))); // We want to sort by descending later
        }
        exactProcs.sortComparing(Pair::component2);
        for (int i = 0; i < totalProcs-assignedProcs; i++) {
            exactProcs.get(i).component1().data.procs++;
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
        // Add switch & controller
        schem.tiles.add(new Schematic.Stile(Blocks.switchBlock, switchPos.x, switchPos.y, null, (byte) 0));
        int controllerX  = controllerPos.x, controllerY = controllerPos.y;
        ProcessorBuilder controllerBuilder = new ProcessorBuilder(controllerX, controllerY, generateControllerCode(shapes));
        controllerBuilder.addAbsoluteLink(switchPos.x, switchPos.y, "switch1");
        controllerBuilder.addAbsoluteLink(switchPos.x, switchPos.y+1, "display1");
        schem.tiles.add(controllerBuilder.getStile());
        // Calculate the number of processors needed.
        Seq<String> workerCode = generateWorkerCode(shapes);
        for (int i = 0; i < workerCode.size; i++) {
            Point2 proc = this.workerPos.get(i);
            Point2 closestProc = closestProc(proc.x, proc.y);
            ProcessorBuilder procBuilder = new ProcessorBuilder(proc.x, proc.y, workerCode.get(i));
            // Innermost ring, we sequentially link each proc to the controller
            // Also consider the edge case where the closest processor is actually the switch block. If it is, we fallback to linking to the last linked processor
            if (proc.dst2(closestProc) == 0 || (closestProc.x == procRange && closestProc.y == procRange-1 && proc.dst2(closestProc) <= procRange*procRange)) {
                procBuilder.addAbsoluteLink(controllerX, controllerY, "processor1");
            } else {
                // Outer rings, we just link as far as possible into the inner rings
                v1.set(closestProc.x, closestProc.y);
                v1.sub(proc.x, proc.y);
                v1.limit(procRange);
                // We want to round towards the origin(relative to the processor) to ensure the point is within range
//                v1.x = (int) v1.x;
//                v1.y = (int) v1.y;
                procBuilder.addRelativeLink((int) v1.x, (int) v1.y, "processor1");
            }
            schem.tiles.add(procBuilder.getStile());
            controllerX = proc.x;
            controllerY = proc.y;

        }
        return schem;
    }

    protected String generateControllerCode(Seq<Seq<Shape>> shapes) {
        int procsRequired = procsRequired(shapes.sum(seq -> seq.size));
        CodeBuilder controllerCode = new CodeBuilder();
        controllerCode.extendLines(controllerStart);
        for (int i = -procsRequired; i < 0; i++) {
            controllerCode.appendLine("set i " + i);
            controllerCode.appendLine("wait 1e-4");
        }
        controllerCode.extendLines(controllerEnd);
        return controllerCode.toString();
    }

    protected Seq<String> generateWorkerCode(Seq<Seq<Shape>> shapes) {
        Seq<String> res = new Seq<>();
        int procsRequired = procsRequired(shapes.sum(seq -> seq.size));
        assert procsRequired <= this.workerPos.size;
        res.add(generateInitWorkerCode(procsRequired, translateAndFlattenChunks(shapes)));
        return res;
    }

    protected Seq<Shape> translateAndFlattenChunks(Seq<Seq<Shape>> shapes) {
        // Translate chunks then flatten results
        Seq<Shape> allShapes = new Seq<>();
        for (int i = 0; i < this.chunks.size; i++) {
            ImageChunk<ChunkData> chunk = this.chunks.get(i);
            int ix = (int) ((chunk.chunkX-procRange) * (this.imageWidth/this.imageBounds.width));
            int iy = (int) ((chunk.chunkY-procRange) * (this.imageHeight/this.imageBounds.height));
            shapes.get(i).each(shape -> shape.translate(ix - DRAW_TRANSLATE_OFFSET, iy - DRAW_TRANSLATE_OFFSET));
            allShapes.add(shapes.get(i));
        }
        return allShapes;
    }

    // TODO REFACTOR THIS, ITS QUITE SIMILAR CODE
    public static Seq<String> generateInitWorkerCode(int processors, Seq<Shape> shapes) {
        IntSeq shapeCounter = new IntSeq(processors);
        Seq<CodeBuilder> code = new Seq<>();
        for (int i = 0; i < processors; i++) {
            CodeBuilder builder = new CodeBuilder();
            // Append init code to each processor
            for (String line : workerStart) {
                builder.appendLine(line.replace("_", String.valueOf(i-processors)));
            }
            code.add(builder);
            shapeCounter.add(0);
        }

        int procIndex = 0;
        for (Shape shape : shapes) {
            CodeBuilder builder = code.get(procIndex);
            builder.appendShape(shape);
            shapeCounter.incr(procIndex, 1);
            int count = shapeCounter.get(procIndex);
            boolean advance = count % 128 == 0 || count >= WORKER_MAX_SHAPES;
            if (advance) {
                if (count % 128 == 0) {
                    builder.appendLine("drawflush display");
                }
                procIndex = (procIndex+1)%processors;
            }
        }
        CodeGenUtil.padLastFlush(shapeCounter, code, "drawflush display");
//        for (int i = 0; i < processors; i++) {
//            Log.info("Proc @, @/@", i, shapeCounter.get(i), WORKER_MAX_SHAPES);
//        }

        return code.map(CodeBuilder::toString);
    }

    public static class ChunkData {
        public int procs;
    }

}
