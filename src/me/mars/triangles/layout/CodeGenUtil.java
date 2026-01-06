package me.mars.triangles.layout;

import arc.struct.IntSeq;
import arc.struct.Seq;

public class CodeGenUtil {
    public static final String[] drawDelay = """
            op add j j 1
            jump $-1 lessThan j @
            """.split("\n");

    /**
     * Pads the last flush such that the ending draws also take up 256 instructions.
     * @param shapeCounter Seq keeping track of how many shapes was appended to each CodeBuilder
     * @param code Seq of CodeBuilders
     * @param flushCmd The command used to flush the buffer
     */
    static void padLastFlush(IntSeq shapeCounter, Seq<CodeBuilder> code, String flushCmd) {
        for (int i = 0; i < shapeCounter.size; i++) {
            if (shapeCounter.get(i) % 128 == 0) continue; // Perfectly aligned, no need for this delayed flush.
            CodeBuilder builder = code.get(i);
            int remaining = 128-(shapeCounter.get(i) % 128) - 1 /*Just entering the loop already takes 2 instructions*/;
            for (String line : drawDelay) {
                builder.appendLine(line.replace("@", String.valueOf(remaining)));
            }
            builder.appendLine(flushCmd);
        }

    }
}
