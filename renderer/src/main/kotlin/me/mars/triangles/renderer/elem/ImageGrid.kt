package me.mars.triangles.renderer.elem

import arc.Core
import arc.graphics.Color
import arc.graphics.Pixmap
import arc.graphics.Texture
import arc.graphics.g2d.*
import arc.input.KeyCode
import arc.math.Mathf
import arc.math.geom.Point2
import arc.scene.Element
import arc.scene.event.ElementGestureListener
import arc.scene.event.InputEvent
import arc.scene.event.InputListener
import arc.scene.ui.layout.Scl
import arc.util.Nullable
import arc.util.Strings
import arc.util.pooling.Pools
import me.mars.triangles.layout.Layout
import me.mars.triangles.layout.LogicDisplayLayout
import me.mars.triangles.utils.Prefs
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.ui.Fonts
import mindustry.world.blocks.logic.LogicDisplay
import kotlin.math.abs

class ImageGrid : Element() {
    var layout: Layout<*> = LogicDisplayLayout(Blocks.logicDisplay as LogicDisplay, 1, 1)
    var onSelect: (Point2) -> Unit = { }

    private var region: TextureRegion = TextureRegion(Texture(0, 0))
    var panX: Float = 0f
    var panY: Float = 0f
    var zoom: Float = 1f

    init {
        this.addListener(object : DragListener(KeyCode.mouseLeft) {
            override fun dragged(xDelta: Float, yDelta: Float) {
                var xDelta = xDelta
                var yDelta = yDelta
                val scaledSize: Float = scaled(this@ImageGrid.zoom)
                xDelta /= scaledSize / 2f
                yDelta /= scaledSize / 2f
                this@ImageGrid.panX += xDelta
                this@ImageGrid.panY += yDelta
                this@ImageGrid.clampPos()
            }

            override fun clicked(x: Float, y: Float) {
                /**
                 *             mouse.sub(this.width / 2f, this.height / 2f)
                 *             if (PicToTri.debugMode) builder.append(" Raw: ").append(mouse.x.toInt()).append(",").append(mouse.y.toInt())
                 *             mouse.sub(this.panX * scaledSize / 2f, this.panY * scaledSize / 2f)
                 *             // TODO: Figure out why this is already scaled down to block size
                 *             mouse.scl(1f / (scaledSize))
                 */
                var x = x
                var y = y
                val scl: Float = scaled(this@ImageGrid.zoom)
                x -= this@ImageGrid.width / 2f
                y -= this@ImageGrid.height / 2f
                x -= this@ImageGrid.panX * scl / 2f
                y -= this@ImageGrid.panY * scl / 2f
                val sx = (x/scl).toInt()
                val sy = (y/scl).toInt()
                if (sx < 0 || sx >= layout.width || sy < 0 || sy >= layout.height) return
                onSelect(Point2(sx, sy))
            }

            override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Element?) {
                this@ImageGrid.requestScroll()
                this@ImageGrid.requestKeyboard()
            }

            override fun scrolled(event: InputEvent?, x: Float, y: Float, amountX: Float, amountY: Float): Boolean {
                this@ImageGrid.zoom = Mathf.clamp(this@ImageGrid.zoom - amountY / 10f, 0.1f, 15f)
                this@ImageGrid.clampPos()
                return true
            }

            override fun keyTyped(event: InputEvent?, character: Char): Boolean {
                if (character == 'r') {
                    this@ImageGrid.panX = 0f
                    this@ImageGrid.panY = 0f
                    return true
                }
                return false
            }
        })
        if (Vars.mobile) {
            this.addListener(object : ElementGestureListener() {
                var lastZoom: Float = 0f
                override fun zoom(event: InputEvent?, initialDistance: Float, distance: Float) {
                    this@ImageGrid.zoom = Mathf.clamp(distance / initialDistance * this.lastZoom, 0.1f, 15f)
                    this@ImageGrid.clampPos()
                }

                override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: KeyCode?) {
                    this.lastZoom = this@ImageGrid.zoom
                }
            })
        }
    }

    fun setDrawable(pixmap: Pixmap?) {
        this.region.texture.dispose()
        this.region = TextureRegion(Texture(pixmap))
    }

    override fun draw() {
        super.draw()
        Lines.rect(this.x, this.y, this.width, this.height)
        // Pre-drawing stuff
        if (!this.clipBegin()) return
        val scaledSize: Float = scaled(this.zoom)
        // TODO This should be layout origin position, either rename better or leave comment
        val ox = this.x + this.width / 2f + this.panX * scaledSize / 2f
        val oy = this.y + this.height / 2f + this.panY * scaledSize / 2f
        // Border for whole schematic
        Lines.rect(ox, oy, scaledSize * layout.width, scaledSize * layout.height)
        if (Prefs.debugMode) {
            Fill.rect(this.panX + this.width / 2f, this.panY + this.width / 2f, scaledSize, scaledSize)
//            Draw.color(Pal.darkerMetal)
//            for (x in 0..<dialog.filler.width) {
//                for (y in 0..<dialog.filler.height) {
//                    if (dialog.filler.occupied(x, y)) Fill.crect(
//                        ox + x * scaledSize,
//                        oy + y * scaledSize,
//                        scaledSize,
//                        scaledSize
//                    )
//                }
//            }
//            Draw.color()
        }
        Draw.color()
        // Draw the image
        val ix = layout.imageBounds.x * scaledSize
        val iy = layout.imageBounds.y * scaledSize
        val iw = layout.imageBounds.width * scaledSize
        val ih = layout.imageBounds.height * scaledSize
        // TODO + or - iw/2?
        Draw.rect(this.region, ox + ix + iw/2f, oy + iy + ih/2f, iw, ih)
        // Draw chunks
        Lines.stroke(1f)
        for (chunk in layout.chunks) {
            val chunkWidth = chunk.width * scaledSize
            val chunkHeight = chunk.height * scaledSize
            Lines.rect(ox + chunk.chunkX*scaledSize, oy + chunk.chunkY*scaledSize, chunkWidth, chunkHeight)
        }
        // Draw schematic blocks
        for (tile in layout.preview.tiles) {
            val region = tile.block.fullIcon
            val tileSize = tile.block.size * scaledSize
            Draw.rect(region, ox + tile.x*tileSize + tileSize/2f, oy + tile.y*tileSize + tileSize/2f, tileSize, tileSize)
        }
        this.clipEnd()
        // Draw coords
        val builder = StringBuilder()
        builder.append(this.panX.toInt()).append(",").append(this.panY.toInt())
        builder.append(" : ").append(Strings.fixed(this.zoom, 1))
        if (this.hasMouse()) {
            val mouse = this.screenToLocalCoordinates(Core.input.mouse())
            mouse.sub(this.width / 2f, this.height / 2f)
            if (Prefs.debugMode) builder.append(" Raw: ").append(mouse.x.toInt()).append(",").append(mouse.y.toInt())
            mouse.sub(this.panX * scaledSize / 2f, this.panY * scaledSize / 2f)
            // TODO: Figure out why this is already scaled down to block size
            mouse.scl(1f / (scaledSize))
            builder.append(" @ ").append(mouse.x.toInt()).append(",").append(mouse.y.toInt())
        }
        val font = Fonts.outline
        font.color = Color.white
        font.getData().setScale(Scl.scl(1f))
        val glyphLayout = Pools.obtain(GlyphLayout::class.java) { GlyphLayout() }
        glyphLayout.setText(font, builder.toString())
        Draw.color(0f, 0f, 0f, 0.5f)
        Fill.rect(this.x + glyphLayout.width / 2f, this.y + glyphLayout.height / 2f, glyphLayout.width, glyphLayout.height)
        Draw.color()
        font.draw(glyphLayout, this.x, this.y + glyphLayout.height)
        Pools.free(glyphLayout)
    }

    fun clampPos() {
        val scaledSize: Float = scaled(this.zoom)
        val xbounds = (this.width) / (scaledSize)
        val ybounds = (this.height) / (scaledSize)
        val w = layout.width
        val h = layout.height
        this.panX = Mathf.clamp(this.panX, -xbounds - w, xbounds - w)
        this.panY = Mathf.clamp(this.panY, -ybounds - h, ybounds - h)
    }
}

private fun scaled(scl: Float): Float {
    // Pixels per world unit at this zoom
    return scl * Vars.tilesize * 0.5f
}

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
