package me.mars.triangles.mod.ui.components

import androidx.compose.runtime.Composable
import app.cash.redwood.Modifier
import me.mars.maple.schema.compose.ColumnScope
import me.mars.maple.ui.Column
import me.mars.maple.ui.Label
import me.mars.maple.ui.TextButton
import me.mars.maple.ui.TextField
import me.mars.triangles.mod.PicToTri.bundle
import me.mars.triangles.mod.ui.model.SelectedFile

@Composable
// TODO Requiring a specific Scope just to set the fill modifier seems a bit wonky. See if anything can be changed in maple.
fun ColumnScope.ImagePicker(
    file: SelectedFile?,
    showFilePicker: () -> Unit,
    onFileRenamed: (String) -> Unit
) {
    Column(modifier = Modifier.fill(fillX = true)) {
        TextButton(bundle("select"), onClick = showFilePicker, modifier = Modifier.fill(fillX = true))
        if (file != null) {
            Label("Schematic name")
            TextField(file.name, onFileRenamed, modifier = Modifier.fill(fillX = true))
        }
    }
}