package me.mars.triangles.schematics;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.util.Eachable;
import arc.util.Time;
import me.mars.triangles.PicToTri;
import mindustry.entities.units.BuildPlan;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.LogicDisplay;
import mindustry.world.meta.BuildVisibility;

import static mindustry.Vars.tilesize;

public class LinkAssistBlock extends Block {
	LogicBlock proc;
	public LinkAssistBlock(LogicBlock proc) {
		super(PicToTri.internalName+"-linker-"+proc.name);
		this.size = proc.size;
		this.proc = proc;
		this.rotate = true;
		this.buildVisibility = BuildVisibility.shown;
		this.category = null;
		this.init();
	}

	@Override
	public void drawPlanConfig(BuildPlan plan, Eachable<BuildPlan> list) {
		// TODO: Figure out why displayRange is more than actual range
		// Remove the -4 once you figure out why
		int displayRange = ((LinkData)plan.config).display.size*tilesize/2-4;
		for (Point2 p : ((LinkData)plan.config).points) {
			float worldX = p.x * tilesize, worldY = p.y * tilesize;
			// TODO: Range does not currently account for display size.
			boolean valid = Mathf.within(worldX, worldY, plan.x * tilesize, plan.y * tilesize, this.proc.range+displayRange);
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
		int i = 1;
		for (Point2 p : ((LinkData)plan.config).points) {
			logicBuild.links.add(new LogicBlock.LogicLink(p.x-plan.x, p.y-plan.y, "display"+(i++), true));
		}
		int offset = -this.proc.sizeOffset+1;
		int rot = plan.rotation+2;
		logicBuild.links.add(new LogicBlock.LogicLink(Geometry.d4x(rot)*offset, Geometry.d4y(rot)*offset, "switch1", true));
		plan.config = logicBuild.config();
	}

	@Override
	public void loadIcon() {
		this.fullIcon = this.proc.fullIcon;
		this.uiIcon = this.proc.uiIcon;
	}

	public static class LinkData {
		LogicDisplay display;
		Point2[] points;
		public LinkData(LogicDisplay display, Point2[] points) {
			this.display = display;
			this.points = points;
		}
	}
}
