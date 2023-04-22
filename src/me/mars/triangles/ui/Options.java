package me.mars.triangles.ui;

import arc.scene.Element;
import arc.scene.event.ChangeListener;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import me.mars.triangles.PicToTri;
import me.mars.triangles.SchemBuilder;

public class Options extends Table {
	ConverterDialog dialog;

	private boolean suppress = false;

	Slider acc = new Slider(0, 0.99f, 0.001f, false);
	Slider procs = new Slider(0, 0, 1f, false);

	private static final int Max_Procs = Math.min(SchemBuilder.fitProcs(10000), 29);

	public Options(ConverterDialog dialog) {
		this.dialog = dialog;
		this.addCaptureListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Element actor) {
				if (suppress && (actor == Options.this.acc || actor == Options.this.procs)) {
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
		});
		this.add(acc);
		this.row();
		this.label(() -> dialog.selectedOpt == null ? PicToTri.bundle("select-display") :
				"Max shapes: " + dialog.selectedOpt.maxGen);
		this.row();
		this.procs.changed(() -> {
			if (dialog.selectedOpt == null) return;
			this.suppress = true;
			SchemBuilder.Display selected = (SchemBuilder.Display) dialog.selectedOpt;
			this.procs.setValue(selected.getProcs((int) this.procs.getValue()));
			this.suppress = false;
		});
		this.add(procs);
	}

	public void updateFields() {
		this.suppress = true;
		if (dialog.selectedOpt == null) {
			this.procs.setRange(0f, 0f);
		} else {
			SchemBuilder.Display selected = (SchemBuilder.Display) dialog.selectedOpt;
			this.procs.setRange(0, Math.min(selected.maxPoints(), Max_Procs));
			this.procs.setValue(selected.points.size);
			this.acc.setValue(this.dialog.selectedOpt.targetAcc);
		}
		this.suppress = false;
	}
}
