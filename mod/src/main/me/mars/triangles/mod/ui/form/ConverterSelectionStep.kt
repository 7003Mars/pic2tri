package me.mars.triangles.mod.ui.form

import androidx.compose.runtime.Composable
import app.cash.redwood.Modifier
import arc.scene.ui.TextButton
import me.mars.maple.schema.compose.ColumnScope
import me.mars.maple.ui.Column
import me.mars.maple.ui.Label
import me.mars.maple.ui.TextButton
import me.mars.triangles.mod.ui.model.ConverterType
import mindustry.ui.Styles

@Composable
fun ColumnScope.ConverterSelectionStep(
    options: List<ConverterType>,
    selected: ConverterType?,
    validationError: String?,
    onConverterSelected: (ConverterType) -> Unit,
) {
    Column(modifier = Modifier.fill(fillX = true)) {
        Label("Select converter")
        for (option in options) {
            TextButton(option.displayName, onClick = { onConverterSelected(option) }, buttonStyle = if (option == selected) TextButton.TextButtonStyle(Styles.defaultt).apply { up = over } else Styles.defaultt)
            if (option == selected && validationError != null) {
                Label(validationError)
            }
        }
    }
}

