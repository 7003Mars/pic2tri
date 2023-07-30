package me.mars.triangles.shapes;

import arc.math.Angles;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.struct.Sort;
import arc.util.Log;
import arc.util.Structs;
import me.mars.triangles.MutateMap;

import java.util.Comparator;

public class NewTriangle extends Triangle{
	@Override
	public void fill(MutateMap pixmap) {
		Point2[] ps = {new Point2(this.x1, this.y1), new Point2(this.x2, this.y2), new Point2(x3, y3)};
		float mx = 0, my = 0;
		for (Point2 p : ps) {
			mx += p.x;
			my += p.y;
		}
		mx/=3f;
		my/=3f;
		float finalMx = mx;
		float finalMy = my;
		Log.info("Mid is @, @", mx, my);
		Sort.instance().sort(ps, Comparator.comparing(p -> -Angles.angle(finalMx, finalMy, p.x, p.y)));
		Structs.each(p -> Log.info("Point is @ ,@", p.x, p.y), ps);
		// Should be ps[1] - ps[0]
		Vec2 vec1 = new Vec2(-ps[0].x+ps[1].x, -ps[0].y+ps[1].y);
		Vec2 vec2 = new Vec2(-ps[1].x+ps[2].x, -ps[1].y+ps[2].y);
		Vec2 vec3 = new Vec2(-ps[2].x+ps[0].x, -ps[2].y+ps[0].y);
		Log.info("vecs are \n@\n@\n@", vec1, vec2, vec3);
		int minX = Structs.findMin(ps, p -> p.x).x, maxX = Structs.findMin(ps, p -> -p.x).x;
		int minY = Structs.findMin(ps, p -> p.y).y, maxY = Structs.findMin(ps, p -> -p.y).y;
		Log.info("X: @-@, Y: @-@", minX, maxX, minY, maxY);
		for (int cx = minX; cx <= maxX; cx++) {
			for (int cy = minY; cy <= maxY; cy++) {
				boolean allIn;
				Vec2 pv = new Vec2();
				pv.set(ps[0].x-cx+0.5f, ps[0].y-cy+0.5f);
				float area1 = vec1.crs(pv);
				pv.set(ps[1].x-cx+0.5f, ps[1].y-cy+0.5f);
				float area2 = vec2.crs(pv);
				pv.set(ps[2].x-cx+0.5f, ps[2].y-cy+0.5f);
				float area3 = vec3.crs(pv);
				allIn =  area1 == 0 ? vec1.y > 0 || (vec1.y == 0 && vec1.x > 0) : area1 > 0;
				allIn &= area2 == 0 ? vec2.y > 0 || (vec2.y == 0 && vec2.x > 0) : area2 > 0;
				allIn &= area3 == 0 ? vec3.y > 0 || (vec3.y == 0 && vec3.x > 0) : area3 > 0;
				if (allIn) pixmap.mark(pixmap.obtainLine().set(cx-1, cx-1, cy-1));
			}
		}

	}

}
