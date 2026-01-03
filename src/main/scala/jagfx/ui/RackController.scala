package jagfx.ui

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

  private val definitions = Vector(
    ("PITCH", true), // 0
    ("VIB RATE", true), // 1
    ("VIB DEPTH", true), // 2
    ("FILT POLE", true), // 3
    ("VOLUME", true), // 4
    ("TREM RATE", true), // 5
    ("TREM DEPTH", true), // 6
    ("TRANSITION", true), // 7
    ("OUTPUT", true), // 8
    ("SILENCE", true), // 9
    ("DURATION", true), // 10
    ("RESPONSE", true) // 11
  )

  private val outputWaveformCanvas = JagWaveformCanvas()
  outputWaveformCanvas.setZoom(4)

  private val poleZeroCanvas = JagPoleZeroCanvas()
  private val freqResponseCanvas = JagFrequencyResponseCanvas()

  definitions.zipWithIndex.foreach { case ((title, enabled), idx) =>
    val cell = JagCellPane(title)
    if !enabled then cell.setDisable(true)
    else cell.setOnMouseClicked(_ => selectCell(idx))

    if Set(3, 11).contains(idx) then cell.setFeatures(false, false)

    GridPane.setHgrow(cell, Priority.ALWAYS)
    GridPane.setVgrow(cell, Priority.ALWAYS)
    cells(idx) = cell
  }

  private def setupOutputCell(): Unit =
    val outputCell = cells(8)
    val container =
      outputCell.getChildren.get(0).asInstanceOf[VBox]
    val canvasWrapper =
      container.getChildren.get(1).asInstanceOf[Pane]

    outputCell.getCanvas.setVisible(false)
    if !canvasWrapper.getChildren.contains(outputWaveformCanvas) then
      canvasWrapper.getChildren.add(outputWaveformCanvas)
      outputWaveformCanvas.widthProperty.bind(canvasWrapper.widthProperty)
      outputWaveformCanvas.heightProperty.bind(canvasWrapper.heightProperty)

    outputCell.setAlternateCanvas(outputWaveformCanvas)

  private def setupFilterCells(): Unit =
    // Cell 3: FILT POLE - pole-zero diagram
    val poleCell = cells(3)
    val poleContainer = poleCell.getChildren.get(0).asInstanceOf[VBox]
    val poleWrapper = poleContainer.getChildren.get(1).asInstanceOf[Pane]
    poleCell.getCanvas.setVisible(false)
    if !poleWrapper.getChildren.contains(poleZeroCanvas) then
      poleWrapper.getChildren.add(poleZeroCanvas)
      poleZeroCanvas.widthProperty.bind(poleWrapper.widthProperty)
      poleZeroCanvas.heightProperty.bind(poleWrapper.heightProperty)
    poleCell.setAlternateCanvas(poleZeroCanvas)

    // Cell 11: RESPONSE - frequency response
    val respCell = cells(11)
    val respContainer = respCell.getChildren.get(0).asInstanceOf[VBox]
    val respWrapper = respContainer.getChildren.get(1).asInstanceOf[Pane]
    respCell.getCanvas.setVisible(false)
    if !respWrapper.getChildren.contains(freqResponseCanvas) then
      respWrapper.getChildren.add(freqResponseCanvas)
      freqResponseCanvas.widthProperty.bind(respWrapper.widthProperty)
      freqResponseCanvas.heightProperty.bind(respWrapper.heightProperty)
    respCell.setAlternateCanvas(freqResponseCanvas)

  setupOutputCell()
  setupFilterCells()

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
  private def envelopeForCell(
      tone: ToneViewModel,
      idx: Int
  ): Option[EnvelopeViewModel] =
    idx match
      case 0  => Some(tone.pitch)
      case 1  => Some(tone.vibratoRate)
      case 2  => Some(tone.vibratoDepth)
      case 4  => Some(tone.volume)
      case 5  => Some(tone.tremoloRate)
      case 6  => Some(tone.tremoloDepth)
      case 7  => Some(tone.filterEnvelope)
      case 9  => Some(tone.gateSilence)
      case 10 => Some(tone.gateDuration)
      case _  => None

  private def bindInspector(idx: Int): Unit =
    val tone = viewModel.getActiveTone
    idx match
      case 3 | 11 => inspector.bindFilter(tone.filterViewModel)
      case _      =>
        envelopeForCell(tone, idx) match
          case Some(env) => inspector.bind(env)
          case None      => inspector.hide()

  private def bindActiveTone(): Unit =
    val tone = viewModel.getActiveTone
    for idx <- cells.indices if cells(idx) != null do
      envelopeForCell(tone, idx).foreach(cells(idx).setViewModel)

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
