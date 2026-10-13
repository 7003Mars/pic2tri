package me.mars.triangles.mod;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.OS;
import me.mars.triangles.converter.GeneratorCache;
import me.mars.triangles.mod.ui.ConverterDialog;
import me.mars.triangles.utils.Prefs;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.mod.Mod;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.SettingsMenuDialog;

import static me.mars.triangles.schematics.SchematicHandler.addDefaultSchematics;
import static me.mars.triangles.utils.Prefs.internalName;
import static me.mars.triangles.utils.Prefs.setting;


public class PicToTri extends Mod {
	public static final String pixmapCheck = internalName+"-pixmap-load-fail";


	public PicToTri() {
		Core.settings.defaults(pixmapCheck, false);
		if (Core.settings.getBool(pixmapCheck)) {
			Core.settings.put(pixmapCheck, false);
			Core.settings.put(setting("java-loader"), true);
			Log.warn("Game crashed while loading image, switching to PixmapIO");
		}
		Core.settings.defaults(setting("first-run"), false);
	}

	@Override
	public void init() {
		Log.info("@ running version: @, with @ threads", internalName,  Vars.mods.getMod(internalName).meta.version, OS.cores);
		if (Core.settings.getBoolOnce(setting("first-run"))) {
			Log.info("First run, adding default schems");
			addDefaultSchematics();
		}
		if (Core.settings.getBool(setting("cache.enabled"))) {
			GeneratorCache.deleteOldCache(Core.settings.getInt(setting("cache.max-age")));
		}

		Vars.ui.settings.addCategory(bundle("mod-name"), t -> {
			t.checkPref(setting("debug-mode"), false, changed -> Prefs.debugMode = changed);
			t.checkPref(setting("java-loader"), false);
			t.getSettings().add(new HiddenSetting(setting("first-run")));
			// Cache settings
			t.checkPref(setting("cache.enabled"), true, changed -> Prefs.cacheEnabled = changed);
			t.pref(new IntegerSetting(setting("cache.max-age"), 30));
			t.pref(new FakeSetting(tb -> {
				tb.button(bundle("cache.clear"), () -> {
					Vars.ui.showConfirm(bundle("cache.clear"), () -> {
						GeneratorCache.deleteOldCache(0);
						Vars.ui.showInfoFade("@done");
					});
				});
				tb.row();
				tb.button(bundle("cache.open-folder"),() -> Core.app.openFolder(Prefs.genCacheDir.absolutePath()));
				tb.row();
			}));
		});

		BaseDialog converterDialog = new ConverterDialog();
		Vars.ui.schematics.buttons.button(bundle("convert"), converterDialog::show);
		Vars.ui.menufrag.addButton(bundle("convert"), Icon.fileImage, converterDialog::show);

		Events.on(EventType.ClientLoadEvent.class, clientLoadEvent -> {
			Prefs.debugMode = Core.settings.getBool(setting("debug-mode"));
			if (Prefs.debugMode) converterDialog.show();
		});
	}

	public static String bundle(String name) {
		return "@"+internalName+"." + name;
	}
}

class IntegerSetting extends SettingsMenuDialog.SettingsTable.Setting {

	public IntegerSetting(String name) {
		super(name);
	}

	public IntegerSetting(String name, int def) {
		this(name);
		Core.settings.defaults(name, def);
	}

	@Override
	public void add(SettingsMenuDialog.SettingsTable table) {
		TextField field = new TextField(Integer.toString(Core.settings.getInt(this.name)));
		field.setFilter(TextField.TextFieldFilter.digitsOnly);
		field.changed(() -> Core.settings.put(this.name, Integer.parseInt(field.getText())));
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

class FakeSetting extends SettingsMenuDialog.SettingsTable.Setting {
	Cons<Table> cons;

	public FakeSetting(Cons<Table> cons) {
		super("");
		this.cons = cons;
	}

	@Override
	public void add(SettingsMenuDialog.SettingsTable settingsTable) {
		cons.get(settingsTable);
	}
}