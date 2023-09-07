package main

import arc.ApplicationListener
import arc.Core
import arc.Graphics
import arc.backend.sdl.SdlApplication
import arc.backend.sdl.SdlConfig
import arc.files.Fi
import arc.func.Prov
import arc.graphics.Color
import arc.graphics.Gl
import arc.graphics.Pixmap
import arc.graphics.Pixmaps
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.g2d.SortedSpriteBatch
import arc.graphics.g2d.TextureAtlas
import arc.graphics.gl.FrameBuffer
import arc.math.Rand
import arc.util.Log
import arc.util.ScreenUtils
import arc.util.Threads
import me.mars.triangles.MutateMap
import me.mars.triangles.shapes.NewTriangle
import me.mars.triangles.shapes.Triangle
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.RepetitionInfo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class RasteriserTest {

    @Test
    fun death() {
        val rand: Rand = Rand()
        val tri: Triangle = Triangle()
        val w = 250
        val h = 250
        val bx = w-1
        val by = h-1
        val pixmap = MutateMap(Pixmap(w, h))
        val times =  10000
        val updateAt = times/100
        repeat(times) {
            if ((it % updateAt) == 0) println("Run: $it")
            with(rand) {
                do {
                    tri.x1 = random(bx)
                    tri.y1 = random(by)
                    tri.x2 = random(bx)
                    tri.y2 = random(by)
                    tri.x3 = random(bx)
                    tri.y3 = random(by)
                } while (tri.invalid())
            }
            tri.fill(pixmap)
            pixmap.drop()
            Thread.sleep(25)
        }

    }

    @RepeatedTest(100)
    fun `just a test`(info: RepetitionInfo) {
        val rand: Rand = Rand()
        val tri: Triangle = Triangle()
        val w = 50
        val h = 50
        val bx = w-1
        val by = h-1
        with(rand) {
            do {
                tri.x1 = random(bx)
                tri.y1 = random(by)
                tri.x2 = random(bx)
                tri.y2 = random(by)
                tri.x3 = random(bx)
                tri.y3 = random(by)
            } while (tri.invalid())
        }
//        with(tri) {
//            x1 = 0
//            x2 = 25
//            x3 = 50
//            y1 = 25
//            y2 = 0
//            y3 = 25
//        }
        val res: Pixmap? = compareTriangles(w, h, tri)
        if (res != null) {
            Fi("out/output-${info.currentRepetition}.png").writePng(res)
            throw AssertionError(tri.toString())
        }
    }

    @RepeatedTest(100)
    fun `Flat test`(info: RepetitionInfo) {
        val rand: Rand = Rand()
        val tri: Triangle = Triangle()
        val w = 50
        val h = 50
        val bx = w-1
        val by = h-1
        with(rand) {
            do {
                tri.x1 = random(bx)
                tri.y1 = random(by)
                tri.x2 = random(bx)
                tri.y2 = random(by)
                tri.x3 = random(bx)
                tri.y3 = tri.y1
            } while (tri.invalid())
        }
        val res: Pixmap? = compareTriangles(w, h, tri)
        if (res != null) {
            Fi("out/flat-${info.currentRepetition}.png").writePng(res)
            throw AssertionError(tri.toString())
        }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun clearOutput(): Unit {
            Fi("out").deleteDirectory()
            MutateMap.testing = true
        }
    }
}

fun compareTriangles(w: Int, h: Int, tri: Triangle): Pixmap? {
    // Init sdl app
    val config: SdlConfig = object : SdlConfig() {
        init {
            disableAudio = true
            width = w
            height = h
            decorated = false
        }
    }
    // Obtain opengl triangle
    val gl: Pixmap = sdlTest(config) {
        // Fill triangle
        Draw.color(Color.white)
//            Log.info(tri)
        Fill.tri(tri.x1.toFloat(), tri.y1.toFloat(), tri.x2.toFloat(), tri.y2.toFloat(), tri.x3.toFloat(), tri.y3.toFloat())
        Draw.flush()
        return@sdlTest ScreenUtils.getFrameBufferPixmap(0, 0, w, h, true)
    }
    if (gl.any { pixel, _, _ -> pixel != Color.blackRgba && pixel != Color.whiteRgba }) Fi("glfail.png").writePng(gl)
    // Obtain pixmap triangle
    val mutate: MutateMap = MutateMap(Pixmap(w, h))
    mutate.fill(Color.black)
    tri.fill(mutate)
    mutate.apply(Color.white.rgba())
    Pixmaps.flip(mutate)
    // Compare them
    var failed = false
    for (x: Int in 0 until w){
        for (y: Int in 0 until h) {
            val p1: Int = gl.get(x, y)
            val p2: Int = mutate.get(x,y)
            if (p1 == Color.blackRgba && p2 == Color.whiteRgba) {
                // Rasterizer has extra pixel
                gl.set(x, y, Color.yellow)
                failed = true
            } else if (p1 == Color.whiteRgba && p2 == Color.blackRgba) {
                // Rasterizer lacks a pixel
                gl.set(x, y, Color.red)
                failed = true
            }
        }
    }
    return if (failed) gl else null
}

fun <T> sdlTest(config: SdlConfig, prov: Prov<T>): T {
    var res: T? = null
    val logLevel = Log.level
    Log.level = Log.LogLevel.none
    SdlApplication(object : ApplicationListener {

        override fun init() {
            Log.level = logLevel
            Core.batch = SortedSpriteBatch()
            Core.atlas = TextureAtlas.blankAtlas()
        }

        override fun update() {
            Core.graphics.clear(Color.black)
            res = prov.get()
            // Dispose everything
            Core.atlas?.dispose() ?: Log.warn("Atlas is null")
            Core.graphics?.dispose() ?: Log.warn("Graphics is null")
            Core.batch?.dispose() ?: Log.warn("Batch is null")
            Core.assets?.dispose() ?: Log.warn("Assets is null")
            Core.audio?.dispose() ?: Log.warn("Audio is null")
            Core.app.exit()
        }
    }, config)
    return res!!
}

fun Pixmap.any(cons: (pixel: Int, x: Int, y: Int) -> Boolean): Boolean {
    var res = false
    for (x in 0 until this.width) {
        for (y in 0 until this.height) {
            if (cons(this.get(x, y), x, y)) {
                Log.warn("Color is ${this.get(x, y)} at $x, $y")
                res = true
            }
        }
    }
    return res
}