package me.mars.triangles.mod.ui.form

import androidx.compose.runtime.*
import arc.Core
import arc.math.geom.Point2
import me.mars.maple.ui.Column
import me.mars.maple.ui.Label
import me.mars.maple.ui.Slider
import me.mars.triangles.layout.Layout
import me.mars.triangles.layout.LogicDisplayLayout
import me.mars.triangles.layout.TiledDisplayLayout
import me.mars.triangles.mod.PicToTri.bundle
import me.mars.triangles.utils.Prefs.internalName

@Composable
fun LayoutEditStep(
    layout: Layout<*>,
    coords: Point2,
) {
    Column {
        when (layout) {
            is LogicDisplayLayout -> LogicDisplayLayoutEditor(layout, coords)
            is TiledDisplayLayout -> TiledDisplayLayoutEditor(layout, coords)
        }
    }
}

@Composable
private fun LogicDisplayLayoutEditor(
    layout: LogicDisplayLayout,
    coords: Point2
) {
    var refresh by remember { mutableStateOf(0) }
    val chunkData = remember(coords, refresh) { layout.getChunk(coords.x, coords.y)?.data }

    if (chunkData == null) {
        Label(bundle("select-display"))
        return
    }

    Label(Core.bundle.format("$internalName.layout.logic-display-layout.proc-count", chunkData.procs.size))
    Slider(
        chunkData.procs.size.toFloat(),
        1f, LogicDisplayLayout.MAX_PROCS.toFloat(), 1f,
        onValueChanged = {
            layout.requestProcs(coords.x, coords.y, it.toInt()); refresh++
        },
    )
}

@Composable
private fun TiledDisplayLayoutEditor(
    layout: TiledDisplayLayout,
    coords: Point2
) {
    var refresh by remember { mutableStateOf(0) }
    val chunkData = remember(coords, refresh) { layout.getChunk(coords.x, coords.y)?.data }

    if (chunkData == null) {
        Label(bundle("select-display"))
    } else {
        Label(Core.bundle.format("$internalName.layout.tiled-display-layout.proc-count", chunkData.procs, layout.workerPos.size))
    }
    Slider(
        layout.workerPos.size.toFloat(),
        1f, LogicDisplayLayout.MAX_PROCS.toFloat()/*We are still limited by the same number of simultaneous draws*/, 1f,
        { layout.requestProcs(it.toInt()); refresh++ }
    )
}
