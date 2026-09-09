package me.mars.triangles.mod.ui.model

import me.mars.triangles.converter.Converter

data class TaskData(
    val name: String,
    val converterTask: Converter.ConverterTask,
    val completedTasks: Int = 0, val totalTasks: Int, val accuracy: Float = 0f
)