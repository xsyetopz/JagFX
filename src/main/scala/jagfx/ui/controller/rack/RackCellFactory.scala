package jagfx.ui.controller.rack

import jagfx.ui.components.canvas.*
import jagfx.ui.components.pane.JagCellPane
import javafx.scene.layout.*

/** Factory for creating rack cells with appropriate canvas bindings. */
class RackCellFactory(
    poleZeroCanvas: JagPoleZeroCanvas,
    freqResponseCanvas: JagFrequencyResponseCanvas,
    outputWaveformCanvas: JagWaveformCanvas,
    onSelect: Int => Unit,
    onMaximize: Int => Unit
):
  /** Creates cell for given definition index. */
  def createCell(defIdx: Int): JagCellPane =
    val defCell = RackDefs.cellDefs(defIdx)
    val cell = JagCellPane(defCell.title)

    if !defCell.enabled then cell.setDisable(true)
    else cell.setOnMouseClicked(_ => onSelect(defIdx))

    defCell.cellType match
      case CellType.Filter         => configureFilterCell(cell, defIdx)
      case CellType.Output         => configureOutputCell(cell)
      case CellType.Envelope(_, _) =>
        cell.setOnMaximizeToggle(() => onMaximize(defIdx))
      case null =>

    GridPane.setHgrow(cell, Priority.ALWAYS)
    GridPane.setVgrow(cell, Priority.ALWAYS)
    cell

  private def configureFilterCell(cell: JagCellPane, idx: Int): Unit =
    cell.setFeatures(false)
    val container = cell.getChildren.get(0).asInstanceOf[VBox]
    val wrapper = container.getChildren.get(1).asInstanceOf[Pane]
    cell.getCanvas.setVisible(false)
    val canvas = if idx == 3 then poleZeroCanvas else freqResponseCanvas

    if !wrapper.getChildren.contains(canvas) then
      wrapper.getChildren.add(canvas)
      canvas.widthProperty.bind(wrapper.widthProperty)
      canvas.heightProperty.bind(wrapper.heightProperty)
    cell.setAlternateCanvas(canvas)

  private def configureOutputCell(cell: JagCellPane): Unit =
    val container = cell.getChildren.get(0).asInstanceOf[VBox]
    val wrapper = container.getChildren.get(1).asInstanceOf[Pane]
    cell.getCanvas.setVisible(false)
    if !wrapper.getChildren.contains(outputWaveformCanvas) then
      wrapper.getChildren.add(outputWaveformCanvas)
      outputWaveformCanvas.widthProperty.bind(wrapper.widthProperty)
      outputWaveformCanvas.heightProperty.bind(wrapper.heightProperty)
    cell.setAlternateCanvas(outputWaveformCanvas)
