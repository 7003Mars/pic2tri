package me.mars.triangles.schematics;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.DelayedRemovalSeq;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Eachable;
import arc.util.Time;
import arc.util.Tmp;
import me.mars.triangles.PicToTri;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Schematic;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.LogicDisplay;

import static mindustry.Vars.tilesize;

public class LinkAssistBlock extends Block {
	LogicBlock proc;
	public LinkAssistBlock(LogicBlock proc) {
		super(PicToTri.internalName+"-linker-"+proc.name);
		this.localizedName = "\u2206";
		this.description = "Not a real block. Used internally by mod";
		this.size = proc.size;
		this.proc = proc;
		this.rotate = true;
		this.requirements(null, ItemStack.empty);
		this.init();
	}

	@Override
	public void drawPlanConfig(BuildPlan plan, Eachable<BuildPlan> list) {
		LinkData data = (LinkData)plan.config;
		if (data == null) {
			plan.block = Blocks.air;
			return;
		}
		int halfDisplay = data.display.size*tilesize/2;
		for (Point2 p : ((LinkData)plan.config).points) {
			float worldX = p.x * tilesize + halfDisplay-4, worldY = p.y * tilesize + halfDisplay-4;
			boolean valid = Mathf.within(worldX, worldY, plan.x * tilesize+this.offset, plan.y * tilesize+this.offset, this.proc.range+halfDisplay);
			Drawf.square(worldX, worldY, tilesize, Time.time*9/Mathf.pi-Mathf.absin(10f, 90f), valid ? Color.green : Pal.noplace);
		}
		Drawf.dashCircle(plan.x*tilesize, plan.y*tilesize, this.proc.range, Pal.range);
	}

	@Override
	public void onNewPlan(BuildPlan plan) {
		plan.block = this.proc;
		LogicBlock.LogicBuild logicBuild = (LogicBlock.LogicBuild) this.proc.newBuilding();
		logicBuild.tile = new Tile(0, 0);
		logicBuild.code = SchematicHandler.reloadScript;
		LinkData data = (LinkData)plan.config;
		DelayedRemovalSeq<Point2> points = new DelayedRemovalSeq<>(data.points);
		int i = 1;
		int halfDisplay = data.display.size*tilesize/2;
		points.begin();
		for (Point2 p : points) {
			float worldX = p.x * tilesize + halfDisplay - 4, worldY = p.y * tilesize + halfDisplay - 4;
			boolean valid = Mathf.within(worldX, worldY, plan.x * tilesize+this.offset, plan.y * tilesize+this.offset, this.proc.range+halfDisplay);
			if (valid) points.remove(p);
			logicBuild.links.add(new LogicBlock.LogicLink(p.x-plan.x, p.y-plan.y, "display"+(i++), true));
		}
		points.end();
		logicBuild.links.add(new LogicBlock.LogicLink(data.buttonPos.x, data.buttonPos.y, "switch1", true));
		plan.config = logicBuild.config();
		if (points.any()) {
//			Log.info("Points @", points);
			Core.app.post(() -> Vars.control.input.useSchematic(this.generateSchematic(points, data.display)));
		} else {
			Schematic airSchem = new Schematic(new Seq<>(), new StringMap(), 0, 0);
			Vars.control.input.useSchematic(airSchem);
		}
	}

	@Override
	public void loadIcon() {
		this.fullIcon = this.proc.fullIcon;
		this.uiIcon = this.proc.uiIcon;
	}

	@Override
	public Object pointConfig(Object config, Cons<Point2> transformer) {
		// Stolen code from LogicBlock, don't ask how it works
		Point2 p = ((LinkData)config).buttonPos;
		Tmp.p2.set((int)(offset / (tilesize/2)), (int)(offset / (tilesize/2)));
		transformer.get(Tmp.p1.set(p.x * 2, p.y * 2).sub(Tmp.p2));
		Tmp.p1.add(Tmp.p2);
		Tmp.p1.x /= 2;
		Tmp.p1.y /= 2;
		p.set(Tmp.p1);
		return config;
	}

	@Override
	public boolean isHidden() {
		return true;
	}

	@Override
	public boolean isVisible() {
		return true;
	}

	public static class LinkData {
		Point2 buttonPos;
		LogicDisplay display;
		Point2[] points;
		public LinkData(LogicDisplay display, Point2 buttonPos, Point2[] points) {
			this.buttonPos = buttonPos;
			this.display = display;
			this.points = points;
		}
	}

	public Schematic generateSchematic(Seq<Point2> points,LogicDisplay display) {
		Schematic.Stile button = new Schematic.Stile(Blocks.switchBlock, 0, 0, null, (byte) 0);
		int offset = -this.sizeOffset;
		LinkAssistBlock.LinkData data = new LinkAssistBlock.LinkData(display, new Point2(-offset-1, -offset), points.toArray(Point2.class));
		Schematic.Stile linker = new Schematic.Stile(this, offset+1, offset, data, (byte) 0);
		return new Schematic(Seq.with(button, linker), new StringMap(), 1+this.size, this.size);
	}
}
