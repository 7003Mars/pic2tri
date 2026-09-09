package me.mars.triangles.mod.ui.model

import arc.files.Fi
import arc.graphics.Pixmap
import me.mars.triangles.converter.Converter
import me.mars.triangles.converter.GifConverter
import me.mars.triangles.converter.ImageConverter
import me.mars.triangles.converter.ImageSize
import me.mars.triangles.converter.MockImageConverter
import me.mars.triangles.layout.Layout
import me.mars.triangles.layout.LogicDisplayLayout
import me.mars.triangles.layout.TiledDisplayLayout
import me.mars.triangles.layout.TiledGifLayout
import me.mars.triangles.mod.PicToTri.bundle
import mindustry.world.blocks.logic.LogicDisplay

enum class FormStage {
    SelectFile,
    SelectLayoutType,
    BuildLayout,
    EditLayout,
    SelectConverterType,
    ConfigureConverter
}

data class FormState(
    val stage: FormStage = FormStage.SelectFile,

    // None, SelectFile
    val file: SelectedFile? = null,
    // SelectLayoutType
    val layoutType: LayoutSelection? = null,
    // BuildLayout
    val layoutBlueprint: LayoutBlueprint? = null,
    // EditLayout
    val layout: Layout<*>? = null,
    // SelectConverterType
    val converterType: ConverterType? = null,
    // ConfigureConverter
    val converter: Converter? = null
)

data class SelectedFile(val file: Fi, val name: String, val pixmap: Pixmap)

enum class LayoutType(val layoutName: String) {
    LogicDisplayLayout("logic-display-layout"),
    TiledDisplayLayout("tiled-display-layout"),
    TiledGifLayout("tiled-gif-layout")
}

data class LayoutSelection(val display: LogicDisplay, val type: LayoutType)

sealed class LayoutBlueprint {
    abstract val imageSize: ImageSize

    data class LogicDisplayLayoutBlueprint(override val imageSize: ImageSize): LayoutBlueprint()
    data class TiledDisplayLayoutBlueprint(override val imageSize: ImageSize, val chunkSize: Int) : LayoutBlueprint()
    data class TiledGifLayoutBlueprint(override val imageSize: ImageSize, val chunkSize: Int, val frameInterval: Int) : LayoutBlueprint()
}

enum class ConverterType(val displayName: String, val supportedTypes: List<Class<out Layout<*>>>,  val create: (Layout<*>, Fi) -> Converter, val validate: (Layout<*>, Fi) -> Unit) {
    Image(
        bundle("converter.image"),
        listOf(LogicDisplayLayout::class.java, TiledDisplayLayout::class.java),
        create = ::ImageConverter,
        validate = { layout, _ ->
            ImageConverter.validateLayout(layout)
        }
    ),
    Mock(
        "DEBUG(DO NOT USE)",
        listOf(LogicDisplayLayout::class.java, TiledDisplayLayout::class.java, TiledGifLayout::class.java),
        create = ::MockImageConverter,
        validate = { layout, _ ->
            MockImageConverter.validateLayout(layout)
        }
    ),
    Gif(
        bundle("converter.gif"),
        listOf(TiledGifLayout::class.java),
        create = ::GifConverter,
        validate = GifConverter::validateLayout
    )
}