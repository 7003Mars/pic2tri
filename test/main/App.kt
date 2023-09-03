package main

import arc.ApplicationListener
import arc.Core
import arc.backend.sdl.SdlApplication
import arc.backend.sdl.SdlConfig
import arc.files.Fi
import arc.graphics.Color
import arc.graphics.Pixmap
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.g2d.SpriteBatch
import arc.graphics.g2d.TextureAtlas
import arc.util.Log
import arc.util.ScreenUtils
import arc.util.Threads
import me.mars.triangles.MutateMap
import me.mars.triangles.shapes.NewTriangle
import me.mars.triangles.shapes.Triangle

fun main() {
//    repeat(3) {
//        draw(it)
//    }
    val w = 50
    val h = 50
    val tri = Triangle()

    // y 24
    with(tri) {r=0; g=0; b=0; x1=35; y1=34; x2=7; y2=37; x3=2; y3=34}
    val targetTri = NewTriangle().also { it.set(tri) }
    val pixmap = MutateMap(Pixmap(w, h))
    targetTri.fill(pixmap)
    Log.info("-----")
    compareTriangles(w, h, tri).also {
        if (it == null) {
            Log.info("Yay!")
            return
        }
        Log.info("ohno")
        Fi("out/single.png").writePng(it)
    }

//    val config: SdlConfig = object : SdlConfig() {
//        init {
//            disableAudio = true
//            initialVisible = false
//            initialBackgroundColor = Color.black
//            width = w
//            height = h
//        }
//    }
//    sdlTest(config) {
//
//    }
}


fun draw(i: Int) {
    val w = 250
    val h = 250
    val config: SdlConfig = SdlConfig().also {
        it.width = w
        it.height = h
        it.disableAudio = true
        it.title = "app-${i}"
    }

    val app = SdlApplication(object : ApplicationListener {
        override fun init() {
            Core.batch = SpriteBatch()
            Core.atlas = TextureAtlas.blankAtlas()
            Draw.proj(0f, 0f, w.toFloat(), h.toFloat())
        }

        override fun update() {
            Core.graphics.clear(Color.black)
            Draw.color(Color.pink)
//            Draw.proj(0f, 0f, 50f, 50f)
            println("${Core.graphics.width}, ${Core.graphics.height}")
            if (i==0 || true) {
                Fill.tri(0f, 0f, 25f, 25f, 50f, 0f)
            } else {
//                Draw.color(Color.pink)
                Fill.tri(-25f, -25f, 0f, 0f, 25f, -25f)
            }
            Draw.flush()
            ScreenUtils.saveScreenshot(Fi("out/rend-${i}.png"), 0, 0, w, h)
            Threads.sleep(500)
            // try disposing
            Core.graphics.dispose()
            Core.atlas.dispose()
            Core.batch.dispose()
            Core.app.exit()
        }
    }, config)
}

//val app = SdlApplication(object : ApplicationListener {
//
//},)