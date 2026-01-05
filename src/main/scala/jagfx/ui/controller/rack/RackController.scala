package jagfx.ui.controller.rack

import javafx.scene.layout._
import jagfx.ui.viewmodel._
import jagfx.ui.components.canvas._
import jagfx.ui.components.pane._
import jagfx.synth.SynthesisExecutor
import jagfx.ui.BindingManager
import jagfx.ui.controller.IController
import jagfx.ui.controller.inspector.InspectorController
import jagfx.utils.ColorUtils

class RackController(viewModel: SynthViewModel, inspector: InspectorController)
    extends IController[GridPane]:
  protected val view = GridPane()
  view.getStyleClass.add("rack")
  view.setHgap(1)
  view.setVgap(1)

  private val _cells = new Array[JagCellPane](12)

  private val _outputWaveformCanvas = JagWaveformCanvas()
  _outputWaveformCanvas.setZoom(4)

  private val _poleZeroCanvas = JagPoleZeroCanvas()
  private val _freqResponseCanvas = JagFrequencyResponseCanvas()

  private val _editor = new RackEditor(viewModel)
  private val _factory = new RackCellFactory(
    viewModel,
    _poleZeroCanvas,
    _freqResponseCanvas,
    _outputWaveformCanvas,
    _selectCell,
    _editor.toggleEditorMode
  )

  private val _filterDisplay = new VBox(2):
    getChildren.addAll(_poleZeroCanvas, _freqResponseCanvas)
    VBox.setVgrow(_freqResponseCanvas, Priority.ALWAYS)

  private val _bindingManager = BindingManager()

  _buildGrid()

  def bind(): Unit =
    _bindActiveTone()

  _bindingManager.listen(viewModel.activeToneIndexProperty)(_ =>
    _bindActiveTone()
  )
  _bindingManager.listen(viewModel.fileLoadedProperty)(_ => _bindActiveTone())
  _bindingManager.listen(viewModel.selectedCellIndex)(_ => _updateSelection())

  for i <- 0 until viewModel.getTones.size do
    val toneIdx = i
    viewModel.getTones
      .get(i)
      .addChangeListener(() =>
        if viewModel.getActiveToneIndex == toneIdx then _updateOutputWaveform()
      )

  def setPlayheadPosition(position: Double): Unit =
    _outputWaveformCanvas.setPlayheadPosition(position)

  def hidePlayhead(): Unit =
    _outputWaveformCanvas.hidePlayhead()

  private def _buildGrid(): Unit =
    view.getChildren.clear()
    view.getColumnConstraints.clear()
    view.getRowConstraints.clear()

    _setupGridConstraints()

    // (Cell Def Index, Col, Row)
    // 0:Pitch, 1:V.Rate, 2:V.Depth, 3:P/Z(Unused), 4:Vol, 5:T.Rate, 6:T.Depth,
    // 7:Filt, 8:Out(Unused), 9:G.Sil, 10:G.Dur, 11:Bode(Unused)

    _createAndAddCell(0, 0, 0) // Pitch
    _createAndAddCell(1, 0, 1) // V.Rate
    _createAndAddCell(2, 0, 2) // V.Depth

    _createAndAddCell(4, 1, 0) // Volume
    _createAndAddCell(5, 1, 1) // T.Rate
    _createAndAddCell(6, 1, 2) // T.Depth

    _createAndAddCell(7, 2, 0) // Filter
    _createAndAddCell(9, 2, 1) // G.Sil
    _createAndAddCell(10, 2, 2) // G.Dur

    _cells(8) = _factory.createCell(8)
    view.add(_cells(8), 0, 3, 3, 1)

    val filterCell = JagCellPane("FILTER DISPLAY")
    filterCell.setFeatures(false, false)
    filterCell.setShowZoomButtons(false)
    val fWrapper = filterCell.getCanvas.getParent.asInstanceOf[Pane]
    filterCell.getCanvas.setVisible(false)
    fWrapper.getChildren.clear()
    fWrapper.getChildren.add(_filterDisplay)

    _poleZeroCanvas.widthProperty.bind(fWrapper.widthProperty)
    _poleZeroCanvas.heightProperty.bind(fWrapper.heightProperty.divide(2))
    _freqResponseCanvas.widthProperty.bind(fWrapper.widthProperty)
    _freqResponseCanvas.heightProperty.bind(fWrapper.heightProperty.divide(2))

    view.add(filterCell, 3, 0, 1, 4)

    // Editor Overlay
    view.add(_editor.getView, 0, 0, 4, 4)
    _editor.canvas.widthProperty.bind(view.widthProperty.subtract(20))
    _editor.canvas.heightProperty.bind(view.heightProperty.subtract(60))

    _updateSelection()

  private def _setupGridConstraints(): Unit =
    val colConstraint = new ColumnConstraints()
    colConstraint.setPercentWidth(25)
    for _ <- 0 until 4 do view.getColumnConstraints.add(colConstraint)

    val rowConstraint = new RowConstraints()
    rowConstraint.setVgrow(Priority.ALWAYS)
    for _ <- 0 until 4 do view.getRowConstraints.add(rowConstraint)

  private def _createAndAddCell(defIdx: Int, col: Int, row: Int): Unit =
    val cell = _factory.createCell(defIdx)
    _cells(defIdx) = cell
    view.add(cell, col, row)

  private def _updateSelection(): Unit =
    val selectedIdx = viewModel.selectedCellIndex.get
    _cells.zipWithIndex.foreach { case (cell, idx) =>
      if cell != null then
        val isSel = idx == selectedIdx
        if cell.selectedProperty.get != isSel then
          cell.selectedProperty.set(isSel)
    }
    if selectedIdx >= 0 && selectedIdx < _cells.length then
      _bindInspector(selectedIdx)

  private def _selectCell(idx: Int): Unit =
    viewModel.selectedCellIndex.set(idx)

  private def _bindInspector(idx: Int): Unit =
    val tone = viewModel.getActiveTone
    val cellDef = RackDefs.cellDefs(idx)
    cellDef.cellType match
      case CellType.Filter =>
        inspector.bindFilter(
          tone.filterViewMode,
          cellDef.title,
          cellDef.desc
        )
      case CellType.Envelope(getter, _) =>
        val env = getter(tone)
        inspector.bind(env, cellDef.title, cellDef.desc)
      case _ => inspector.hide()

  private def _bindActiveTone(): Unit =
    val tone = viewModel.getActiveTone
    for idx <- _cells.indices if _cells(idx) != null do
      val cellDef = RackDefs.cellDefs(idx)
      cellDef.cellType match
        case CellType.Envelope(getter, _) =>
          _cells(idx).setViewModel(getter(tone))
          if cellDef.title.startsWith("G.") then
            _cells(idx).getCanvas match
              case c: JagEnvelopeCanvas =>
                c.setGraphColor(ColorUtils.Gating)
              case null =>
        case _ => // do nothing

    _poleZeroCanvas.setViewModel(tone.filterViewMode)
    _freqResponseCanvas.setViewModel(tone.filterViewMode)

    _updateOutputWaveform()

  private def _updateOutputWaveform(): Unit =
    viewModel.getActiveTone.toModel() match
      case Some(tone) =>
        SynthesisExecutor.synthesizeTone(tone) { audio =>
          _outputWaveformCanvas.setAudioBuffer(audio)
        }
      case None =>
        _outputWaveformCanvas.clearAudio()
