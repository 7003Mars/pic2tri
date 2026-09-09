package me.mars.triangles.mod.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.redwood.Modifier
import me.mars.maple.schema.api.Arrangement
import me.mars.maple.schema.compose.ColumnScope
import me.mars.maple.schema.compose.RowScope
import me.mars.maple.schema.modifier.Align
import me.mars.maple.ui.*
import me.mars.triangles.converter.ImageGenerationService
import me.mars.triangles.mod.ui.Styles2
import me.mars.triangles.mod.ui.model.TaskData
import mindustry.gen.Icon
import mindustry.gen.Tex
import mindustry.graphics.Pal

@Composable
fun Sidebar(
    activeGenerators: Int,
    tasks: List<TaskData>,
    onExport: (task: TaskData) -> Unit,
    onCancel: (task: TaskData) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(arrangement = Arrangement.Start, modifier = Modifier.then(modifier)) {
        Label("Active generators:")
        Bar(
            "$activeGenerators/${ImageGenerationService.MAX_THREADS}", Pal.bar,
            activeGenerators.toFloat()/ImageGenerationService.MAX_THREADS,
            modifier = Modifier.sizeIn(minWidth = 150f, minHeight = 30f).fill(true, true)
        )
        for (task in tasks) {
            TaskView(
                task,
                onExport = { onExport(task) },
                onCancel = { onCancel(task) }
            )
        }
    }
}

@Composable
fun ColumnScope.TaskView(
    task: TaskData,
    onExport: () -> Unit,
    onCancel: () -> Unit
) {
    var collapsed: Boolean by remember { mutableStateOf(true) }
    val complete = task.completedTasks == task.totalTasks

    Column(background = Tex.pane, modifier = Modifier.padding(5, 5, 5, 5)) {
        Bar("${task.completedTasks}/${task.totalTasks}", Pal.bar, task.completedTasks.toFloat()/task.totalTasks, modifier = Modifier.sizeIn(150f, 50f).fill(true, true))
        val mod = Modifier.sizeIn(40f, 40f, 40f, 40f)
        Row {
            ImageButton(Icon.exportSmall, onExport, disabled = !complete, modifier = mod)
            ImageButton(Icon.cancelSmall, onCancel, modifier = mod)
            CheckBox("", !collapsed, { collapsed = !collapsed }, Styles2.collapseStyle, modifier = mod)
        }
        Collapser(collapsed) {
            Row(arrangement = Arrangement.SpaceBetween) {
                Label("Accuracy: ")
                Label(task.accuracy.toString())
            }
        }
    }
}