package main

import arc.ApplicationListener
import arc.Core
import arc.backend.sdl.SdlApplication
import arc.backend.sdl.SdlConfig
import arc.files.Fi
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.g2d.SortedSpriteBatch
import arc.graphics.g2d.SpriteBatch
import arc.graphics.g2d.TextureAtlas
import arc.util.ScreenUtils
import arc.util.Threads

fun main() {
    repeat(3) {
        draw(it)
    }
}

val w = 250
val h = 250

fun draw(i: Int) {
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