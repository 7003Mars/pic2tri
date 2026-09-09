package me.mars.triangles.mod.ui.form

//import me.mars.maple.ui.ImageButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.cash.redwood.Modifier
import arc.scene.style.TextureRegionDrawable
import me.mars.maple.ui.*
import me.mars.triangles.mod.PicToTri.bundle
import me.mars.triangles.mod.ui.components.ImagePicker
import me.mars.triangles.mod.ui.model.LayoutSelection
import me.mars.triangles.mod.ui.model.LayoutType
import me.mars.triangles.mod.ui.model.SelectedFile
import mindustry.gen.Icon
import mindustry.ui.Styles
import mindustry.world.blocks.logic.LogicDisplay

@Composable
fun LayoutTypeSelectionStep(
    file: SelectedFile,
    showFilePicker: () -> Unit,
    onFileRenamed: (String) -> Unit,
    layoutTypes: Map<LayoutType, List<LogicDisplay>>,
    onLayoutTypeSelected: (LayoutSelection) -> Unit,
    ) {
    val displayIconRegions = remember { layoutTypes.values.flatten().associateWith { TextureRegionDrawable(it.uiIcon) } }

    Column {
        ImagePicker(file, showFilePicker, onFileRenamed)
        layoutTypes.forEach {
            Column {
                Row {
                    Label(bundle("layout.${it.key.layoutName}"))
                    ImageButton(Icon.eyeSmall, {}, style = Styles.clearNonei) // TODO Should be the question mark icon, also get descriptions for each layout bundled.
                }
                ScrollPane(xScrollingDisabled = false, yScrollingDisabled = true, scrollbarsOnTop = false) {
                    Row {
                        for (display in it.value) {
                            ImageButton(displayIconRegions[display]!!, onClick = {
                                onLayoutTypeSelected(LayoutSelection(display, it.key))
                            }, modifier = Modifier.padding(left = 5, right = 5).sizeIn(50f, 50f, 50f, 50f))
                        }
                    }
                }

            }
        }
    }

}