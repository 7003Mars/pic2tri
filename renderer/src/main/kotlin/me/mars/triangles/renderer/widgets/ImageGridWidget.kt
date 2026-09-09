package me.mars.triangles.renderer.widgets

import app.cash.redwood.Modifier
import arc.graphics.Pixmap
import arc.math.geom.Point2
import arc.scene.Element
import me.mars.triangles.layout.Layout
import me.mars.triangles.renderer.elem.ImageGrid

class ImageGridWidget : me.mars.triangles.ui.schema.widget.ImageGrid<Element> {
    override val value: ImageGrid = ImageGrid()
    override var modifier: Modifier = Modifier

    override fun layout(layout: Layout<*>) {
        value.layout = layout
    }

    override fun pixmap(pixmap: Pixmap) {
        value.setDrawable(pixmap)
    }

    override fun onSelect(onSelect: (Point2) -> Unit) {
        value.onSelect = onSelect
    }


}