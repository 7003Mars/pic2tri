package me.mars.triangles.mod.ui.components

import androidx.compose.runtime.Composable
import arc.scene.ui.TextButton
import me.mars.maple.ui.Column
import me.mars.maple.ui.Label
import me.mars.maple.ui.Row
import me.mars.maple.ui.TextButton
import me.mars.triangles.mod.PicToTri.bundle
import me.mars.triangles.mod.ui.model.FormStage
import mindustry.ui.Styles

@Composable
fun StepDisplay(
    curStage: FormStage,
    furthestStage: FormStage,
    onStageSelected: (FormStage) -> Unit
) {
    Column {
        Label(bundle("form-stage.${curStage.name}"))
        Row {
            for (stage in FormStage.entries) {
                val stageNum = stage.ordinal.toString()
                if (stage == curStage) {
                    Column {
                        TextButton(stageNum, {}, buttonStyle = TextButton.TextButtonStyle(Styles.defaultt).apply {  })
                        Label("^")
                    }
                } else {
                    TextButton(stageNum, { onStageSelected(stage) }, buttonStyle = TextButton.TextButtonStyle(Styles.defaultt).apply {  }, disabled = stage > furthestStage)
                }
            }
        }
    }
}