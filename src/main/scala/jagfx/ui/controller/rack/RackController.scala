package jagfx.ui.controller.rack

import jagfx.synth.SynthesisExecutor
import jagfx.ui.BindingManager
import jagfx.ui.components.canvas.*
import jagfx.ui.components.pane.*
import jagfx.ui.controller.ControllerLike
import jagfx.ui.controller.inspector.InspectorController
import jagfx.ui.viewmodel.*
import jagfx.utils.ColorUtils
import javafx.scene.layout.*

/** Grid-based controller for rack cells displaying envelope and filter editors.
  */
class RackController(viewModel: SynthViewModel, inspector: InspectorController)
    extends ControllerLike[GridPane]:
  // Fields
  protected val view: GridPane = GridPane()
  private val cells = new Array[JagCellPane](12)
  private val bindingManager = BindingManager()
  private val outputWaveformCanvas = JagWaveformCanvas()
  private val poleZeroCanvas = JagPoleZeroCanvas()
  private val freqResponseCanvas = JagFrequencyResponseCanvas()
  private val editor = new RackEditor(viewModel)
  private val factory = new RackCellFactory(
    poleZeroCanvas,
    freqResponseCanvas,
    outputWaveformCanvas,
    selectCell,
    editor.toggleEditorMode
  )
  private val filterDisplay = new VBox(2):
    getChildren.addAll(poleZeroCanvas, freqResponseCanvas)
    VBox.setVgrow(freqResponseCanvas, Priority.ALWAYS)

  // Init: styling
  view.getStyleClass.add("rack")
  view.setHgap(1)
  view.setVgap(1)
  outputWaveformCanvas.setZoom(4)

  // Init: listeners
  bindingManager.listen(viewModel.activeVoiceIndexProperty)(_ =>
    bindActiveVoice()
  )
  bindingManager.listen(viewModel.fileLoadedProperty)(_ => bindActiveVoice())
  bindingManager.listen(viewModel.selectedCellIndex)(_ => updateSelection())

  for i <- 0 until viewModel.getVoices.size do
    val voiceIdx = i
    viewModel.getVoices
      .get(i)
      .addChangeListener(() =>
        if viewModel.getActiveVoiceIndex == voiceIdx then updateOutputWaveform()
      )

  // Init: build grid
  buildGrid()

  /** Binds active voice to all cells. */
  def bind(): Unit =
    bindActiveVoice()

  /** Sets playhead position on waveform canvas. */
  def setPlayheadPosition(position: Double): Unit =
    outputWaveformCanvas.setPlayheadPosition(position)

  /** Hides playhead on waveform canvas. */
  def hidePlayhead(): Unit =
    outputWaveformCanvas.hidePlayhead()

  private def buildGrid(): Unit =
    view.getChildren.clear()
    view.getColumnConstraints.clear()
    view.getRowConstraints.clear()

    setupGridConstraints()

    createAndAddCell(0, 0, 0)
    createAndAddCell(1, 0, 1)
    createAndAddCell(2, 0, 2)

    createAndAddCell(4, 1, 0)
    createAndAddCell(5, 1, 1)
    createAndAddCell(6, 1, 2)

    createAndAddCell(7, 2, 0)
    createAndAddCell(9, 2, 1)
    createAndAddCell(10, 2, 2)

    cells(8) = factory.createCell(8)
    view.add(cells(8), 0, 3, 3, 1)

    val filterCell = JagCellPane("FILTER DISPLAY")
    filterCell.setFeatures(false)
    filterCell.setShowZoomButtons(false)
    val fWrapper = filterCell.getCanvas.getParent.asInstanceOf[Pane]
    filterCell.getCanvas.setVisible(false)
    fWrapper.getChildren.clear()
    fWrapper.getChildren.add(filterDisplay)

    poleZeroCanvas.widthProperty.bind(fWrapper.widthProperty)
    poleZeroCanvas.heightProperty.bind(fWrapper.heightProperty.divide(2))
    freqResponseCanvas.widthProperty.bind(fWrapper.widthProperty)
    freqResponseCanvas.heightProperty.bind(fWrapper.heightProperty.divide(2))

    view.add(filterCell, 3, 0, 1, 4)

    view.add(editor.getView, 0, 0, 4, 4)
    editor.getCanvas.widthProperty.bind(view.widthProperty.subtract(20))
    editor.getCanvas.heightProperty.bind(view.heightProperty.subtract(60))

    updateSelection()

  private def setupGridConstraints(): Unit =
    val colConstraint = new ColumnConstraints()
    colConstraint.setPercentWidth(25)
    for _ <- 0 until 4 do view.getColumnConstraints.add(colConstraint)

    val rowConstraint = new RowConstraints()
    rowConstraint.setVgrow(Priority.ALWAYS)
    for _ <- 0 until 4 do view.getRowConstraints.add(rowConstraint)

  private def createAndAddCell(defIdx: Int, col: Int, row: Int): Unit =
    val cell = factory.createCell(defIdx)
    cells(defIdx) = cell
    view.add(cell, col, row)

  private def updateSelection(): Unit =
    val selectedIdx = viewModel.selectedCellIndex.get
    cells.zipWithIndex.foreach { case (cell, idx) =>
      if cell != null then
        val isSel = idx == selectedIdx
        if cell.selectedProperty.get != isSel then
          cell.selectedProperty.set(isSel)
    }
    if selectedIdx >= 0 && selectedIdx < cells.length then
      bindInspector(selectedIdx)

  private def selectCell(idx: Int): Unit =
    viewModel.selectedCellIndex.set(idx)

  private def bindInspector(idx: Int): Unit =
    val voice = viewModel.getActiveVoice
    val cellDef = RackDefs.cellDefs(idx)
    cellDef.cellType match
      case CellType.Filter =>
        inspector.bindFilter(voice.filterViewMode, cellDef.desc)
      case CellType.Envelope(getter, _) =>
        val env = getter(voice)
        inspector.bind(env, cellDef.desc)
      case _ => inspector.hide()

  private def bindActiveVoice(): Unit =
    val voice = viewModel.getActiveVoice
    for idx <- cells.indices if cells(idx) != null do
      val cellDef = RackDefs.cellDefs(idx)
      cellDef.cellType match
        case CellType.Envelope(getter, _) =>
          cells(idx).setViewModel(getter(voice))
          if cellDef.title.startsWith("G.") then
            cells(idx).getCanvas match
              case c: JagEnvelopeCanvas => c.setGraphColor(ColorUtils.Gating)
              case null                 =>
        case _ =>

    poleZeroCanvas.setViewModel(voice.filterViewMode)
    freqResponseCanvas.setViewModel(voice.filterViewMode)

    updateOutputWaveform()

  private def updateOutputWaveform(): Unit =
    viewModel.getActiveVoice.toModel() match
      case Some(voice) =>
        SynthesisExecutor.synthesizeVoice(voice) { audio =>
          outputWaveformCanvas.setAudioBuffer(audio)
        }
      case None =>
        outputWaveformCanvas.clearAudio()
