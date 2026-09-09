package me.mars.triangles.renderer

import arc.scene.Element
import me.mars.maple.renderer.ArcUiWidgetFactory
import me.mars.triangles.renderer.widgets.ImageGridWidget
import me.mars.triangles.ui.schema.widget.ImageGrid
import me.mars.triangles.ui.schema.widget.TrianglesUiSchemaWidgetFactory
import me.mars.triangles.ui.schema.widget.TrianglesUiSchemaWidgetSystem

class TrianglesUiWidgetFactory : TrianglesUiSchemaWidgetFactory<Element> {
    override fun ImageGrid(): ImageGrid<Element> = ImageGridWidget()
}

fun TrianglesUiWidgetSystem(): TrianglesUiSchemaWidgetSystem<Element> {
    return TrianglesUiSchemaWidgetSystem(
        Primitives = ArcUiWidgetFactory(),
        TrianglesUiSchema = TrianglesUiWidgetFactory()
    )
}