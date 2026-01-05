package jagfx.ui.components.canvas

import jagfx.ui.viewmodel.EnvelopeViewModel
import jagfx.utils.ColorUtils._
import jagfx.utils.DrawingUtils._
import jagfx.constants.Int16

/** Canvas rendering envelope segments with grid. */
class JagEnvelopeCanvas extends JagBaseCanvas:
  private var _viewModel: Option[EnvelopeViewModel] = None

  getStyleClass.add("jag-envelope-canvas")

  private var _graphColor: Int = Graph

  def setGraphColor(color: Int): Unit =
    _graphColor = color
    requestRedraw()

  def setViewModel(vm: EnvelopeViewModel): Unit =
    _viewModel = Some(vm)
    vm.addChangeListener(() =>
      javafx.application.Platform.runLater(() => requestRedraw())
    )
    requestRedraw()

  override protected def drawContent(buffer: Array[Int], w: Int, h: Int): Unit =
    _drawGrid(buffer, w, h)
    drawCenterLine(buffer, w, h)
    _viewModel.foreach(vm => _drawEnvelope(buffer, w, h, vm))

  private def _drawGrid(buffer: Array[Int], w: Int, h: Int): Unit =
    _drawVerticalGrid(buffer, w, h)
    _drawHorizontalGrid(buffer, w, h)

  private def _drawVerticalGrid(buffer: Array[Int], w: Int, h: Int): Unit =
    val zoomedWidth = w * zoomLevel
    val majorCols = 8
    val majorWidth = zoomedWidth / majorCols

    if zoomLevel > 1 then
      val minorCols = majorCols * zoomLevel
      val minorWidth = zoomedWidth / minorCols
      for i <- 1 until minorCols do
        if i % zoomLevel != 0 then // skip positions WHERE major lines
          val x = (i * minorWidth) - panOffset
          if x >= 0 && x < w then line(buffer, w, h, x, 0, x, h, GridLineMinor)

    for i <- 1 until majorCols do
      val x = (i * majorWidth) - panOffset
      if x >= 0 && x < w then line(buffer, w, h, x, 0, x, h, GridLineFaint)

  private def _drawHorizontalGrid(buffer: Array[Int], w: Int, h: Int): Unit =
    val rows = 4
    for i <- 1 until rows do
      val y = i * h / rows
      line(buffer, w, h, 0, y, w, y, GridLineFaint)

  private def _drawEnvelope(
      buffer: Array[Int],
      w: Int,
      h: Int,
      vm: EnvelopeViewModel
  ): Unit =
    val segments = vm.getSegments
    if segments.nonEmpty then
      val zoomedWidth = w * zoomLevel
      val step = zoomedWidth.toDouble / math.max(1, segments.length - 1)

      // Y calc: 0 at bottom, 1 at top, 0.5 at center
      var prevX = 0 - panOffset
      val range = Int16.Range.toDouble
      var prevY = ((1.0 - segments(0) / range) * h).toInt
      if prevX >= 0 && prevX < w then
        fillRect(buffer, w, h, prevX - 1, prevY - 1, 3, 3, _graphColor)

      for i <- 1 until segments.length do
        val x = (i * step).toInt - panOffset
        val y = ((1.0 - segments(i) / range) * h).toInt
        // only draw if visible
        if x >= -w && x < w * 2 && prevX >= -w && prevX < w * 2 then
          line(buffer, w, h, prevX, prevY, x, y, _graphColor)
          if x >= 0 && x < w then
            fillRect(buffer, w, h, x - 1, y - 1, 3, 3, _graphColor)
        prevX = x
        prevY = y

object JagEnvelopeCanvas:
  def apply(): JagEnvelopeCanvas = new JagEnvelopeCanvas()
