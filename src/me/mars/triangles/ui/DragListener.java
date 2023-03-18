package me.mars.triangles.ui;

import arc.Core;
import arc.input.KeyCode;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;

public abstract class DragListener extends InputListener {
	public KeyCode touchKey;

	private float lastX, lastY;

	public DragListener(KeyCode key) {
		this.touchKey = key;
	}

	@Override
	public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
		if ((Core.app.isMobile() && pointer != 0) || button != this.touchKey) return false;
//		Log.info("Touchdown: @, @", x,y);
		this.lastX = x;
		this.lastY = y;
		return true;
	}

	@Override
	public void touchDragged(InputEvent event, float x, float y, int pointer) {
		if ((Core.app.isMobile() && pointer != 0)) return;
//		Log.info("Drag: @, @, last: @, @", x, y, this.lastX, this.lastY);
		this.dragged(x - this.lastX, y - this.lastY);
		this.lastX = x;
		this.lastY = y;
	}

	@Override
	public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {

	}

	public abstract void dragged(float xDelta, float yDelta);
}
