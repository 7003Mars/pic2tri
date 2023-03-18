package me.mars.triangles.ui;

import arc.func.Cons;
import arc.scene.event.ChangeListener;
import arc.scene.ui.Slider;
import arc.struct.Seq;

public class ValuesSlider<T> extends Slider {
	private boolean suppress = true;
	Seq<T> values = new Seq<>();
	public ValuesSlider(Seq<T> values) {
		super(0, values.size-1, 1f, false);
		this.addCaptureListener(sceneEvent -> {
			if (sceneEvent instanceof ChangeListener.ChangeEvent && suppress) {
				sceneEvent.stop();
				return true;
			}
			return false;
		});
	}

	public void setValues(Seq<T> values) {
		if (values.size < 1) {
			throw new IllegalArgumentException("Array must contain at least 1 value");
		}
		this.values = values;
		this.suppress = true;
		this.setRange(0, values.size-1);
		this.setValue(0);
		// Force the event to fire
		this.suppress = false;
		this.change();
	}

	public void selected(Cons<T> listener) {
		this.changed(() -> listener.get(this.getSelected()));
	}

	public T getSelected() {
		return this.values.get((int) this.getValue());
	}
}
