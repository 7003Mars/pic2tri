package me.mars.triangles.ui;

import arc.graphics.Color;
import arc.scene.ui.CheckBox.CheckBoxStyle;
import arc.scene.ui.ProgressBar.ProgressBarStyle;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

public class Styles2 {
	public static CheckBoxStyle collapseStyle = new CheckBoxStyle(){{
		checkboxOn = Icon.downOpen;
		checkboxOff = Icon.upOpen;
		checkboxOnOver = checkboxOn;
		checkboxOver = checkboxOff;
		checkboxOnDisabled = checkboxOn;
		checkboxOffDisabled = checkboxOff;
		font = Fonts.def;
		fontColor = Color.white;
		disabledFontColor = Color.gray;
	}};
}
