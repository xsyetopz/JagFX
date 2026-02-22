package jagfx.ui.components.canvas

import jagfx.Constants.Int16
import jagfx.model.EnvelopeSegment
import jagfx.ui.viewmodel.EnvelopeViewModel
import jagfx.utils.ColorUtils.*
import jagfx.utils.DrawingUtils.*

/** Canvas rendering envelope segments with grid. */
class JagEnvelopeCanvas extends JagBaseCanvas:
  // Fields
  private var viewModel: Option[EnvelopeViewModel] = None
  private var graphColor: Int = Graph
  private var cachedPointXs: Vector[Int] = Vector.empty
  private var cachedZoomedWidth: Int = -1

  // Init: styling
  getStyleClass.add("jag-envelope-canvas")

  /** Sets graph line color. */
  def setGraphColor(color: Int): Unit =
    graphColor = color
    requestRedraw()

  /** Binds envelope view model. */
  def setViewModel(vm: EnvelopeViewModel): Unit =
    viewModel.foreach(_.removeChangeListener(() => invalidateCache()))
    viewModel = Some(vm)
    vm.addChangeListener(() =>
      invalidateCache()
      requestRedraw()
    )
    invalidateCache()
    requestRedraw()

  override protected def drawContent(
      buffer: Array[Int],
      width: Int,
      height: Int
  ): Unit =
    drawGrid(buffer, width, height)
    drawCenterLine(buffer, width, height)
    viewModel.foreach(vm =>
      drawStartEndMarkers(buffer, width, height, vm)
      drawEnvelope(buffer, width, height, vm)
    )

  private def invalidateCache(): Unit =
    cachedPointXs = Vector.empty
    cachedZoomedWidth = -1

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

  private def drawStartEndMarkers(
      buffer: Array[Int],
      width: Int,
      height: Int,
      vm: EnvelopeViewModel
  ): Unit =
    val range = Int16.Range.toDouble
    val startY = ((1.0 - vm.start.get / range) * height).toInt
    val endY = ((1.0 - vm.end.get / range) * height).toInt

    val lineSpace = 4
    for x <- 0 until width by lineSpace * 2 do
      val x2 = math.min(width, x + lineSpace)
      line(buffer, width, height, x, startY, x2, startY, GridLineFaint)
      line(buffer, width, height, x, endY, x2, endY, GridLineFaint)

  private def drawEnvelope(
      buffer: Array[Int],
      width: Int,
      height: Int,
      vm: EnvelopeViewModel
  ): Unit =
    val segments = vm.getFullSegments
    if segments.nonEmpty then
      val zoomedWidth = width * zoomLevel

      val xs =
        if cachedPointXs.length == segments.length && cachedZoomedWidth == zoomedWidth
        then cachedPointXs
        else
          val computed = computePointXs(zoomedWidth, segments)
          cachedPointXs = computed
          cachedZoomedWidth = zoomedWidth
          computed

      val range = Int16.Range.toDouble
      var prevX = xs(0) - panOffset
      var prevY = ((1.0 - segments(0).peak / range) * height).toInt

      if prevX >= 0 && prevX < width then
        fillRect(buffer, width, height, prevX - 1, prevY - 1, 3, 3, graphColor)

      for i <- 1 until segments.length do
        val x = xs(i) - panOffset
        val y = ((1.0 - segments(i).peak / range) * height).toInt

        if x >= -width && x < width * 2 && prevX >= -width && prevX < width * 2
        then
          line(buffer, width, height, prevX, prevY, x, y, graphColor)
          if x >= 0 && x < width then
            fillRect(buffer, width, height, x - 1, y - 1, 3, 3, graphColor)
        prevX = x
        prevY = y

  private def computePointXs(
      width: Int,
      segments: Vector[EnvelopeSegment]
  ): Vector[Int] =
    if segments.isEmpty then Vector.empty
    else
      var t = 0
      val pointTimes = segments.indices.map { i =>
        if i == 0 then 0
        else
          t += segments(i).duration
          t
      }.toVector

      val totalTime = pointTimes.lastOption.getOrElse(0).toDouble
      if totalTime <= 1e-3 then
        val step = width.toDouble / math.max(1, segments.length - 1)
        segments.indices.map(i => (i * step).toInt).toVector
      else
        val scale = width / totalTime
        pointTimes.map(t => (t * scale).toInt)

object JagEnvelopeCanvas:
  /** Creates envelope canvas. */
  def apply(): JagEnvelopeCanvas = new JagEnvelopeCanvas()
