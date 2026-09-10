package me.mars.triangles.mod.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.cash.redwood.Modifier
import arc.Core
import arc.files.Fi
import arc.graphics.Pixmap
import arc.graphics.PixmapIO
import arc.math.Mathf
import arc.math.geom.Point2
import arc.scene.event.VisibilityEvent
import arc.util.ArcRuntimeException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import me.mars.maple.schema.compose.BoxScope
import me.mars.maple.ui.Box
import me.mars.maple.ui.Column
import me.mars.maple.ui.Maple
import me.mars.maple.ui.Row
import me.mars.maple.ui.TextButton
import me.mars.triangles.converter.Converter
import me.mars.triangles.converter.GifConverter
import me.mars.triangles.converter.ImageConverter
import me.mars.triangles.converter.ImageGenerationService
import me.mars.triangles.converter.ImageSize
import me.mars.triangles.converter.MockImageConverter
import me.mars.triangles.layout.Layout
import me.mars.triangles.layout.LogicDisplayLayout
import me.mars.triangles.layout.TiledDisplayLayout
import me.mars.triangles.layout.TiledGifLayout
import me.mars.triangles.mod.PicToTri.bundle
import me.mars.triangles.mod.PicToTri.debugMode
import me.mars.triangles.mod.PicToTri.pixmapCheck
import me.mars.triangles.mod.ui.components.Sidebar
import me.mars.triangles.mod.ui.components.StepDisplay
import me.mars.triangles.mod.ui.form.ConverterEditStep
import me.mars.triangles.mod.ui.form.ConverterSelectionStep
import me.mars.triangles.mod.ui.form.ImageSelectionStep
import me.mars.triangles.mod.ui.form.LayoutBuilderStep
import me.mars.triangles.mod.ui.form.LayoutEditStep
import me.mars.triangles.mod.ui.form.LayoutTypeSelectionStep
import me.mars.triangles.mod.ui.model.*
import me.mars.triangles.renderer.TrianglesUiWidgetSystem
import me.mars.triangles.ui.schema.compose.ImageGrid
import me.mars.triangles.utils.Prefs.setting
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.gen.Tex
import mindustry.ui.FileChooser
import mindustry.ui.dialogs.BaseDialog
import mindustry.world.blocks.logic.LogicBlock
import mindustry.world.blocks.logic.LogicDisplay
import mindustry.world.blocks.logic.TileableLogicDisplay

class ConverterDialog : BaseDialog(bundle("mod-name")) {
    init {
        closeOnBack()
        addCloseButton()
        addListener {
            if (it is VisibilityEvent) {
                if (it.isHide) {
                    Vars.platform.endForceLandscape()
                } else {
                    Vars.platform.beginForceLandscape()
                }
            }
            return@addListener false // TODO Should it ever be true?
        }

        update {
            if (this.isShown) {
                refreshTaskData()
            }
        }


    }

    // region Sidebar state
    val tasks: MutableStateFlow<List<TaskData>> = MutableStateFlow(emptyList())
    val activeGenerators: MutableStateFlow<Int> = MutableStateFlow(0)

    fun refreshTaskData() {
        activeGenerators.value = ImageGenerationService.getActiveTasks().size
        tasks.update {
            it.map { task ->
                task.copy(completedTasks = task.converterTask.completedChunks, accuracy = 0f/* TODO */)
            }
        }
    }

    fun exportTask(taskData: TaskData) {
        val task = taskData.converterTask
        if (!task.result.isDone) {
            throw IllegalStateException("Task is not done")
        }
        val schem = task.converter.layout.build(task.result.get().chunks)
        schem.labels.add(Core.bundle[setting("mod-name")])
        schem.tags.put("name", if (debugMode) "!DEBUGMODE" else taskData.name)
        if (debugMode) {
            Vars.schematics.all().select { it.name() == "!DEBUGMODE" }.map { Vars.schematics.remove(it) }
        }
        Vars.schematics.add(schem)
        // TODO VERY HACKY WAY TO RELOAD SCHEMS
        Vars.ui.schematics.hide()
        Vars.ui.schematics.show()
        tasks.update { tasks -> tasks.filter { it.converterTask != taskData.converterTask } }
    }

    fun cancelTask(task: TaskData) {
        Vars.ui.showInfo("TODO")
        // TODO
    }
    // endregion


    // region Grid state
    val coords: MutableStateFlow<Point2> = MutableStateFlow(Point2())
    // endregion


    // region Form state
    val formState: MutableStateFlow<FormState> = MutableStateFlow(FormState())
    // Form history
    val history: MutableStateFlow<List<FormState>> = MutableStateFlow(listOf(formState.value)) // TODO Is this bad practice? Should we move this into an init block or smth
    // Step 2 options
    val layoutTypes: MutableStateFlow<Map<LayoutType, List<LogicDisplay>>> = MutableStateFlow(emptyMap())
    // Step 3 options
    val imageSnapSizes: MutableStateFlow<List<ImageSize>> = MutableStateFlow(emptyList())
    // Step 4 options
    val supportedConverterTypes: MutableStateFlow<List<ConverterType>> = MutableStateFlow(emptyList())
    val validationError: MutableStateFlow<String?> = MutableStateFlow(null)

    fun validateStep(form: FormState): Boolean {
        return when (form.stage) {
            FormStage.SelectFile -> form.file != null
            FormStage.SelectLayoutType -> form.layoutType != null
            FormStage.BuildLayout -> form.layoutBlueprint != null
            FormStage.EditLayout -> true
            FormStage.SelectConverterType -> {
                var errorMsg: String? = null
                try {
                    form.converterType?.validate(form.layout!!, form.file!!.file)
                } catch (e: Converter.UnsupportedLayoutException) {
                    errorMsg = e.reason
                }
                validationError.value = errorMsg
                form.converterType != null && errorMsg == null
            }
            FormStage.ConfigureConverter -> true
        }
    }

    fun goToStage(stage: FormStage) {
        formState.value = history.value[stage.ordinal]
    }

    fun nextStep() {
        val form: FormState = formState.value
        val updated: FormState = when (form.stage) {
            FormStage.SelectFile -> {
                layoutTypes.update { getLayoutTypes() }
                form.copy(stage = FormStage.SelectLayoutType)
            }
            FormStage.SelectLayoutType -> {
                val sizes = calculateSnapSizes(form.layoutType!!.display, ImageSize(form.file!!.pixmap.width, form.file.pixmap.height))
                imageSnapSizes.update { sizes }
                val blueprint = when (form.layoutType.type) {
                    LayoutType.LogicDisplayLayout -> LayoutBlueprint.LogicDisplayLayoutBlueprint(sizes[0])
                    LayoutType.TiledDisplayLayout -> LayoutBlueprint.TiledDisplayLayoutBlueprint(sizes[0], 3)
                    LayoutType.TiledGifLayout -> LayoutBlueprint.TiledGifLayoutBlueprint(sizes[0], 3, 1)
                }
                form.copy(layoutBlueprint = blueprint, stage = FormStage.BuildLayout)
            }
            FormStage.BuildLayout -> {
                form.copy(stage = FormStage.EditLayout, layout = buildLayout(form.layoutType!!, form.layoutBlueprint!!))
            }
            FormStage.EditLayout -> {
                supportedConverterTypes.update { getSupportedConverterTypes(form.layout!!) }
                form.copy(stage = FormStage.SelectConverterType)
            }
            FormStage.SelectConverterType -> {
                val converter: Converter = when (form.converterType!!) {
                    ConverterType.Image -> ImageConverter(form.layout, form.file!!.file)
                    ConverterType.Mock -> MockImageConverter(form.layout, form.file!!.file)
                    ConverterType.Gif -> GifConverter(form.layout, form.file!!.file)
                }
                form.copy(stage = FormStage.ConfigureConverter, converter = converter)
            }
            FormStage.ConfigureConverter -> {
                submit(form)
                FormState()
            }
        }
        history.update { if (form.stage == FormStage.ConfigureConverter) listOf(updated) else  it.slice(0..<form.stage.ordinal) + form }

        formState.value = updated
    }

    fun submit(form: FormState) {
        val task = form.converter!!.submit()
        form.file!!.pixmap.dispose()
        tasks.update { it + TaskData(form.file.name, task, totalTasks = task.totalChunks) }
    }

    // region Step 1: Image selection
    fun selectFile(path: Fi) {
        Core.settings.put(pixmapCheck, true)
        Core.settings.forceSave()
        val pixmap = if (Core.settings.getBool(setting("java-loader"))) PixmapIO.readPNG(path) else Pixmap(path)
        Core.settings.put(pixmapCheck, false)
        Core.settings.forceSave()

        formState.update {
            it.file?.pixmap?.dispose()
            it.copy(file = SelectedFile(path, path.nameWithoutExtension(), pixmap))
        }

        if (path.extEquals("gif") || path.extEquals("webp")) {
            Vars.ui.showInfo(bundle("gif-warning"))
        }
    }

    fun updateFileName(name: String) {
        formState.update {
            it.copy(file = it.file!!.copy(name = name))
        }
    }

    fun showImagePicker() {
        val extensions = if (Core.settings.getBool(setting("java-loader"))) arrayOf("png") else arrayOf("jpg", "jpeg", "png", "bmp", "psd", "hdr", "pic", "ppm", "pgm")
        FileChooser.open(*extensions).submit {
            try {
                selectFile(it)
            } catch (_: ArcRuntimeException) {
                Vars.ui.showErrorMessage(bundle("load-fail"))
            }
        }
    }
    // endregion

    // region Step 2: Layout type selection
    fun setLayoutSelection(layoutSelection: LayoutSelection) {
        formState.update {
            it.copy(layoutType = layoutSelection)
        }
    }

    fun getLayoutTypes(): Map<LayoutType, List<LogicDisplay>> {
        val types: MutableMap<LayoutType, List<LogicDisplay>> = mutableMapOf()
        val displays = Vars.content.blocks().select { it is LogicDisplay }.`as`<LogicDisplay>().toList()

        types[LayoutType.LogicDisplayLayout] = displays.filter { it !is TileableLogicDisplay }
        types[LayoutType.TiledDisplayLayout] = displays.filterIsInstance<TileableLogicDisplay>()
        types[LayoutType.TiledGifLayout] = displays.filterIsInstance<TileableLogicDisplay>()

        return types.toMap()
    }
    // endregion

    // region Step 3: Layout building
    fun editLayoutBlueprint(layoutBlueprint: LayoutBlueprint) {
        formState.update {
            it.copy(layoutBlueprint = layoutBlueprint, layout = buildLayout(it.layoutType!!, layoutBlueprint))
        }
    }

    fun calculateSnapSizes(display: LogicDisplay, imageSize: ImageSize): List<ImageSize> {
        var maxSize = Mathf.ceilPositive((Blocks.microProcessor as LogicBlock).range/Vars.tilesize*2)
        if (display is TileableLogicDisplay) {
            maxSize = Math.min(display.maxDisplayDimensions, maxSize)
        }
        return buildList {
            val loss = if (display is TileableLogicDisplay) 12/* TileableLogicDisplay#frameSize*2 */ else 0
            for (i in 1 ..(maxSize/display.size)) {
                val targetDim = i*display.displaySize - loss
                // Scale to fit width
                val xScl: Float = (targetDim) / imageSize.width.toFloat()
                val imageHeight: Int = (imageSize.height * xScl).toInt()
                if (!(display is TileableLogicDisplay && imageHeight > 512-loss )) {
                    this.add(ImageSize(targetDim, imageHeight))
                }
                // Scale to fit height
                val yScl: Float = (targetDim) / imageSize.height.toFloat()
                val imageWidth: Int = (imageSize.width * yScl).toInt()
                if (!(display is TileableLogicDisplay && imageWidth > 512-loss)) {
                    this.add(ImageSize(imageWidth, targetDim))
                }
            }
            this.sortBy { it.width * it.height }
        }.distinct()
    }

    fun buildLayout(layoutSelection: LayoutSelection, formBlueprint: LayoutBlueprint): Layout<*> {
        val imageSize = formBlueprint.imageSize
        val display = layoutSelection.display
        return when (layoutSelection.type) {
            LayoutType.LogicDisplayLayout -> LogicDisplayLayout(display, imageSize.width, imageSize.height)
            LayoutType.TiledDisplayLayout -> {
                val blueprint = formBlueprint as LayoutBlueprint.TiledDisplayLayoutBlueprint
                TiledDisplayLayout(display as TileableLogicDisplay, imageSize.width, imageSize.height, blueprint.chunkSize)
            }
            LayoutType.TiledGifLayout -> {
                val blueprint = formBlueprint as LayoutBlueprint.TiledGifLayoutBlueprint
                TiledGifLayout(display as TileableLogicDisplay, imageSize.width, imageSize.height, blueprint.chunkSize, blueprint.frameInterval)
            }
        }
    }

    // endregion

    // region Step 4: Layout editing
    /**
     * Unfortunately, I haven't figured how to make layout editing immutable, so we rn the Composable directly updates the same object
     */
    // endregion

    // region Step 5: Converter type selection
    fun setConverterType(converterType: ConverterType) {
        formState.update {
            it.copy(converterType = converterType)
        }
        validationError.value = null
    }

    fun getSupportedConverterTypes(layout: Layout<*>): List<ConverterType> {
        return ConverterType.entries.filter { layout.javaClass in it.supportedTypes }
    }
    // endregion

    // region Step 6: Converter config
    // TODO
    // endregion
    // endregion


    // We need to make sure all fields have been initialised before we run the composable.
    init {
        val ui = Maple.runMaple(TrianglesUiWidgetSystem(), {
            Content(this@ConverterDialog)
        })
        // TODO This should be part of maple, but the files pulled from the repo are outdated
        cont.add(ui.root).grow()
    }
}

@Composable
private fun BoxScope.Content(dialog: ConverterDialog) {
    val activeGenerators by dialog.activeGenerators.collectAsState()
    val tasks by dialog.tasks.collectAsState()

    val history by dialog.history.collectAsState()
    val state: FormState by dialog.formState.collectAsState()

    val coords by dialog.coords.collectAsState()
    val layoutTypes by dialog.layoutTypes.collectAsState()
    val imageSnapSizes by dialog.imageSnapSizes.collectAsState()
    val supportedConverterTypes by dialog.supportedConverterTypes.collectAsState()
    val validationError by dialog.validationError.collectAsState()

    Row(modifier = Modifier.fill(true, true)) {
        // Left sidebar
        Sidebar(
            activeGenerators, tasks,
            dialog::exportTask, dialog::cancelTask,
            modifier = Modifier.fill(true, true)
        )
        // Grid
        if (state.layout != null) {
            ImageGrid(
                state.layout!!, state.file!!.pixmap,
                onSelect = { dialog.coords.value = it },
                modifier = Modifier.weight(1f).fill(true, true)
            )
        } else {
            Box(modifier = Modifier.weight(1f)) {}
        }
        // Form
        Column(background = Tex.pane, modifier = Modifier.fill(true, true).sizeIn(minWidth = 350f)) {
            StepDisplay(state.stage, history.last().stage, dialog::goToStage)
            when (state.stage) {
                FormStage.SelectFile -> ImageSelectionStep(state.file, dialog::showImagePicker, dialog::updateFileName)
                FormStage.SelectLayoutType -> LayoutTypeSelectionStep(
                    state.file!!, dialog::showImagePicker, dialog::updateFileName,
                    layoutTypes, dialog::setLayoutSelection
                )
                FormStage.BuildLayout -> {
                    LayoutBuilderStep(
                        imageSnapSizes, state.layoutBlueprint!!,
                        dialog::editLayoutBlueprint
                    )
                }
                FormStage.EditLayout -> LayoutEditStep(state.layout!!, coords)
                FormStage.SelectConverterType -> ConverterSelectionStep(
                    supportedConverterTypes, state.converterType, validationError,
                    dialog::setConverterType
                )
                FormStage.ConfigureConverter -> ConverterEditStep()
            }
            Box(modifier = Modifier.weight(1f)) {  } // TODO Add spacer element to maple
            TextButton("Next", onClick = {
                if (dialog.validateStep(state)) {
                    dialog.nextStep()
                }
            }, modifier = Modifier.fill(fillX = true))
        }
    }
}