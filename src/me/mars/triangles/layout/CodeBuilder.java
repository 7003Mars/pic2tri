package me.mars.triangles.layout;

import arc.util.Log;
import me.mars.triangles.PicToTri;
import me.mars.triangles.shapes.Shape;
import me.mars.triangles.shapes.Triangle;
import mindustry.logic.LExecutor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeBuilder {
    public static int MAX_SAFE_COORDINATE = 0b0111111111;
    static Pattern pattern = Pattern.compile("\\$(-?\\d+)?");

    StringBuilder builder = new StringBuilder();
    int lineCount = 0;

    public void appendLine(String line) {
        // The functional version of matcher.replaceAll() is java 9+ only ):
        Matcher matcher = pattern.matcher(line);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String offset = matcher.group(1);
            int targetLine = offset != null ? lineCount + Integer.parseInt(offset) : lineCount;
            matcher.appendReplacement(result, String.valueOf(targetLine));
        }
        matcher.appendTail(result);
        lineCount++;
        builder.append(result);
        if (!line.endsWith("\n")) {
            builder.append("\n");
        }
    }

    private static boolean checkSafeCoordinate(int coord) {
        return Math.abs(coord) <= MAX_SAFE_COORDINATE;
    }

    public void appendShape(Shape shape) {
        if (PicToTri.debugMode) {
            if (shape instanceof Triangle tri) {
                if (!(checkSafeCoordinate(tri.x1) && checkSafeCoordinate(tri.x2) && checkSafeCoordinate(tri.x3) &&
                        checkSafeCoordinate(tri.y1) && checkSafeCoordinate(tri.y2) && checkSafeCoordinate(tri.y3))) {
                    Log.warn("Triangle contains coords which will overflow: @", tri);
                }
            }
        }
        builder.append(shape.toInstr());
        lineCount += 2;
    }

    public void extendLines(String[] lines) {
        for (String line : lines) {
            this.appendLine(line);
        }
    }

    @Override
    public String toString() {
        if (lineCount > LExecutor.maxInstructions) {
            Log.warn("Number of instructions exceeds default limit: @>@", lineCount, LExecutor.maxInstructions);
        }
        return this.builder.toString();
    }
}

