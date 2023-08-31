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
import me.mars.triangles.shapes.Triangle
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.RepetitionInfo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class RasteriserTest {
    @RepeatedTest(10)
    fun death(info: RepetitionInfo) {
        val rand: Rand = Rand()
        val tri: Triangle = Triangle()
        val w = 250
        val h = 250
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
        val config: SdlConfig = object : SdlConfig() {
            init {
                disableAudio = true
                initialVisible = false
                initialBackgroundColor = Color.black
                width = w
                height = h
                title = "a-${info.currentRepetition}"

            }
        }
        val gl: Pixmap = sdlTest(config) {
            // Set the background
//            Draw.color(Color.yellow)
//            Fill.crect(0f, 0f, w.toFloat(), h.toFloat())
//            Draw.flush()
            // Fill triangle
            Draw.color(Color.white)
            Fill.tri(tri.x1.toFloat(), tri.y1.toFloat(), tri.x2.toFloat(), tri.y2.toFloat(), tri.x3.toFloat(), tri.y3.toFloat())
            Draw.flush()
//            Draw.proj(0f, 0f, w.toFloat(), h.toFloat())
            return@sdlTest ScreenUtils.getFrameBufferPixmap(0, 0, w, h, true)
        }
        Thread.sleep(25)
        if (gl.any { pixel, _, _ -> pixel != Color.blackRgba && pixel != Color.whiteRgba }) {
            Log.info("trash created ${info.currentRepetition}/${info.totalRepetitions}")
            Fi("gl.png").writePng(gl)
            throw AssertionError()
        }
    }

//    @Test
//    fun crap() {
//        SdlApplication(object : ApplicationListener {
//            var i = 0
//            override fun init() {
//                Core.atlas = TextureAtlas.blankAtlas()
//                Core.batch = SortedSpriteBatch()
//            }
//
//            override fun update() {
////                if (i == 0) Core.graphics.clear(Color.black)
//                Draw.color(Color.pink)
////                Draw.proj(0f, 0f, 50f, 50f)
//                Fill.tri(0f, 0f, 25f, 25f, 50f, 50f)
//                Draw.flush()
//                if (i++ > 50) Core.app.exit()
//                Threads.sleep(100)
//            }
//        }, object : SdlConfig(){
//            init {
//                width = 50
//                height = 50
//                disableAudio = true
//                decorated = false
//            }
//        })
//    }

    @RepeatedTest(10)
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
        val res: Pixmap = compareTriangles(w, h, tri)
        val failed = res.any { pixel, _, _ -> pixel != Color.blackRgba && pixel != Color.whiteRgba }
        if (failed) Fi("output-${info.currentRepetition}.png").writePng(res)
        assertFalse(failed)

    }

    fun compareTriangles(w: Int, h: Int, tri: Triangle): Pixmap {
        Fi("glfail.png").delete()
        // Init triangles
        // Init sdl app
        val config: SdlConfig = object : SdlConfig() {
            init {
                disableAudio = true
//                gl30 = true
                width = w
                height = h
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
        for (x: Int in 0 until w){
            for (y: Int in 0 until h) {
                val p1: Int = gl.get(x, y)
                val p2: Int = mutate.get(x,y)
                if (p1 == Color.blackRgba && p2 == Color.whiteRgba) {
                    // Rasterizer has extra pixel
                    gl.set(x, y, Color.yellow)
                } else if (p1 == Color.whiteRgba && p2 == Color.blackRgba) {
                    // Rasterizer lacks a pixel
                    gl.set(x, y, Color.red)
                }
            }
        }
        return gl
    }

    fun <T> sdlTest(config: SdlConfig, prov: Prov<T>): T {
        var res: T? = null
        val logLevel = Log.level
//        Log.level = Log.LogLevel.none
        SdlApplication(object : ApplicationListener {
//            var count = 0

            override fun init() {
                Log.level = logLevel
                Core.batch = SortedSpriteBatch()
                Core.atlas = TextureAtlas.blankAtlas()
            }

            override fun update() {
//                if (count == 0) {
                Core.graphics.clear(Color.black)
//                Draw.proj(0f, 0f, Core.graphics.width.toFloat(), Core.graphics.height.toFloat())
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
}

fun Pixmap.any(cons: (pixel: Int, x: Int, y: Int) -> Boolean): Boolean {
    for (x in 0 until this.width) {
        for (y in 0 until this.height) {
            if (cons(this.get(x, y), x, y)) {
                Log.warn("Color is ${this.get(x, y)} at $x, $y")
                return true
            }
        }
    }
    return false
}