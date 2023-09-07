package me.mars.triangles.schematics;

import arc.Core;
import arc.Events;
import arc.struct.Seq;
import arc.struct.StringMap;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.EventType;
import mindustry.game.Schematic;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.LogicBlock;

import static me.mars.triangles.PicToTri.setting;

public class SchematicHandler {
	static final String reloadScript = """
			print "For use with the PicToTri mod. Link to all the displays you wish to load then click the switch"
			sensor e switch1 @enabled
			jump 1 notEqual e 1
			set i 0
			getlink b i
			sensor type b @type
			jump 10 equal type @logic-display
			jump 10 equal type @large-logic-display
			jump 12 equal type @switch
			jump 13 always 0 0
			draw clear 0 0 0 0 0 0
			drawflush b
			control enabled b 0 0 0 0
			op add i i 1
			set t @tick
			jump 15 equal t @tick
			jump 4 notEqual i @links
			""";

	public static ImageAnchorBlock anchorBlock;
	public static LinkAssistBlock microLink, link, hyperLink;

	public static void create() {
		Events.on(EventType.ContentInitEvent.class, contentInitEvent -> {
			anchorBlock = new ImageAnchorBlock();
			microLink = new LinkAssistBlock((LogicBlock) Blocks.microProcessor);
//			link = new LinkAssistBlock((LogicBlock) Blocks.logicProcessor);
//			hyperLink = new LinkAssistBlock((LogicBlock) Blocks.hyperProcessor);

			// Gotta reload them due to the new blocks
			Vars.schematics.load();
		});
	}


	public static void addDefaultSchematics() {
		Schematic.Stile button = new Schematic.Stile(Blocks.switchBlock, 0, 0 , null, (byte) 0);
		Block[] processors = {Blocks.microProcessor, Blocks.logicProcessor, Blocks.hyperProcessor};
		LogicBlock.LogicBuild logicBuild = (LogicBlock.LogicBuild) Blocks.microProcessor.newBuilding();
		logicBuild.updateCode(reloadScript);
		logicBuild.tile = new Tile(0, 0);
		for (Block processor: processors) {
			int offset = -processor.sizeOffset;
			logicBuild.links.clear();
			logicBuild.tile = new Tile(1+offset, offset);
			logicBuild.links.add(new LogicBlock.LogicLink(0, 0, "switch1", true));
			Schematic.Stile procTile = new Schematic.Stile(processor, 1+offset, offset,
					logicBuild.config(), (byte) 0);
			Schematic schem = new Schematic(Seq.with(button, procTile), StringMap.of("name", "Display loader"),
					1+processor.size, processor.size);
			schem.labels.add(Core.bundle.get(setting("mod-name")));
			Vars.schematics.add(schem);
		}
	}


}

