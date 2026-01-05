package jagfx.ui.components.canvas

import javafx.scene.input.MouseEvent
import jagfx.ui.viewmodel.EnvelopeViewModel
import jagfx.utils.ColorUtils._
import jagfx.utils.DrawingUtils._
import jagfx.constants.Int16

private val _PointRadius = 5
private val _HitRadius = 10

/** Interactive canvas for envelope editing with draggable control points.
  */
class JagEnvelopeEditorCanvas extends JagBaseCanvas:
  private var _viewModel: Option[EnvelopeViewModel] = None
  private var _hoveredPointIndex: Int = -1
  private var _selectedPointIndex: Int = -1
  private var _dragging: Boolean = false
  private var _dragStartY: Double = 0

  getStyleClass.add("jag-envelope-editor-canvas")

  def setViewModel(vm: EnvelopeViewModel): Unit =
    _viewModel = Some(vm)
    vm.addChangeListener(() =>
      javafx.application.Platform.runLater(() => requestRedraw())
    )
    requestRedraw()

  def getViewModel: Option[EnvelopeViewModel] = _viewModel

  override protected def drawContent(buffer: Array[Int], w: Int, h: Int): Unit =
    _drawGrid(buffer, w, h)
    drawCenterLine(buffer, w, h)
    _viewModel.foreach(vm => _drawEnvelope(buffer, w, h, vm))
    _viewModel.foreach(vm => _drawControlPoints(buffer, w, h, vm))

  private def _drawGrid(buffer: Array[Int], w: Int, h: Int): Unit =
    val majorCols = 8
    for i <- 1 until majorCols do
      val x = i * w / majorCols
      line(buffer, w, h, x, 0, x, h, GridLineFaint)
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
      val step = w.toDouble / math.max(1, segments.length - 1)
      val range = Int16.Range.toDouble

      var prevX = 0
      var prevY = ((1.0 - segments(0) / range) * h).toInt

      for i <- 1 until segments.length do
        val x = (i * step).toInt
        val y = ((1.0 - segments(i) / range) * h).toInt
        line(buffer, w, h, prevX, prevY, x, y, Graph)
        prevX = x
        prevY = y

  private def _drawControlPoints(
      buffer: Array[Int],
      w: Int,
      h: Int,
      vm: EnvelopeViewModel
  ): Unit =
    val segments = vm.getSegments
    if segments.isEmpty then return

    val step = w.toDouble / math.max(1, segments.length - 1)
    val range = Int16.Range.toDouble

    for i <- segments.indices do
      val x = (i * step).toInt
      val y = ((1.0 - segments(i) / range) * h).toInt

      val color =
        if i == _selectedPointIndex then PointSelected
        else if i == _hoveredPointIndex then PointHover
        else PointNormal

      _fillCircle(buffer, w, h, x, y, _PointRadius, color)
      _drawCircle(buffer, w, h, x, y, _PointRadius, White)

  private def _fillCircle(
      buffer: Array[Int],
      w: Int,
      h: Int,
      cx: Int,
      cy: Int,
      r: Int,
      color: Int
  ): Unit =
    for dy <- -r to r do
      for dx <- -r to r do
        if dx * dx + dy * dy <= r * r then
          val px = cx + dx
          val py = cy + dy
          if px >= 0 && px < w && py >= 0 && py < h then
            buffer(py * w + px) = color

  private def _drawCircle(
      buffer: Array[Int],
      w: Int,
      h: Int,
      cx: Int,
      cy: Int,
      r: Int,
      color: Int
  ): Unit =
    var x = r
    var y = 0
    var err = 0
    while x >= y do
      _setPixel(buffer, w, h, cx + x, cy + y, color)
      _setPixel(buffer, w, h, cx + y, cy + x, color)
      _setPixel(buffer, w, h, cx - y, cy + x, color)
      _setPixel(buffer, w, h, cx - x, cy + y, color)
      _setPixel(buffer, w, h, cx - x, cy - y, color)
      _setPixel(buffer, w, h, cx - y, cy - x, color)
      _setPixel(buffer, w, h, cx + y, cy - x, color)
      _setPixel(buffer, w, h, cx + x, cy - y, color)
      y += 1
      err += 1 + 2 * y
      if 2 * (err - x) + 1 > 0 then
        x -= 1
        err += 1 - 2 * x

  private def _setPixel(
      buffer: Array[Int],
      w: Int,
      h: Int,
      x: Int,
      y: Int,
      color: Int
  ): Unit =
    if x >= 0 && x < w && y >= 0 && y < h then buffer(y * w + x) = color

  private def _findPointAt(mx: Double, my: Double): Int =
    _viewModel match
      case None     => -1
      case Some(vm) =>
        val segments = vm.getSegments
        if segments.isEmpty then -1
        else
          val w = getWidth
          val h = getHeight
          val step = w / math.max(1, segments.length - 1)
          val range = Int16.Range.toDouble

          segments.indices.indexWhere { i =>
            val x = i * step
            val y = (1.0 - segments(i) / range) * h
            val dist = math.sqrt((mx - x) * (mx - x) + (my - y) * (my - y))
            dist <= _HitRadius
          }

  setOnMouseMoved((e: MouseEvent) =>
    val newHover = _findPointAt(e.getX, e.getY)
    if newHover != _hoveredPointIndex then
      _hoveredPointIndex = newHover
      requestRedraw()
  )

  setOnMousePressed((e: MouseEvent) =>
    val pointIdx = _findPointAt(e.getX, e.getY)
    if pointIdx >= 0 then
      _selectedPointIndex = pointIdx
      _dragging = true
      _dragStartY = e.getY
      requestRedraw()
      e.consume()
  )

  setOnMouseDragged((e: MouseEvent) =>
    if _dragging && _selectedPointIndex >= 0 then
      _viewModel.foreach { vm =>
        val h = getHeight
        val range = Int16.Range.toDouble

        val normalizedY = 1.0 - (e.getY / h)
        val newPeak = (normalizedY * range).toInt.max(0).min(Int16.Range - 1)

        val fullSegments = vm.getFullSegments
        if _selectedPointIndex < fullSegments.length then
          val seg = fullSegments(_selectedPointIndex)
          vm.updateSegment(_selectedPointIndex, seg.duration, newPeak)

        requestRedraw()
      }
      e.consume()
  )

  setOnMouseReleased((e: MouseEvent) => _dragging = false)

  setOnMouseExited((e: MouseEvent) =>
    if !_dragging then
      _hoveredPointIndex = -1
      requestRedraw()
  )

object JagEnvelopeEditorCanvas:
  def apply(): JagEnvelopeEditorCanvas = new JagEnvelopeEditorCanvas()
