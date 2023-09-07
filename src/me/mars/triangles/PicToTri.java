package me.mars.triangles;

import arc.Core;
import arc.Events;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.OS;
import me.mars.triangles.schematics.SchematicHandler;
import me.mars.triangles.ui.ConverterDialog;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.mod.Mod;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.SettingsMenuDialog;

import static me.mars.triangles.schematics.SchematicHandler.addDefaultSchematics;


public class PicToTri extends Mod {
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
		Core.settings.defaults(setting("first-run"), false);
		Core.settings.defaults(setting("debug-mode"), false);
		Core.settings.defaults(setting("default-seed"), "");
		Core.settings.defaults(setting("java-loader"), true);

		SchematicHandler.create();
	}

	@Override
	public void init() {
		Log.info("@ running version: @, with @ threads", internalName,  Vars.mods.getMod(internalName).meta.version, OS.cores);
		if (!Core.settings.getBoolOnce(setting("first-run"))) {
			Log.info("First run, adding default schems");
			addDefaultSchematics();
		}
		Vars.ui.settings.addCategory(bundle("mod-name"), t -> {
			t.checkPref(setting("debug-mode"), false, changed -> debugMode = changed);
			t.pref(new SeedSetting(setting("default-seed")));
			t.checkPref(setting("java-loader"), false);
			t.getSettings().add(new HiddenSetting(setting("first-run")));
		});

		BaseDialog converterDialog = new ConverterDialog();
		Vars.ui.schematics.buttons.button(bundle("convert"), converterDialog::show);
		Vars.ui.menufrag.addButton(bundle("convert"), Icon.fileImage, converterDialog::show);

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
}

class SeedSetting extends SettingsMenuDialog.SettingsTable.Setting {

	public SeedSetting(String name) {
		super(name);
	}

	@Override
	public void add(SettingsMenuDialog.SettingsTable table) {
		TextField field = new TextField(Core.settings.getString(this.name));
		field.setFilter(TextField.TextFieldFilter.digitsOnly);
		field.changed(() -> Core.settings.put(this.name, field.getText()));
		Table prefTable = table.table().left().padTop(3f).get();
		prefTable.label(() -> title);
		prefTable.add(field);
		addDesc(prefTable);
		table.row();
	}
}

class HiddenSetting extends SettingsMenuDialog.SettingsTable.Setting {

	public HiddenSetting(String name) {
		super(name);
	}

	@Override
	public void add(SettingsMenuDialog.SettingsTable table) {}
}
