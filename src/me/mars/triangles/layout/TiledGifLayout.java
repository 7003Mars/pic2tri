package me.mars.triangles.layout;

import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import me.mars.triangles.shapes.Shape;
import mindustry.logic.LExecutor;
import mindustry.world.blocks.logic.TileableLogicDisplay;

import java.util.*;

public class TiledGifLayout extends TiledDisplayLayout {
    // region Mlog code
    // Watcher
    private static final int WATCHER_REPEAT_COUNT = 396;
    public static final String watcher;
    static {
        String[] watcherStart = """
                read controller processor1 "controller"
                sensor valid controller @dead
                jump 0 equal valid 1
                read display controller "display"
                read switch controller "switch"
                """.split("\n");
        String[] repeat = """
                sensor op display @operations
                jump begin equal op 0
                """.split("\n");
        String[] watcherEnd = """
                sensor enabled switch @enabled
                jump 0 equal enabled 1
                begin:
                write 1 controller "reset"
                read reset controller "reset"
                jump $-1 equal reset 1
                """.split("\n");
        CodeBuilder code = new CodeBuilder();
        code.extendLines(watcherStart);
        for (int i = 0; i < WATCHER_REPEAT_COUNT; i++) {
            code.extendLines(repeat);
        }
        code.extendLines(watcherEnd);
        watcher = code.toString();
    }
    // Controller
    public static String getController(int initProcs, int frames, int frameInterval) {
        String[] controllerStart = """
                set controller @this
                set switch switch1
                set display display1
                jump 0 equal display null
                set reset 0
                control enabled switch 1 0 0 0
                draw reset 0 0 0 0 0 0
                draw translate _ _ 0 0 0 0
                drawflush display
                """.replace("_", String.valueOf(DRAW_TRANSLATE_OFFSET)).split("\n");
        CodeBuilder code = new CodeBuilder();
        // Init code
        code.extendLines(controllerStart);
        // Init frames
        for (int i = -initProcs; i < 0; i++) {
            code.appendLine("set i " + i);
            code.appendLine("wait 1e-4");
        }
        code.appendLine("set i null");
        code.appendLine("wait " + Mathf.ceil(TiledDisplayLayout.executorCycleTime));
        for (int i = 0; i < 9; i++) {
            code.appendLine("wait 0"); // Bunch of no-ops to empty the accumulator
        }
        code.appendLine("first:");
        code.appendLine("set i 0");
        code.appendLine("wait 0");
        code.appendLine("inc:");
        // Actual loop begins here
        code.appendLine("op add i i 1");
        for (int i = 0; i < frameInterval-1; i++) {
            code.appendLine("wait 1e-4");
        }
        code.appendLine("jump inc lessThan i " + frames);
        // REMOVEME
        code.appendLine("draw clear 255 255 255 0 0 0");
        code.appendLine("drawflush display");
        code.appendLine("jump first equal reset false");
        // END
//        code.appendLine("jump first equal reset false");
        return code.toString();
    }
    // Worker
    public static int WORKER_MAX_FRAME_SHAPES = LExecutor.maxGraphicsBuffer/2;
    public static String[] workerStart = """
            read controller processor1 "controller"
            read display controller "display"
            sensor valid controller @dead
            jump 0 equal valid 1
            set t @tick
            jump $ equal t @tick
            """.split("\n");
    // We need to no-op for an even number of instructions.
    public static String[] workerFlush = """
            read i controller "i"
            jump $-1 notEqual i _
            drawflush display
            wait 0
            """.split("\n");

    public static int getWorkerMinFrames(int frameInterval) {
        // Assuming timings are so tight that workers only have time to complete one frame every cycle, how many instructions will a worker have?
        // Also assume each frame uses the maximum possible number of shapes
        int singleFrameInstructionCount = workerStart.length + WORKER_MAX_FRAME_SHAPES*2 + workerFlush.length;
        // Number of ticks needed for this worker to complete its frame and loop.
        int period = Mathf.ceil((float) singleFrameInstructionCount /2);
        return Mathf.ceil((float) period /frameInterval);
    }
    // endregion

    public int frameInterval;

    public TiledGifLayout(TileableLogicDisplay tiledDisplay, int imageWidth, int imageHeight, int chunkSize, int frameInterval) {
        super(tiledDisplay, imageWidth, imageHeight, chunkSize);
        this.frameInterval = frameInterval;
    }


    // region UI METHODS

    public int minFrameCount() {
        return getWorkerMinFrames(this.frameInterval);
    }

    // endregion

    /**
     * How many shapes are used for init in the chunks of shapes given
     */
    protected int totalInitShapes(Seq<Seq<Shape>> shapes) {
        int shapeCount = 0;
        for (int i = 0; i < this.chunks.size; i++) {
            shapeCount += shapes.get(i).size;
        }
        return shapeCount;
    }


    @Override
    protected String generateControllerCode(Seq<Seq<Shape>> shapes) {
        int initProcsRequired = procsRequired(totalInitShapes(shapes)), frameCount = shapes.size - this.chunks.size;
        return getController(initProcsRequired, frameCount, this.frameInterval);
    }

    @Override
    protected Seq<String> generateWorkerCode(Seq<Seq<Shape>> shapes) {
        // TODO IMPT VERY ugly temp code, either cleanup or document
        Seq<String> res = new Seq<>();
        int initProcsRequired = procsRequired(totalInitShapes(shapes)), frameCount = shapes.size - this.chunks.size;
        res.add(super.generateWorkerCode(new Seq<>(true, shapes.toArray(Seq.class), 0, this.chunks.size)));
        Seq<Seq<Shape>> frames = new Seq<>(true, shapes.toArray(Seq.class), this.chunks.size, frameCount);
        frames.each(frame -> frame.each(shape -> shape.translate(-DRAW_TRANSLATE_OFFSET, -DRAW_TRANSLATE_OFFSET)));
        // We need to increase the number of processors to include the new frames
        Seq<String> gifCode = generateGifWorkerCode(frames);
        this.requestProcs(initProcsRequired + gifCode.size + 1); // One extra worker is used as the watcher. It should be placed at the bottom left
        Point2 botLeft = this.workerPos.copy().min((a, b) -> {
            int result = a.y - b.y;
            return result != 0 ? result : a.x - b.x;
        });
        res.add(gifCode);
        res.insert(this.workerPos.indexOf(botLeft), watcher);
        return res;
    }

    public Seq<String> generateGifWorkerCode(Seq<Seq<Shape>> frames) {
        // Thanks gemini, deepseek, qwen and chatgpt for all those algos (sorry earth)
        // REMOVEME
        Log.info("Building gif with frames of sizes\n@",frames.mapInt(f -> f.size));
        int minShapes = 256;
        Seq<Frame> frameStartTimes = new Seq<>();
        for (int frameIndex = 0; frameIndex < frames.size; frameIndex++) {
            Seq<Shape> frame = frames.get(frameIndex);
            if (frame.isEmpty()) continue;
            if (frame.size < minShapes) minShapes = frame.size;
            frameStartTimes.add(new Frame(frameIndex*this.frameInterval, frame));
        }
        // REMOVEME
        Log.info("Min shapes @", minShapes);
        frameStartTimes.sort(frame -> frame.startTick);
        int gifDuration = frames.size*this.frameInterval;
        TreeMap<Integer, TreeSet<Worker>> workers = new TreeMap<>();
        Seq<Worker> fullWorkers = new Seq<>();
        for (Frame frame : frameStartTimes) {
            Worker foundWorker = null;
            // Use an iterator so we can safely remove entries on the fly
            for (var treeIterator = workers.tailMap(frame.getInstructions()).entrySet().iterator(); treeIterator.hasNext(); ) {
                Map.Entry<Integer, TreeSet<Worker>> entry = treeIterator.next();
                TreeSet<Worker> workerSet = entry.getValue();
                Iterator<Worker> setIterator = workerSet.iterator();
                while (setIterator.hasNext()) {
                    Worker worker = setIterator.next();
                    // No point doing any cleanup for future frames. Could technically headSet() to create a view but we'd need to make a fake worker.
                    if (worker.nextFreeTick() >= frame.startTick) break;
                    if (frame.targetTick >= worker.firstFrameBeginTick() + gifDuration) {
                        // Expired worker, remove it
                        fullWorkers.add(worker);
                        setIterator.remove();
                    }
                }
                if (workerSet.isEmpty()) {
                    treeIterator.remove();
                    continue;
                }
                Worker earliestWorker = workerSet.first();
                if (earliestWorker.nextFreeTick() < frame.startTick) {
                    foundWorker = earliestWorker;
                    workerSet.pollFirst();
                    if (workerSet.isEmpty()) {
                        treeIterator.remove();
                    }
                    break;
                }
            }
            if (foundWorker == null) {
                foundWorker = new Worker();
            }
            foundWorker.addFrame(frame);
            if (foundWorker.capacity < minShapes*2) {
                fullWorkers.add(foundWorker);
                continue;
            }
            workers.computeIfAbsent(foundWorker.capacity, k -> new TreeSet<>(Comparator.comparing(Worker::nextFreeTick).thenComparing(System::identityHashCode))).add(foundWorker);
        }
        Seq<Worker> allWorkers = new Seq<>();
        // REMOVEME
        Log.info("There are @ frames", frames.size);
        for (Map.Entry<Integer, TreeSet<Worker>> entry : workers.entrySet()) {
            TreeSet<Worker> queue = entry.getValue();
            while (!queue.isEmpty()) {
                Worker worker = queue.pollFirst();
                addWorker(frames.size, this.frameInterval, allWorkers, worker);
            }
        }
        Log.info("Adding @ full workers", fullWorkers.size);
        for (Worker worker : fullWorkers) {
            addWorker(frames.size, this.frameInterval, allWorkers, worker);
        }
        // REMOVEME
        for (Worker worker : allWorkers) {
            int instructionsUsed = LExecutor.maxInstructions-workerStart.length-worker.capacity;
            Log.info("Worker capacity: @(@%). Worker has @ frames, frames begin at @, @. Free tick at @",
                    instructionsUsed, (float) (instructionsUsed)/LExecutor.maxInstructions*100f,
                    worker.frames.size, worker.firstFrameBeginTick(), worker.firstFrameBeginTick()+gifDuration, worker.nextFreeTick());
        }
        // Actual code generation
        Seq<CodeBuilder> code = new Seq<>();
        for (int i = 0; i < allWorkers.size; i++) {
            CodeBuilder builder = new CodeBuilder();
            code.add(builder);
            Worker worker = allWorkers.get(i);
            // Init code
            builder.extendLines(workerStart);
            for (int j = 0; j < worker.frames.size; j++) {
                // Immediately start drawing
                worker.frames.get(j).shapes.each(builder::appendShape);
                // But wait until the correct frame to flush
                for (String line : workerFlush) {
                    builder.appendLine(line.replace("_", String.valueOf(worker.frames.get(j).targetTick)));
                }
            }
        }
        return code.map(CodeBuilder::toString);
    }

    private static void addWorker(int frameCount, int frameInterval, Seq<Worker> allWorkers, Worker worker) {
        if (worker.nextFreeTick() + Mathf.ceil((float) workerStart.length/2)+1 > worker.frames.first().startTick + frameCount*frameInterval) {
            // REMOVEME
            System.out.println("Worker frame timings:");
            for (Frame frame : worker.frames) {
                System.out.print(Strings.format("[@->@],", frame.startTick, frame.targetTick));
            }
            System.out.println();
            Log.info("Worker free at @+@=@ but first frame begins at @, next loop at @. Freeing last frame, with new frame count at @",
                    worker.nextFreeTick(), Mathf.ceil((float) workerStart.length/2)+1, worker.nextFreeTick() + Mathf.ceil((float) workerStart.length/2)+1,
                    worker.frames.first().startTick, worker.frames.first().startTick + frameCount*frameInterval, worker.frames.size-1);
            // Extract out last frame into new worker
            Worker extraWorker = new Worker();
            extraWorker.addFrame(worker.frames.pop());
            allWorkers.add(extraWorker);
        }
        allWorkers.add(worker);
    }

    private static class Frame {
        int targetTick;
        int startTick;
        Seq<Shape> shapes;


        public Frame(int targetTick, Seq<Shape> shapes) {
            this.targetTick = targetTick;
            this.shapes = shapes;
            int duration = Mathf.ceil(this.getInstructions()/2f);
            this.startTick = targetTick - duration;
        }

        public int getInstructions() {
            return this.shapes.size * 2 + workerFlush.length;
        }
    }

    private static class Worker {
        Seq<Frame> frames = new Seq<>();
        int capacity = LExecutor.maxInstructions - workerStart.length;

        public void addFrame(Frame frame) {
            this.frames.add(frame);
            this.capacity -= frame.getInstructions();
        }


        public int nextFreeTick() {
            return frames.isEmpty() ? Integer.MIN_VALUE : frames.get(frames.size-1).targetTick;
        }

        public int firstFrameBeginTick() {
            return this.frames.isEmpty() ? Integer.MIN_VALUE : frames.first().startTick;
        }

        public int cycleDuration() {
            Frame firstFrame = this.frames.first(), lastFrame = this.frames.get(this.frames.size-1);
            return lastFrame.targetTick - firstFrame.startTick + Mathf.ceil(workerStart.length)+1;
        }
    }
}
