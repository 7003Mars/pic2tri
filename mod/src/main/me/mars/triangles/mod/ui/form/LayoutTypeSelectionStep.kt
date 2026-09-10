package me.mars.triangles.mod.ui.form

//import me.mars.maple.ui.ImageButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.cash.redwood.Modifier
import arc.scene.style.TextureRegionDrawable
import arc.scene.ui.ImageButton
import me.mars.maple.schema.compose.ColumnScope
import me.mars.maple.ui.*
import me.mars.triangles.mod.PicToTri.bundle
import me.mars.triangles.mod.ui.model.LayoutSelection
import me.mars.triangles.mod.ui.model.LayoutType
import mindustry.gen.Icon
import mindustry.ui.Styles
import mindustry.world.blocks.logic.LogicDisplay

@Composable
fun ColumnScope.LayoutTypeSelectionStep(
    layoutTypes: Map<LayoutType, List<LogicDisplay>>,
    layoutSelection: LayoutSelection?,
    onLayoutTypeSelected: (LayoutSelection) -> Unit,
    ) {
    val displayIconRegions = remember { layoutTypes.values.flatten().associateWith { TextureRegionDrawable(it.uiIcon) } }

    Column(modifier = Modifier.fill(true)) {
        layoutTypes.forEach {
            Column {
                Row {
                    Label(bundle("layout.${it.key.layoutName}"))
                    ImageButton(Icon.eyeSmall, {}, style = Styles.clearNonei) // TODO Should be the question mark icon, also get descriptions for each layout bundled.
                }
                ScrollPane(xScrollingDisabled = false, yScrollingDisabled = true, scrollbarsOnTop = false) {
                    Row {
                        for (display in it.value) {
                            ImageButton(
                                displayIconRegions[display]!!,
                                onClick = { onLayoutTypeSelected(LayoutSelection(display, it.key)) },
                                style = if (layoutSelection != null && layoutSelection.type == it.key && layoutSelection.display == display) ImageButton.ImageButtonStyle(Styles.defaulti).apply { up = over } else Styles.defaulti,
                                modifier = Modifier.padding(left = 5, right = 5).sizeIn(50f, 50f, 50f, 50f))
                        }
                    }
                }

            }
        }
    }

}