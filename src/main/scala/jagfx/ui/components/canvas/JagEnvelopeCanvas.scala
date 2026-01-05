package jagfx.ui.components.canvas

import jagfx.constants.Int16
import jagfx.ui.viewmodel.EnvelopeViewModel
import jagfx.utils.ColorUtils.*
import jagfx.utils.DrawingUtils.*

/** Canvas rendering envelope segments with grid. */
class JagEnvelopeCanvas extends JagBaseCanvas:
  // Fields
  private var viewModel: Option[EnvelopeViewModel] = None
  private var graphColor: Int = Graph

  // Init: styling
  getStyleClass.add("jag-envelope-canvas")

  /** Sets graph line color. */
  def setGraphColor(color: Int): Unit =
    graphColor = color
    requestRedraw()

  /** Binds envelope view model. */
  def setViewModel(vm: EnvelopeViewModel): Unit =
    viewModel = Some(vm)
    vm.addChangeListener(() =>
      javafx.application.Platform.runLater(() => requestRedraw())
    )
    requestRedraw()

  override protected def drawContent(
      buffer: Array[Int],
      width: Int,
      height: Int
  ): Unit =
    drawGrid(buffer, width, height)
    drawCenterLine(buffer, width, height)
    viewModel.foreach(vm => drawEnvelope(buffer, width, height, vm))

  private def drawGrid(buffer: Array[Int], width: Int, height: Int): Unit =
    drawVerticalGrid(buffer, width, height)
    drawHorizontalGrid(buffer, width, height)

  private def drawVerticalGrid(
      buffer: Array[Int],
      width: Int,
      height: Int
  ): Unit =
    val zoomedWidth = width * zoomLevel
    val majorCols = 8
    val majorWidth = zoomedWidth / majorCols

    if zoomLevel > 1 then
      val minorCols = majorCols * zoomLevel
      val minorWidth = zoomedWidth / minorCols
      for i <- 1 until minorCols do
        if i % zoomLevel != 0 then
          val x = (i * minorWidth) - panOffset
          if x >= 0 && x < width then
            line(buffer, width, height, x, 0, x, height, GridLineMinor)

    for i <- 1 until majorCols do
      val x = (i * majorWidth) - panOffset
      if x >= 0 && x < width then
        line(buffer, width, height, x, 0, x, height, GridLineFaint)

  private def drawHorizontalGrid(
      buffer: Array[Int],
      width: Int,
      height: Int
  ): Unit =
    val rows = 4
    for i <- 1 until rows do
      val y = i * height / rows
      line(buffer, width, height, 0, y, width, y, GridLineFaint)

  private def drawEnvelope(
      buffer: Array[Int],
      width: Int,
      height: Int,
      vm: EnvelopeViewModel
  ): Unit =
    val segments = vm.getSegments
    if segments.nonEmpty then
      val zoomedWidth = width * zoomLevel
      val step = zoomedWidth.toDouble / math.max(1, segments.length - 1)
      var prevX = 0 - panOffset
      val range = Int16.Range.toDouble
      var prevY = ((1.0 - segments(0) / range) * height).toInt
      if prevX >= 0 && prevX < width then
        fillRect(buffer, width, height, prevX - 1, prevY - 1, 3, 3, graphColor)

      for i <- 1 until segments.length do
        val x = (i * step).toInt - panOffset
        val y = ((1.0 - segments(i) / range) * height).toInt
        if x >= -width && x < width * 2 && prevX >= -width && prevX < width * 2
        then
          line(buffer, width, height, prevX, prevY, x, y, graphColor)
          if x >= 0 && x < width then
            fillRect(buffer, width, height, x - 1, y - 1, 3, 3, graphColor)
        prevX = x
        prevY = y

object JagEnvelopeCanvas:
  /** Creates envelope canvas. */
  def apply(): JagEnvelopeCanvas = new JagEnvelopeCanvas()
