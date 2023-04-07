package me.mars.triangles.ui;

import arc.graphics.Color;
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
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

public class ConverterWrapper extends Table {
	CheckBox collasped;
	private boolean nameSet = false;
	ConverterDialog dialog;
	Converter conv;

	public ConverterWrapper(ConverterDialog dialog, Converter conv) {
		this.dialog = dialog;
		this.conv = conv;
		this.background(Tex.pane).top();
		TextField nameField = new TextField();
		nameField.update(() -> {
			boolean complete = this.conv.complete();
			nameField.setDisabled(!complete);
			if (!complete) {
				nameField.setText(formatTime(this.conv.estTime()));
			} else if (!this.nameSet) {
				this.nameSet = true;
				nameField.setText(this.conv.name);
			}
		});
		nameField.changed(() -> {
			if (!nameField.isValid()) return;
			this.conv.name = nameField.getText();
		});
		this.add(nameField);
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
		this.add(this.collasped = new CheckBox(null, new CheckBox.CheckBoxStyle(){{
			checkboxOn = Icon.downOpen;
			checkboxOff = Icon.upOpen;
			checkboxOnOver = checkboxOn;
			checkboxOver = checkboxOff;
			checkboxOnDisabled = checkboxOn;
			checkboxOffDisabled = checkboxOff;
			font = Fonts.def;
			fontColor = Color.white;
			disabledFontColor = Color.gray;
		}})).margin(1f);
		this.row();
		this.collapser(t -> {
			t.setBackground(Styles.grayPanel);
			t.row();
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
				t.label(() -> formatTime(gen.timeToCompletion()) + " " + Strings.fixed(gen.acc()*100f, 3) + "%");
				t.row();
			}
		}, true, () -> this.collasped.isChecked());
	}

	static String formatTime(float time) {
		if (time == -1) return "";
		int seconds = (int) (time % 60);
		return (int)(time/60) + ":" + (seconds < 10 ? "0"+seconds : seconds);
	}

}
