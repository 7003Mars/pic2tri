package me.mars.triangles;

import arc.Core;
import arc.Events;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.OS;
import arc.util.Strings;
import me.mars.triangles.ui.ConverterDialog;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.mod.Mod;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.SettingsMenuDialog;
import static mindustry.ui.dialogs.SettingsMenuDialog.SettingsTable.TextSetting;

public class PicToTri extends Mod {
	public static final String internalName = "triangles";
	public static volatile boolean debugMode = false;

	@Override
	public void init() {
		Log.info("@ running version: @, with @ threads", internalName,  Vars.mods.getMod(internalName).meta.version, OS.cores);

		Vars.ui.settings.addCategory("Triangles", t -> {
			t.checkPref(setting("debug-mode"), false, changed -> debugMode = changed);
			t.pref(new SeedSetting(setting("default-seed")));
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
