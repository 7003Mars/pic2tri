package me.mars.triangles.mod.ui.form

import androidx.compose.runtime.Composable
import arc.Core
import me.mars.maple.ui.Label
import me.mars.maple.ui.Slider
import me.mars.triangles.converter.ImageSize
import me.mars.triangles.mod.PicToTri.bundle
import me.mars.triangles.mod.ui.components.ValueSlider
import me.mars.triangles.mod.ui.model.LayoutBlueprint
import me.mars.triangles.utils.Prefs.internalName

@Composable
fun LayoutBuilderStep(
    imageSizes: List<ImageSize>,
    layoutBlueprint: LayoutBlueprint,
    onLayoutBlueprintChanged: (LayoutBlueprint) -> Unit,
) {
    fun changeImageSize(newSize: ImageSize) {
        val updated = when (layoutBlueprint) {
            is LayoutBlueprint.LogicDisplayLayoutBlueprint -> layoutBlueprint.copy(imageSize = newSize)
            is LayoutBlueprint.TiledDisplayLayoutBlueprint -> layoutBlueprint.copy(imageSize = newSize)
            is LayoutBlueprint.TiledGifLayoutBlueprint -> layoutBlueprint.copy(imageSize = newSize)
        }
        onLayoutBlueprintChanged(updated)
    }

    Label(bundle("image-scale"))
    ValueSlider(imageSizes, layoutBlueprint.imageSize, onValueChanged = { changeImageSize(it) })
    Label(Core.bundle.format("${internalName}.layout-size", layoutBlueprint.imageSize.width, layoutBlueprint.imageSize.height))

    when (layoutBlueprint) {
        is LayoutBlueprint.LogicDisplayLayoutBlueprint -> {} // No extra options to configure
        is LayoutBlueprint.TiledDisplayLayoutBlueprint -> {
            Label(bundle("chunk-size"))
            Slider(
                layoutBlueprint.chunkSize.toFloat(),
                3f, 16f, 1f,
                onValueChanged = { onLayoutBlueprintChanged(layoutBlueprint.copy(chunkSize = it.toInt())) }
            )
        }
        is LayoutBlueprint.TiledGifLayoutBlueprint -> {
            Label(bundle("chunk-size"))
            Slider(
                layoutBlueprint.chunkSize.toFloat(),
                3f, 16f, 1f,
                onValueChanged = { onLayoutBlueprintChanged(layoutBlueprint.copy(chunkSize = it.toInt())) }
            )
            Label(Core.bundle.format("fps", 60/layoutBlueprint.frameInterval))
            ValueSlider(
                listOf(1, 2, 3, 4, 5, 6, 10, 12, 15, 20, 30),
                layoutBlueprint.frameInterval,
                onValueChanged = { onLayoutBlueprintChanged(layoutBlueprint.copy(frameInterval = it)) }
            )
        }

    }
}