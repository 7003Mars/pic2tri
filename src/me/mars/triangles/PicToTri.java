package me.mars.triangles;

import arc.Core;
import arc.Events;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.OS;
import me.mars.triangles.ui.ConverterDialog;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.EventType;
import mindustry.game.Schematic;
import mindustry.mod.Mod;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.SettingsMenuDialog;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.LogicBlock;


public class PicToTri extends Mod {
	private static final String reloadScript = """
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

	public static final String internalName = "triangles";
	public static final String pixmapCheck = internalName+"-pixmap-load-fail";

	public static volatile boolean debugMode = false;

	public PicToTri() {
		Core.settings.defaults(pixmapCheck, false);
		if (Core.settings.getBool(pixmapCheck)) {
			Core.settings.put(pixmapCheck, false);
			Core.settings.put(setting("java-loader"), true);
			Log.warn("Game crashed while loading image, switching to PixmapIO");
		}
	}

	@Override
	public void init() {
		Log.info("@ running version: @, with @ threads", internalName,  Vars.mods.getMod(internalName).meta.version, OS.cores);
		if (!Core.settings.getBoolOnce(setting("first-run"))) addDefaultSchematics();
		Vars.ui.settings.addCategory(bundle("mod-name"), t -> {
			t.checkPref(setting("debug-mode"), false, changed -> debugMode = changed);
			t.pref(new SeedSetting(setting("default-seed")));
			t.checkPref(setting("java-loader"), false);
			Core.settings.defaults(setting("default-seed"), "");
		});

		BaseDialog converterDialog = new ConverterDialog();
		Vars.ui.schematics.buttons.button(bundle("convert"), converterDialog::show);

		Events.on(EventType.ClientLoadEvent.class, clientLoadEvent -> {
			debugMode = Core.settings.getBool(setting("debug-mode"));
			if (debugMode) converterDialog.show();
		});
	}

	public static String bundle(String name) {
		return "@"+internalName+"." + name;
	}

	public static String setting(String name) {
		return internalName + "." + name;
	}

	public void addDefaultSchematics() {
		Log.info("First run, adding default schems");
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

class SeedSetting extends SettingsMenuDialog.SettingsTable.Setting {

	public SeedSetting(String name) {
		super(name);
	}

	@Override
	public void add(SettingsMenuDialog.SettingsTable table) {
		TextField field = new TextField("");
		field.setFilter(TextField.TextFieldFilter.digitsOnly);
		field.changed(() -> Core.settings.put(this.name, field.getText()));
		Table prefTable = table.table().left().padTop(3f).get();
		prefTable.label(() -> title);
		prefTable.add(field);
		addDesc(prefTable);
		table.row();
	}
}
