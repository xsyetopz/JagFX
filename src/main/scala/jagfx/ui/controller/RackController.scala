package jagfx.ui.controller

import javafx.scene.layout._
import jagfx.ui.viewmodel._
import jagfx.ui.components.canvas._
import jagfx.ui.components.pane._
import jagfx.ui.components.slider._
import jagfx.ui.components.field._
import jagfx.ui.components.button._
import jagfx.ui.components.group._
import jagfx.synth.SynthesisExecutor
import javafx.beans.value.ChangeListener
import jagfx.ui.BindingManager
import jagfx.ui.controller.inspector.InspectorController

class RackController(viewModel: SynthViewModel, inspector: InspectorController)
    extends IController[GridPane]:
  protected val view = GridPane()
  view.getStyleClass.add("rack")
  view.setHgap(1)
  view.setVgap(1)

  private val cells = new Array[JagCellPane](12)

  private enum CellType:
    case Envelope(
        getter: ToneViewModel => EnvelopeViewModel,
        inspectorMode: Boolean = true
    )
    case Filter
    case Output

  private case class RackCellDef(
      title: String,
      desc: String,
      cellType: CellType,
      enabled: Boolean = true
  )

  private val definitions = Vector(
    RackCellDef(
      "PITCH",
      "Defines base pitch trajectory. Envelope values are added to fundamental frequency over time.",
      CellType.Envelope(_.pitch)
    ),
    RackCellDef(
      "V.RATE",
      "Modulates speed of vibrato (FM). Higher values create faster pitch wobbling.",
      CellType.Envelope(_.vibratoRate)
    ),
    RackCellDef(
      "V.DEPTH",
      "Controls intensity of vibrato. Higher values cause wider pitch variations.",
      CellType.Envelope(_.vibratoDepth)
    ),
    RackCellDef(
      "P/Z",
      "Visualizes IIR filter poles/zeros. Drag points to shape frequency response.",
      CellType.Filter
    ),
    RackCellDef(
      "VOLUME",
      "Shapes overall loudness over time. Use to create attacks, decays, and swells.",
      CellType.Envelope(_.volume)
    ),
    RackCellDef(
      "T.RATE",
      "Modulates speed of tremolo (AM). Higher values create faster volume fluctuations.",
      CellType.Envelope(_.tremoloRate)
    ),
    RackCellDef(
      "T.DEPTH",
      "Controls intensity of tremolo. Higher values create deeper volume cuts.",
      CellType.Envelope(_.tremoloDepth)
    ),
    RackCellDef(
      "FILTER",
      "Interpolates between initial/final filter states. 0 = Start Filter, 1 = End Filter.",
      CellType.Envelope(_.filterEnvelope)
    ),
    RackCellDef(
      "OUTPUT",
      "Real-time visualization of synthesized waveform for active tone.",
      CellType.Output
    ),
    RackCellDef(
      "G.SIL",
      "Sets initial delay (silence) before note begins playing.",
      CellType.Envelope(_.gateSilence)
    ),
    RackCellDef(
      "G.DUR",
      "Determines total length of active note segment in samples.",
      CellType.Envelope(_.gateDuration)
    ),
    RackCellDef(
      "BODE",
      "Displays frequency magnitude response (Bode plot) of active filter.",
      CellType.Filter
    )
  )

  private val outputWaveformCanvas = JagWaveformCanvas()
  outputWaveformCanvas.setZoom(4)

  private val poleZeroCanvas = JagPoleZeroCanvas()
  private val freqResponseCanvas = JagFrequencyResponseCanvas()

  definitions.zipWithIndex.foreach { case (defCell, idx) =>
    val cell = JagCellPane(defCell.title)
    if !defCell.enabled then cell.setDisable(true)
    else cell.setOnMouseClicked(_ => selectCell(idx))

    defCell.cellType match
      case CellType.Filter =>
        cell.setFeatures(false, false)
        val container = cell.getChildren.get(0).asInstanceOf[VBox]
        val wrapper = container.getChildren.get(1).asInstanceOf[Pane]
        cell.getCanvas.setVisible(false)
        val canvas =
          if idx == 3 then poleZeroCanvas else freqResponseCanvas
        if !wrapper.getChildren.contains(canvas) then
          wrapper.getChildren.add(canvas)
          canvas.widthProperty.bind(wrapper.widthProperty)
          canvas.heightProperty.bind(wrapper.heightProperty)
        cell.setAlternateCanvas(canvas)

      case CellType.Output =>
        val container = cell.getChildren.get(0).asInstanceOf[VBox]
        val wrapper = container.getChildren.get(1).asInstanceOf[Pane]
        cell.getCanvas.setVisible(false)
        if !wrapper.getChildren.contains(outputWaveformCanvas) then
          wrapper.getChildren.add(outputWaveformCanvas)
          outputWaveformCanvas.widthProperty.bind(wrapper.widthProperty)
          outputWaveformCanvas.heightProperty.bind(wrapper.heightProperty)
        cell.setAlternateCanvas(outputWaveformCanvas)

      case _ => // standard envelope cell

    GridPane.setHgrow(cell, Priority.ALWAYS)
    GridPane.setVgrow(cell, Priority.ALWAYS)
    cells(idx) = cell
  }

  private val filterDisplay = new VBox(2):
    getChildren.addAll(poleZeroCanvas, freqResponseCanvas)
    VBox.setVgrow(freqResponseCanvas, Priority.ALWAYS)

  /*
      PITCH    | VOLUME   | FILTER   | [FILTER DISPLAY]
      V.RATE   | T.RATE   | G.SIL    | [FILTER DISPLAY]
      V.DEPTH  | T.DEPTH  | G.DUR    | [FILTER DISPLAY]
        [ OUTPUT WAVEFORM SPAN 3 ]   | [FILTER DISPLAY]
   */

  private val bindingManager = BindingManager()

  buildGrid()

  def bind(): Unit =
    bindActiveTone()

  bindingManager.listen(viewModel.activeToneIndexProperty)(_ =>
    bindActiveTone()
  )
  bindingManager.listen(viewModel.fileLoadedProperty)(_ => bindActiveTone())
  bindingManager.listen(viewModel.selectedCellIndex)(_ => updateSelection())

  for i <- 0 until viewModel.getTones.size do
    viewModel.getTones.get(i).addChangeListener(() => updateOutputWaveform())

  /** Set playhead position (`0.0` to `1.0`) on output waveform. */
  def setPlayheadPosition(position: Double): Unit =
    outputWaveformCanvas.setPlayheadPosition(position)

  /** Hide playhead on output waveform. */
  def hidePlayhead(): Unit =
    outputWaveformCanvas.hidePlayhead()

  private def buildGrid(): Unit =
    view.getChildren.clear()
    view.getColumnConstraints.clear()
    view.getRowConstraints.clear()

    val colConstraint = new ColumnConstraints()
    colConstraint.setPercentWidth(25)
    for _ <- 0 until 4 do view.getColumnConstraints.add(colConstraint)

    val rowConstraint = new RowConstraints()
    rowConstraint.setVgrow(Priority.ALWAYS)
    for _ <- 0 until 4 do view.getRowConstraints.add(rowConstraint)

    // (Cell Def Index, Col, Row)
    // 0:Pitch, 1:V.Rate, 2:V.Depth, 3:P/Z(Unused), 4:Vol, 5:T.Rate, 6:T.Depth,
    // 7:Filt, 8:Out(Unused), 9:G.Sil, 10:G.Dur, 11:Bode(Unused)

    addCell(0, 0, 0) // Pitch
    addCell(1, 0, 1) // V.Rate
    addCell(2, 0, 2) // V.Depth

    addCell(4, 1, 0) // Volume
    addCell(5, 1, 1) // T.Rate
    addCell(6, 1, 2) // T.Depth

    addCell(7, 2, 0) // Filter
    addCell(9, 2, 1) // G.Sil
    addCell(10, 2, 2) // G.Dur

    val outputCell = cells(8)
    view.add(outputCell, 0, 3, 3, 1) // Col 0, Row 3, Span 3x1

    val filterCell = JagCellPane("FILTER DISPLAY")
    filterCell.setFeatures(false, false)
    filterCell.setShowZoomButtons(false)
    val fWrapper = filterCell.getCanvas.getParent.asInstanceOf[Pane]
    filterCell.getCanvas.setVisible(false)
    fWrapper.getChildren.clear() // remove canvas
    fWrapper.getChildren.add(filterDisplay)

    poleZeroCanvas.widthProperty.bind(fWrapper.widthProperty)
    poleZeroCanvas.heightProperty.bind(fWrapper.heightProperty.divide(2))
    freqResponseCanvas.widthProperty.bind(fWrapper.widthProperty)
    freqResponseCanvas.heightProperty.bind(fWrapper.heightProperty.divide(2))

    view.add(filterCell, 3, 0, 1, 4)

    updateSelection()

  private def addCell(defIdx: Int, col: Int, row: Int): Unit =
    view.add(cells(defIdx), col, row)

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
    val tone = viewModel.getActiveTone
    val cellDef = definitions(idx)
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

  private def bindActiveTone(): Unit =
    val tone = viewModel.getActiveTone
    for idx <- cells.indices if cells(idx) != null do
      val cellDef = definitions(idx)
      cellDef.cellType match
        case CellType.Envelope(getter, _) =>
          cells(idx).setViewModel(getter(tone))
          if cellDef.title.startsWith("G.") then
            cells(idx).getCanvas match
              case c: JagEnvelopeCanvas =>
                c.setGraphColor(jagfx.utils.ColorUtils.Gating)
              case null =>
        case _ => // do nothing

    poleZeroCanvas.setViewModel(tone.filterViewMode)
    freqResponseCanvas.setViewModel(tone.filterViewMode)

    updateOutputWaveform()

  private def updateOutputWaveform(): Unit =
    viewModel.getActiveTone.toModel() match
      case Some(tone) =>
        SynthesisExecutor.synthesizeTone(tone) { audio =>
          outputWaveformCanvas.setAudioBuffer(audio)
        }
      case None =>
        outputWaveformCanvas.clearAudio()
