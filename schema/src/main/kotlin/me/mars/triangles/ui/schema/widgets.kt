package me.mars.triangles.ui.schema

import app.cash.redwood.schema.Property
import app.cash.redwood.schema.Widget
import arc.graphics.Pixmap
import arc.math.geom.Point2
import me.mars.triangles.layout.Layout

@Widget(1)
data class ImageGrid(
    @Property(1) val layout: Layout<*>,
    @Property(2) val pixmap: Pixmap,
    @Property(3) val onSelect: (Point2) -> Unit
)