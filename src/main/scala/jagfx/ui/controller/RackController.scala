package jagfx.ui.controller

import javafx.scene.layout._
import jagfx.ui.viewmodel._
import jagfx.ui.components._
import jagfx.synth.ToneSynthesizer
import javafx.beans.value.ChangeListener

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
      "VIBRATO RATE",
      "Modulates speed of vibrato (FM). Higher values create faster pitch wobbling.",
      CellType.Envelope(_.vibratoRate)
    ),
    RackCellDef(
      "VIBRATO DEPTH",
      "Controls intensity of vibrato. Higher values cause wider pitch variations.",
      CellType.Envelope(_.vibratoDepth)
    ),
    RackCellDef(
      "FILTER POLES/ZEROS",
      "Visualizes IIR filter poles/zeros. Drag points to shape frequency response.",
      CellType.Filter
    ),
    RackCellDef(
      "VOLUME",
      "Shapes overall loudness over time. Use to create attacks, decays, and swells.",
      CellType.Envelope(_.volume)
    ),
    RackCellDef(
      "TREMOLO RATE",
      "Modulates speed of tremolo (AM). Higher values create faster volume fluctuations.",
      CellType.Envelope(_.tremoloRate)
    ),
    RackCellDef(
      "TREMOLO DEPTH",
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
      "GATE SILENCE",
      "Sets initial delay (silence) before note begins playing.",
      CellType.Envelope(_.gateSilence)
    ),
    RackCellDef(
      "GATE DURATION",
      "Determines total length of active note segment in samples.",
      CellType.Envelope(_.gateDuration)
    ),
    RackCellDef(
      "FILTER RESPONSE",
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

  viewModel.rackMode.addListener((_, _, _) => buildGrid())
  viewModel.selectedCellIndex.addListener((_, _, _) => updateSelection())
  buildGrid()

  def bind(): Unit =
    bindActiveTone()

  private val activeToneChangeListener: ChangeListener[Number] = (_, _, _) =>
    bindActiveTone()
  viewModel.activeToneIndexProperty.addListener(activeToneChangeListener)
  viewModel.fileLoadedProperty.addListener((_, _, _) => bindActiveTone())

  for i <- 0 until viewModel.getTones.size do
    viewModel.getTones.get(i).addChangeListener(() => updateOutputWaveform())

  private def buildGrid(): Unit =
    view.getChildren.clear()
    view.getColumnConstraints.clear()

    val mode = viewModel.rackMode.get
    val indices = mode match
      case RackMode.Main   => Vector(0, 1, 2, 4, 5, 6, 8, 9, 10)
      case RackMode.Filter => Vector(3, 7, 11)
      case RackMode.Both   => (0 to 11).toVector

    // Main/Filter use 3-col grid, Both uses 4-col
    val cols = if mode == RackMode.Both then 4 else 3

    val constraint = new ColumnConstraints()
    constraint.setPercentWidth(100.0 / cols)
    for _ <- 0 until cols do view.getColumnConstraints.add(constraint)

    indices.zipWithIndex.foreach { case (cellIdx, i) =>
      val cell = cells(cellIdx)
      val col = i % cols
      val row = i / cols

      view.add(cell, col, row)
    }
    updateSelection()

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

  /** Returns envelope for cell index, or `None` for disabled cells. */
  private def bindInspector(idx: Int): Unit =
    val tone = viewModel.getActiveTone
    val cellDef = definitions(idx)
    cellDef.cellType match
      case CellType.Filter =>
        inspector.bindFilter(
          tone.filterViewModel,
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
        case _ => // do nothing for other types

    poleZeroCanvas.setViewModel(tone.filterViewModel)
    freqResponseCanvas.setViewModel(tone.filterViewModel)

    updateOutputWaveform()

  private def updateOutputWaveform(): Unit =
    javafx.application.Platform.runLater(() =>
      val toneVm = viewModel.getActiveTone
      toneVm.toModel() match
        case Some(tone) =>
          val audio = ToneSynthesizer.synthesize(tone)
          outputWaveformCanvas.setAudioBuffer(audio)
        case None =>
          outputWaveformCanvas.clearAudio()
    )

  /** Set playhead position (`0.0` to `1.0`) on output waveform. */
  def setPlayheadPosition(position: Double): Unit =
    outputWaveformCanvas.setPlayheadPosition(position)

  /** Hide playhead on output waveform. */
  def hidePlayhead(): Unit =
    outputWaveformCanvas.hidePlayhead()
