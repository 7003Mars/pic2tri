package me.mars.triangles.mod.ui

import arc.Core
import arc.input.KeyCode
import arc.scene.event.InputEvent
import arc.scene.event.InputListener
import kotlin.math.abs

abstract class DragListener(var tapKey: KeyCode?) : InputListener() {
    private var lastX = 0f
    private var lastY = 0f
    private var startX = 0f
    private var startY = 0f

    var tapSquare: Float = 14f
    var pressed: Boolean = false

    override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: KeyCode?): Boolean {
        if (Core.app.isMobile && pointer != 0) return false
        this.pressed = button == this.tapKey
        if (this.pressed) {
            this.startX = x
            this.startY = y
        }
        //		Log.info("Touchdown: @, @", x,y);
        this.lastX = x
        this.lastY = y
        return true
    }

    override fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int) {
        if ((Core.app.isMobile && pointer != 0)) return
        this.pressed = this.pressed and this.inSquare(x, y)
        this.dragged(x - this.lastX, y - this.lastY)
        this.lastX = x
        this.lastY = y
    }

    override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: KeyCode?) {
        if (this.pressed && button == this.tapKey && this.inSquare(x, y)) this.clicked(x, y)
    }

    private fun inSquare(x: Float, y: Float): Boolean {
        return abs(this.startX - x) <= this.tapSquare && abs(this.startY - y) <= this.tapSquare
    }

    abstract fun dragged(xDelta: Float, yDelta: Float)

    abstract fun clicked(x: Float, y: Float)
}
