package me.mars.triangles.mod.ui.components

import androidx.compose.runtime.Composable
import me.mars.maple.ui.Slider

@Composable
fun <T> ValueSlider(
    items: List<T>,
    selected: T,
    onValueChanged: (T) -> Unit
) {

    Slider(
        items.indexOf(selected).toFloat(),
        min = 0f, max = items.size.toFloat()-1, stepSize = 1f,
        onValueChanged = { onValueChanged(items[it.toInt()]) }
    )
}