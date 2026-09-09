package me.mars.triangles.mod.ui;

import arc.graphics.Color;
import arc.scene.ui.CheckBox.CheckBoxStyle;
import mindustry.gen.Icon;
import mindustry.ui.Fonts;

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
