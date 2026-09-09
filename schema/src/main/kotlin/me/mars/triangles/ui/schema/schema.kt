package me.mars.triangles.ui.schema

import app.cash.redwood.schema.Schema
import me.mars.maple.schema.Primitives

@Schema(
    members = [
        ImageGrid::class
    ],
    dependencies = [
        Schema.Dependency(1, Primitives::class)
    ]
)
interface TrianglesUiSchema