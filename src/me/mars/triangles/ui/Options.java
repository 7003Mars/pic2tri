package me.mars.triangles.ui;

import arc.scene.Element;
import arc.scene.event.ChangeListener;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import me.mars.triangles.SchemBuilder;

public class Options extends Table {
	ConverterDialog dialog;

	private boolean stopEvent = false;

	Slider acc = new Slider(0, 0.99f, 0.001f, false);
	Slider procs = new Slider(0, 0, 1f, false);

	public Options(ConverterDialog dialog) {
		this.dialog = dialog;
		this.addCaptureListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Element actor) {
				if (stopEvent && (actor == Options.this.acc || actor == Options.this.procs)) {
					event.stop();
				}
			}
		});
//		this.top().left();
		this.label(() -> "Accuracy:" + Strings.fixed(this.acc.getValue()*100, 1) + "%");
		this.row();
		this.acc.changed(() -> {
			if (dialog.selectedOpt == null) return;
			dialog.selectedOpt.targetAcc = acc.getValue();
			dialog.updateSize();
		});
		this.add(acc);
		this.row();
		this.label(() -> "Max shapes: " + (int)this.procs.getValue() * SchemBuilder.Max_Shapes);
		this.row();
		this.procs.changed(() -> {
			if (dialog.selectedOpt == null) return;
			this.stopEvent = true;
			SchemBuilder.Display selected = (SchemBuilder.Display) dialog.selectedOpt;
			this.procs.setValue(selected.getProcs((int) this.procs.getValue()));
			this.stopEvent = false;
			dialog.updateSize();
		});
//		this.procs.keyDown(keyCode -> {
//			int curVal = (int) this.procs.getValue();
//			if (keyCode == KeyCode.left) {
//				this.procs.setValue(curVal-1);
//			} else if (keyCode == KeyCode.right) {
//				this.procs.setValue(curVal+1);
//			}
//		});
		this.add(procs);
	}

	public void updateFields() {
		// TODO: Get free space from Fill
		this.stopEvent = true;
		if (dialog.selectedOpt == null) {
			this.procs.setRange(0f, 0f);
		} else {
			SchemBuilder.Display selected = (SchemBuilder.Display) dialog.selectedOpt;
			this.procs.setRange(0, Math.min(selected.maxPoints(), 10000/SchemBuilder.Max_Shapes));
			this.procs.setValue(selected.points.size);
			this.acc.setValue(this.dialog.selectedOpt.targetAcc);
		}
		this.stopEvent = false;
	}
}
