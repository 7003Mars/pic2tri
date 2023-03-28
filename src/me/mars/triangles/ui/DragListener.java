package me.mars.triangles.ui;

import arc.Core;
import arc.input.KeyCode;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;

public abstract class DragListener extends InputListener {
	public KeyCode tapKey;

	private float lastX, lastY;
	private float startX, startY;

	public float tapSquare = 14;
	public boolean pressed = false;

	public DragListener(KeyCode tapKey) {
		this.tapKey = tapKey;
	}

	@Override
	public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
		if (Core.app.isMobile() && pointer != 0) return false;
		this.pressed = button == this.tapKey;
		if (this.pressed) {
			this.startX = x;
			this.startY = y;
		}
//		Log.info("Touchdown: @, @", x,y);
		this.lastX = x;
		this.lastY = y;
		return true;
	}

	@Override
	public void touchDragged(InputEvent event, float x, float y, int pointer) {
		if ((Core.app.isMobile() && pointer != 0)) return;
		this.pressed &= this.inSquare(x, y);
		this.dragged(x - this.lastX, y - this.lastY);
		this.lastX = x;
		this.lastY = y;
	}

	@Override
	public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
		if (this.pressed && button == this.tapKey && this.inSquare(x, y)) this.clicked(x, y);
	}

	private boolean inSquare(float x, float y) {
		return Math.abs(this.startX - x) <= this.tapSquare && Math.abs(this.startY - y) <= this.tapSquare;
	}

	public abstract void dragged(float xDelta, float yDelta);

	public abstract void clicked(float x, float y);
}
