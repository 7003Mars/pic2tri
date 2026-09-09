package me.mars.triangles.shapes;

import arc.graphics.Color;
import arc.util.io.Reads;
import arc.util.io.Writes;

public class ShapeIO {
    public static void writeShape(Writes write, Shape shape) {
        if (shape instanceof FillShape fillShape) {
            write.b(1);
            write.i(fillShape.x);
            write.i(fillShape.y);
            write.i(fillShape.w);
            write.i(fillShape.h);
        } else if (shape instanceof Triangle triangle) {
            write.b(2);
            write.i(triangle.x1);
            write.i(triangle.y1);
            write.i(triangle.x2);
            write.i(triangle.y2);
            write.i(triangle.x3);
            write.i(triangle.y3);
        } else if (shape instanceof Rectangle rectangle) {
            write.b(3);
            write.i(rectangle.x1);
            write.i(rectangle.y1);
            write.i(rectangle.x2);
            write.i(rectangle.y2);
        } else {
            throw new IllegalArgumentException("Unknown shape type: " + shape.getClass());
        }
        write.i(Color.packRgba(shape.r, shape.g, shape.b, shape.a));
    }

    public static Shape readShape(Reads read) {
        byte type = read.b();
        Shape shape;
        if (type == 1) {
            FillShape fillShape = new FillShape(0, 0, 0);
            fillShape.x = read.i();
            fillShape.y = read.i();
            fillShape.w = read.i();
            fillShape.h = read.i();
            shape = fillShape;
        } else if (type == 2) {
            Triangle triangle = new Triangle();
            triangle.x1 = read.i();
            triangle.y1 = read.i();
            triangle.x2 = read.i();
            triangle.y2 = read.i();
            triangle.x3 = read.i();
            triangle.y3 = read.i();
            shape = triangle;
        } else if (type == 3) {
            Rectangle rectangle = new Rectangle();
            rectangle.x1 = read.i();
            rectangle.y1 = read.i();
            rectangle.x2 = read.i();
            rectangle.y2 = read.i();
            shape = rectangle;
        } else {
            throw new IllegalArgumentException("Unknown shape type: " + type);
        }
        int col = read.i();
        shape.r = (short) Color.ri(col);
        shape.g = (short) Color.gi(col);
        shape.b = (short) Color.bi(col);
        shape.a = (short) Color.ai(col);
        return shape;
    }
}
