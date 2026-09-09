package me.mars.triangles.mod.ui.form

import androidx.compose.runtime.Composable
import app.cash.redwood.Modifier
import me.mars.maple.schema.compose.ColumnScope
import me.mars.maple.ui.Column
import me.mars.triangles.mod.ui.components.ImagePicker
import me.mars.triangles.mod.ui.model.SelectedFile

@Composable
fun ColumnScope.ImageSelectionStep(
    file: SelectedFile?,
    showFilePicker: () -> Unit,
    onFileRenamed: (String) -> Unit
) {
    Column(modifier = Modifier.fill(fillX = true)) {
        ImagePicker(file, showFilePicker, onFileRenamed)
    }
}