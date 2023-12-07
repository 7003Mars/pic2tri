package me.mars.triangles.ui;

import arc.scene.ui.Button;
import arc.scene.ui.CheckBox;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import me.mars.triangles.Converter;
import me.mars.triangles.Generator;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.ui.Styles;

public class ConverterWrapper extends Table {
	CheckBox collapsed;
	ConverterDialog dialog;
	Converter conv;

	public ConverterWrapper(ConverterDialog dialog, Converter conv) {
		this.dialog = dialog;
		this.conv = conv;
		this.background(Tex.pane).top();
		Bar progressBar = new Bar(() -> (int)(this.conv.totalProgress()*100)+"%",
				() -> Pal.gray, () -> this.conv.totalProgress());
		progressBar.visible(() -> !this.conv.complete());
		TextField nameField = new TextField(this.conv.name);
		nameField.changed(() -> {
			if (!nameField.isValid()) return;
			this.conv.name = nameField.getText();
		});
		nameField.visible(() -> this.conv.complete());
		this.stack(progressBar, nameField);
		Button exportButton = new Button(Icon.export);
		exportButton.setDisabled(() -> !this.conv.complete());
		exportButton.clicked(() -> {
			this.conv.build();
			dialog.converters.remove(conv);
			dialog.convertersUpdated();
		});
		this.add(exportButton).margin(1f);
		Button cancelButton = new Button(Icon.cancel);
		cancelButton.clicked(() -> Vars.ui.showConfirm("Stop task?", () -> {
			this.conv.cancel();
			dialog.converters.remove(conv);
			dialog.convertersUpdated();
		}));
		cancelButton.setDisabled(() -> this.conv.complete());
		this.add(cancelButton).margin(1f);
		this.add(this.collapsed = new CheckBox(null, Styles2.collapseStyle)).margin(1f);
		this.row();
		this.collapser(t -> {
			t.setBackground(Styles.grayPanel);
			t.row().defaults().left();
			for (Generator gen : conv.generators) {
				t.image(() -> {
					switch (gen.getState()) {
						case Done -> {
							return Icon.ok.getRegion();
						}
						case Ready -> {
							return Icon.pause.getRegion();
						}
						case Start -> {
							return Icon.settings.getRegion();
						}
					}
					return null;
				});
				// TODO: Show time only if running
				t.label(() -> gen.getState() == Generator.GenState.Start ? formatTime(gen.timeToCompletion()) : "").growX();
				t.label(() -> Strings.fixed(gen.acc()*100f, 3) + "%");
				t.row();
			}
		}, true, () -> this.collapsed.isChecked()).colspan(4).growX();
	}

	static String formatTime(float time) {
		if (time == -1) return "";
		int seconds = (int) (time % 60);
		return (int)(time/60) + ":" + (seconds < 10 ? "0"+seconds : seconds);
	}

}
