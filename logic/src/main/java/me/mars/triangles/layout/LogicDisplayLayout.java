package me.mars.triangles.layout;

import arc.Core;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.math.geom.Rect;
import arc.struct.Bits;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
import me.mars.triangles.schematics.SchematicHandler;
import me.mars.triangles.shapes.Shape;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.Schematic;
import mindustry.logic.LExecutor;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.LogicDisplay;

import static me.mars.triangles.layout.CodeGenUtil.drawDelay;
import static me.mars.triangles.utils.Prefs.setting;

public class LogicDisplayLayout extends Layout<LogicDisplayLayout.ChunkData> {
    // region Mlog code
    public static int procRange = (int) (((LogicBlock)Blocks.microProcessor).range/ Vars.tilesize);
    public static int MAX_PROCS;
    static {
        // Every processor has a "cooldown" of 257(256 draws + 1 flush)/2 ticks before they will flush again, so we can cram that many continuous draw flushes in that cooldown time
        MAX_PROCS = 257/((LogicBlock)Blocks.microProcessor).instructionsPerTick;
    }

    // If only one processor is used
    static final String[] singleStart = """
			sensor e display1 @enabled
			jump 0 equal e 1
			control enabled display1 1 0 0 0
			""".split("\n");
    public static final int MAX_SHAPES_SINGLE;
    static {
        int freeInstructions = LExecutor.maxInstructions - singleStart.length;
        freeInstructions-= Mathf.ceilPositive(freeInstructions/256f);
        MAX_SHAPES_SINGLE = freeInstructions/2;
    }

    // We wait to align all processors to start at the next tick
    public static final String[] multiStart = """
			sensor e display1 @enabled
			jump 0 equal e 1
			wait 1e-4
			control enabled display1 1 0 0 0
			set j 0
			""".split("\n");
    // Can't use wait instructions due to accumulator increasing.
    static final String[] repeat = """
			set t @tick
			jump $ equal t @tick
			""".split("\n");
    // The maximum shapes possible are when there are 0 repeats at all.
    private static int maxShapesForProc(int procIndex) {
        int maxFreeInstructions = LExecutor.maxInstructions - multiStart.length - drawDelay.length;
        int freeInstructions = maxFreeInstructions - repeat.length * procIndex;
        // Every 256 instructions we need to flush once
        freeInstructions -= Mathf.ceilPositive(freeInstructions/256f);
        return freeInstructions/2;
    }
    public static final int MAX_SHAPES_PER_PROC = maxShapesForProc(0);
    // endregion

    protected Bits occupied = new Bits();

    public LogicDisplayLayout(LogicDisplay displayBlock, int imageWidth, int imageHeight) {
        super(displayBlock, imageWidth, imageHeight);
        this.xChunks = Mathf.ceil((float) imageWidth/displayBlock.displaySize);
        this.yChunks = Mathf.ceil((float) imageHeight/displayBlock.displaySize);
        // TODO Can offset bounds to center image
        // (imageWidth/displayDensity)/block.size => imageWidth/(block.dispSize/disp.size)/block.size
        this.imageBounds = new Rect(procRange, procRange, ((float) imageWidth /displayBlock.displaySize)*displayBlock.size, ((float) imageHeight /displayBlock.displaySize)*displayBlock.size);
        this.displayWidth = xChunks * displayBlock.size;
        this.displayHeight = yChunks * displayBlock.size;
        this.width = this.preview.width = this.displayWidth + 2 * procRange;
        this.height = this.preview.height = this.displayHeight + 2 * procRange;

        for (int y = procRange; y < this.height - procRange; y++) {
            int start = y * this.width + procRange;
            this.occupied.set(start, start + this.width - (2*procRange));
        }

        for (int i = 0; i < xChunks * yChunks; i++) {
            ImageChunk<ChunkData> chunk = new ImageChunk<>(procRange+(i % xChunks)*displayBlock.size, procRange+(i / xChunks)*displayBlock.size, displayBlock.size, displayBlock.size);
            chunk.data = new ChunkData();
            this.chunks.add(chunk);
            requestProcs(chunk.chunkX, chunk.chunkY, 1);
        }
    }

    // region Ui methods
    public int requestProcs(int selX, int selY, int target) {
        target = Math.max(Math.min(target, MAX_PROCS), 1);
        ImageChunk<ChunkData> chunk = this.getChunk(selX, selY);
        ChunkData data = chunk.data;
        if (target == data.procs.size) {
            return data.procs.size;
        }
        if (target < data.procs.size) {
            shrinkProcs(data, target);
            return target;
        }
        shrinkProcs(data, 0);
        int dir = 0;
        int x = chunk.chunkX - display.sizeOffset, y = chunk.chunkY - display.sizeOffset;
        int segLen = 0, maxSegLen = 1;
        int range2 = procRange*2 + this.display.size;
        range2 *= range2;
        for (int i = 0; i < range2; i++) {
            if (data.procs.size >= target) return target;
            x+= Geometry.d4x(dir);
            y+= Geometry.d4y(dir);
            segLen++;
            if (segLen >= maxSegLen) {
                dir = (dir+1)%4;
                segLen = 0;
                if (dir == 0 || dir == 2) maxSegLen += 1;
            }
            if (!this.occupied.get(y * this.width + x) && within(chunk.chunkX, chunk.chunkY, x, y)) {
                this.occupied.set(y * this.width + x);
                data.procs.add(new Point2(x, y));
                this.preview.tiles.add(new Schematic.Stile(Blocks.microProcessor, x, y, null, (byte) 0));
            }
        }
        return data.procs.size;
    }

    public ImageChunk<ChunkData> getChunk(int x, int y) {
        int offset = procRange;
        int chunkX = (x - offset)/this.display.size;
        int chunkY = (y - offset)/this.display.size;
        if (chunkX < 0 || chunkX >= this.xChunks || chunkY < 0 || chunkY >= this.yChunks) {
            return null;
        }
        return this.chunks.get(chunkY * this.xChunks + chunkX);
    }

    public static int totalShapes(int procs) {
        if (procs == 1) {
            return MAX_SHAPES_SINGLE;
        }
        // The boilerplate code each processor must have
        int maxFreeInstructions = LExecutor.maxInstructions - multiStart.length - drawDelay.length;
        int total = 0;
        /*
        The size of repeat instructions for n processors follows the sequence 0, 2, 4, 6, 8, ..., 2(n-1)
         */
        for (int i = 0; i < procs; i++) {
            int freeInstructions = maxFreeInstructions - repeat.length * i;
            // Every 256 instructions we need to flush once
            freeInstructions -= Mathf.ceilPositive(freeInstructions/256f);
            // Calculate the number of shapes for one individual processor first
            total += freeInstructions/2;
        }
        return total;
    }

    // endregion

    public static int procsRequired(int shapes) {
        // O(N^2) but who cares
        for (int i = 1; i <= MAX_PROCS; i++) {
            if (totalShapes(i) >= shapes) return i;
        }
        return -1;
    }

    @Override
    public Schematic build(Seq<Seq<Shape>> shapes) {
        Schematic schem = new Schematic(new Seq<>(), new StringMap(), this.width, this.height);
        for (int i = 0; i < this.chunks.size; i++) {
            ImageChunk<ChunkData> chunk = this.chunks.get(i);
            int requiredProcs = procsRequired(shapes.get(i).size);
            assert requiredProcs <= chunk.data.procs.size;
            Seq<CodeBuilder> code = generateProcessorCode(requiredProcs, shapes.get(i));
            int displayX = chunk.chunkX - display.sizeOffset, displayY = chunk.chunkY - display.sizeOffset;
            schem.tiles.add(new Schematic.Stile(this.display, displayX, displayY, null, (byte) 0));
            for (int j = 0; j < requiredProcs; j++) {
                Point2 pos = chunk.data.procs.get(j);
                ProcessorBuilder procBuilder = new ProcessorBuilder(pos.x, pos.y, code.get(j).toString());
                procBuilder.addAbsoluteLink(displayX, displayY, "display1");
                schem.tiles.add(procBuilder.getStile());
            }
        }
        if (Core.settings.getBool(setting("add-metadata"))) {
            schem.tiles.add(SchematicHandler.anchorBlock.generateStile(procRange, procRange, this.display, this.chunks.map(chunk -> new Point2(chunk.chunkX-procRange, chunk.chunkY-procRange))));
        }
        return schem;
    }

    public static Seq<CodeBuilder> generateProcessorCode(int processors, Seq<Shape> shapes) {
        // One processor only, use the simplified code
        if (processors == 1) {
            CodeBuilder builder = new CodeBuilder();
            builder.extendLines(singleStart);
            int shapeCount = 0;
            for (Shape shape : shapes) {
                shapeCount += 1;
                builder.appendShape(shape);
                if (shapeCount % 128 == 0) {
                    builder.appendLine("drawflush display1");
                }
            }
            if (shapeCount%128 != 0) {
                builder.appendLine("drawflush display1");
            }
            return Seq.with(builder);
        }
        // Multiple processors
        IntSeq shapeCounter = new IntSeq(processors);
        Seq<CodeBuilder> code = new Seq<>();
        for (int i = 0; i < processors; i++) {
            CodeBuilder builder = new CodeBuilder();
            // Append init code to each processor
            builder.extendLines(multiStart);
            for (int j = 0; j < i; j++) {
                builder.extendLines(repeat);
            }
            code.add(builder);
            shapeCounter.add(0);
        }

        int procIndex = 0;
        // This variable is what we modulo procIndex against. It updates once an entire cycle has been completed
        int curFreeProcs = processors;
        // This is the live counter of how many processors will be usable at the end of a cycle.
        int appliedFreeProcs = curFreeProcs;
        for (Shape shape : shapes) {
            CodeBuilder builder = code.get(procIndex);
            builder.appendShape(shape);
            shapeCounter.incr(procIndex, 1);
            int count = shapeCounter.get(procIndex);
            int procMaxShapes = maxShapesForProc(procIndex);
            boolean advance = count % 128 == 0 || count >= procMaxShapes;
            if (advance) {
                if (count % 128 == 0) {
                    builder.appendLine("drawflush display1");
                }
                if (count >= procMaxShapes) {
                    appliedFreeProcs -= 1;
                }
                procIndex = (procIndex+1)%curFreeProcs;
                if (procIndex == 0) {
                    // Start of new cycle, apply new limits;
                    curFreeProcs = appliedFreeProcs;
                    if (curFreeProcs < 0) {
                        Log.err("All processors occupied but there are more shapes to fit");
//                        Log.err("Something went wrong, but we have @/@ shapes, @ left", tmp, shapes.size, shapes.size-tmp);
                        break;
                    }
                }
            }
        }
        CodeGenUtil.padLastFlush(shapeCounter, code, "drawflush display1");
//        for (int i = 0; i < processors; i++) {
//            int procMaxShapes = MAX_SHAPES_PER_PROC - i*(repeat.length/2);
//            Log.info("Proc @, @/@", i, shapeCounter.get(i), procMaxShapes);
//        }
        return code;
    }


    protected void shrinkProcs(ChunkData chunkData, int target) {
        while (chunkData.procs.size > target) {
            Point2 point = chunkData.procs.pop();
            this.preview.tiles.remove(t -> t.x == point.x && t.y == point.y);
            this.occupied.clear(point.y * this.width + point.x);
        }
    }
    /**
     * displayX and displayY should be the center of the display
     */
    protected boolean within(int displayX, int displayY, int x, int y) {
        float range2 = procRange + this.display.size/2f;
        range2*= range2;
        return Mathf.dst2(displayX, displayY, x, y) < range2;
    }


    public static class ChunkData {
        public Seq<Point2> procs = new Seq<>();
    }

}