package me.mars.triangles.ui;

import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.blocks.logic.LogicDisplay;
import mindustry.world.blocks.logic.TileableLogicDisplay;

import static me.mars.triangles.PicToTri.bundle;

public class Configs extends Table {
//	ConverterDialog dialog;

	ValuesSlider<Float> scaleSlider = new ValuesSlider<>(Seq.with(1f));
	TextField nameField = new TextField();
	TextField seedField = new TextField();

	public Configs(ConverterDialog dialog) {
		// TODO Temporarily disable tileable displays as mod does not support them yet
		Seq<LogicDisplay> displays = Vars.content.blocks().select(block -> block instanceof LogicDisplay && !(block instanceof TileableLogicDisplay)).<LogicDisplay>as();
//		this.dialog = dialog;

		this.nameField.setMessageText(bundle("name-field"));
		this.add(this.nameField);
		this.row();
		this.row();
		this.seedField.setMessageText(bundle("seed-field"));
		this.seedField.setFilter(TextField.TextFieldFilter.digitsOnly);
		this.add(this.seedField);
		this.row();
		this.add(bundle("image-scale"));
		this.row();
		this.add(scaleSlider);
		this.scaleSlider.selected(value -> {
			dialog.scl = value;
			dialog.updateSize();
		});
		this.row();
		Table blockSelector = new Table();
		ItemSelection.buildTable(blockSelector, displays, () -> dialog.lDisplay, (logicDisplay -> {
			if (logicDisplay == null) return;
			dialog.lDisplay = logicDisplay;
			dialog.updateScales();
		}));
		this.add(blockSelector).left();
	}
}
