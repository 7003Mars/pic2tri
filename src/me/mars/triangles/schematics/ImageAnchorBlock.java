package me.mars.triangles.schematics;

import arc.Core;
import arc.graphics.Color;
import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.Log;
import arc.util.Structs;
import me.mars.triangles.PicToTri;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.ctype.Content;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Schematic.Stile;
import mindustry.graphics.Drawf;
import mindustry.world.Block;
import mindustry.world.blocks.logic.LogicDisplay;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.modules.ItemModule;

import static me.mars.triangles.PicToTri.setting;

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
		if (plan.rotation != 0) {
			plan.block = Blocks.air;
			return;
		}
		Object config = plan.config;
		if (config instanceof Object[] objArray && objArray.length == 3 &&
				objArray[0] instanceof Integer ver && ver == 1 &&
				objArray[1] instanceof Content content &&
				objArray[2] instanceof Point2[] rawPoints) {
			Seq<Point2> points = new Seq<>(rawPoints).map(p -> p.cpy().add(plan.x, plan.y));
			LogicDisplay display = (LogicDisplay) content;
			LinkAssistBlock linkBlock = getSuitableBlock(points, display);
			if (Core.settings.getBool(setting("link-assist"))) {
				Core.app.post(() -> Vars.control.input.useSchematic(linkBlock.generateSchematic(points, display)));
			}

		} else {
			Log.err("Invalid anchor data. Should be array{Int, Content, Point2[]}, is @",
					config instanceof Object[] arr ? new Seq<>(arr) : config);
		}
		plan.block = Blocks.air;
	}

	@Override
	public void drawPlan(BuildPlan plan, Eachable<BuildPlan> list, boolean valid) {
		// TODO: Show a warning if the schematic was rotated
		if (!PicToTri.debugMode) return;
		Drawf.select(plan.x*8, plan.y*8, 4, Color.black);
		Object config = plan.config;
		if (config instanceof Object[] objects && objects[2] instanceof Point2[] points) {
			for (Point2 p : points) {
				Drawf.square(p.x*8+plan.x*8, p.y*8+plan.y*8, 4, Color.white);
			}
		}
	}

	@Override
	public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {}

	@Override
	public boolean isHidden() {
		return true;
	}

	@Override
	public boolean isVisible() {
		return true;
	}

	public Stile generateStile(int x, int y, LogicDisplay displayType, Seq<Point2> points) {
		return new Stile(this, x, y, Structs.arr(1, displayType, points.toArray(Point2.class)), (byte) 0);
	}

	static LinkAssistBlock getSuitableBlock(Seq<Point2> points, LogicDisplay display) {
		Seq<LinkAssistBlock> blockPriority = Seq.with(SchematicHandler.hyperLink, SchematicHandler.link, SchematicHandler.microLink);
		int width = points.max(p -> p.x).x - points.min(p -> p.x).x, height = points.max(p -> p.y).y - points.min(p -> p.y).y;
		int size = Math.max(width, height)*8 + display.size*4;
		boolean sandbox = Vars.state.rules.infiniteResources;
		ItemModule items = Vars.player.team().items();
		for (int i = 0; i < blockPriority.size; i++) {
			LinkAssistBlock block = blockPriority.get(i);
			if (!(sandbox || items.has(block.proc.requirements))) continue;
			if ((i+1) < blockPriority.size) {
				LinkAssistBlock next = blockPriority.get(i+1);
				if (next.proc.range*2 > size) continue;
			}
			return block;
		}
		return SchematicHandler.microLink;
	}
}
