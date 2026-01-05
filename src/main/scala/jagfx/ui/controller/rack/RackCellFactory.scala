package jagfx.ui.controller.rack

import javafx.scene.layout._
import jagfx.ui.components.pane.JagCellPane
import jagfx.ui.components.canvas._
import jagfx.ui.viewmodel.SynthViewModel

class RackCellFactory(
    viewModel: SynthViewModel,
    poleZeroCanvas: JagPoleZeroCanvas,
    freqResponseCanvas: JagFrequencyResponseCanvas,
    outputWaveformCanvas: JagWaveformCanvas,
    onSelect: Int => Unit,
    onMaximize: Int => Unit
):
  def createCell(defIdx: Int): JagCellPane =
    val defCell = RackDefs.cellDefs(defIdx)
    val cell = JagCellPane(defCell.title)

    if !defCell.enabled then cell.setDisable(true)
    else cell.setOnMouseClicked(_ => onSelect(defIdx))

    defCell.cellType match
      case CellType.Filter         => _configureFilterCell(cell, defIdx)
      case CellType.Output         => _configureOutputCell(cell)
      case CellType.Envelope(_, _) =>
        cell.setOnMaximizeToggle(() => onMaximize(defIdx))
      case null => // no-op

    GridPane.setHgrow(cell, Priority.ALWAYS)
    GridPane.setVgrow(cell, Priority.ALWAYS)
    cell

  private def _configureFilterCell(cell: JagCellPane, idx: Int): Unit =
    cell.setFeatures(false, false)
    val container = cell.getChildren.get(0).asInstanceOf[VBox]
    val wrapper = container.getChildren.get(1).asInstanceOf[Pane]
    cell.getCanvas.setVisible(false)
    val canvas = if idx == 3 then poleZeroCanvas else freqResponseCanvas

    if !wrapper.getChildren.contains(canvas) then
      wrapper.getChildren.add(canvas)
      canvas.widthProperty.bind(wrapper.widthProperty)
      canvas.heightProperty.bind(wrapper.heightProperty)
    cell.setAlternateCanvas(canvas)

  private def _configureOutputCell(cell: JagCellPane): Unit =
    val container = cell.getChildren.get(0).asInstanceOf[VBox]
    val wrapper = container.getChildren.get(1).asInstanceOf[Pane]
    cell.getCanvas.setVisible(false)
    if !wrapper.getChildren.contains(outputWaveformCanvas) then
      wrapper.getChildren.add(outputWaveformCanvas)
      outputWaveformCanvas.widthProperty.bind(wrapper.widthProperty)
      outputWaveformCanvas.heightProperty.bind(wrapper.heightProperty)
    cell.setAlternateCanvas(outputWaveformCanvas)
