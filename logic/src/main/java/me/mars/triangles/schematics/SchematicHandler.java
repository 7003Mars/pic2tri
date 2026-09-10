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

import static me.mars.triangles.utils.Prefs.setting;

public class SchematicHandler {
	// Mindcode src:
	// eJxlUMFq60AMvPsrpjcHGic9B4NPPfXQQ35gY8uxqLoy3jUhkI+Pdu285FEwazQazQyz2+3wqRPmQLhwHBAHwje3Rz1OjF/tKnyx/0FUOJG87TiM4q4BV53tJtiNQtR1aevRCrfGN2IwwXYoCvLuJNShXpGPqlmhQ1H0Zs5gj31VVY2YV0CnBXASNZ0aZ4oJLXlzMJR7vJV5VTXxOhLqGo3omdvtmgu323L7SnDTmbb/0TY5rSkCrfrIfqakT95CZZvyEfuppyNNLrL6kFT3rxJCbir370hfDgp0k7v0Modhybuii9Szkt5JyM5AtLGJ1t4yXgYWSuCKLrX8i5h/1q29lvYhuCZ6FP3HJ/HvkbGdTw==
	static final String reloadScript = """
		jump 2 always 0 0
		print "For use with the PicToTri mod. Link to all the displays you wish to load then click the switch"
		sensor :enabled switch1 @enabled
		set :i 0
		jump 22 greaterThanEq 0 @links
		getlink :block :i
		sensor *tmp2 :block @type
		op equal *tmp3 *tmp2 @logic-display
		op equal *tmp5 *tmp2 @large-logic-display
		op or *tmp6 *tmp3 *tmp5
		jump 20 equal *tmp6 false
		sensor *tmp9 :block @operations
		op equal *tmp10 *tmp9 0
		op or *tmp11 :enabled *tmp10
		jump 20 equal *tmp11 false
		draw clear 0 0 0 0 0 0
		drawflush :block
		control enabled :block false 0 0 0
		set :t @tick
		jump 19 equal :t @tick
		op add :i :i 1
		jump 5 lessThan :i @links
		jump 0 equal :enabled false
		control enabled switch1 false 0 0 0
		""";

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
			Schematic schem = new Schematic(Seq.with(button, procTile), StringMap.of("name", "Display loader v2"),
					1+processor.size, processor.size);
			schem.labels.add(Core.bundle.get(setting("mod-name")));
			Vars.schematics.add(schem);
		}
	}


}

