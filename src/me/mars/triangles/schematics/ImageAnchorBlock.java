package me.mars.triangles.schematics;

import arc.Core;
import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Eachable;
import arc.util.Log;
import arc.util.Structs;
import me.mars.triangles.PicToTri;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.ctype.Content;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Schematic;
import mindustry.game.Schematic.Stile;
import mindustry.world.Block;
import mindustry.world.blocks.logic.LogicDisplay;
import mindustry.world.meta.BuildVisibility;

public class ImageAnchorBlock extends Block {

	public ImageAnchorBlock() {
		super(PicToTri.internalName+"-anchor");
		this.size = 0;
		this.rotate = true;
		this.buildVisibility = BuildVisibility.shown;
		this.category = null;
		this.init();
	}

	@Override
	public void onNewPlan(BuildPlan plan) {
		// Schematic was rotated, ignore it
		if (plan.rotation != 0) return;
		Object config = plan.config;
		if (config instanceof Object[] objArray &&
				objArray.length == 2 && objArray[0] instanceof Content content &&
				objArray[1] instanceof Point2[] rawPoints) {
			Seq<Point2> points = new Seq<>(rawPoints).map(p -> new Point2().set(p).add(plan.x, plan.y));
			LogicDisplay display = (LogicDisplay) content;
			Core.app.post(() -> {
				// TODO Add support for links and hyperLinks(Based on schematic size). Also deal with rotation offset hell
				LinkAssistBlock linkBlock = SchematicHandler.microLink;
				Stile button = new Stile(Blocks.switchBlock, 0, 0, null, (byte) 0);
				int offset = -linkBlock.sizeOffset;
				Stile linker = new Stile(linkBlock, offset+1, offset, new LinkAssistBlock.LinkData(display, points.toArray(Point2.class)), (byte) 0);
				Schematic schem = new Schematic(Seq.with(button, linker), new StringMap(), 1+linkBlock.size, linkBlock.size);
				Vars.control.input.useSchematic(schem);
			});

		} else {
			Log.err("Invalid anchor data. Should be array{Content, Point2[]}, is @", config);
		}
		plan.block = Blocks.air;
	}

	@Override
	public void drawPlan(BuildPlan plan, Eachable<BuildPlan> list, boolean valid) {}

	@Override
	public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {}

	public Stile generateStile(int x, int y, LogicDisplay displayType, Seq<Point2> points) {
		return new Stile(this, x, y, Structs.arr(displayType, points.toArray(Point2.class)), (byte) 0);
	}
}
