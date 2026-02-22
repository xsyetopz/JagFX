package jagfx.ui.components.canvas

import scala.collection.mutable.ListBuffer

import jagfx.Constants.Int16
import jagfx.model.EnvelopeSegment
import jagfx.ui.viewmodel.EnvelopeViewModel
import jagfx.utils.ColorUtils.*
import jagfx.utils.DrawingUtils.*
import jagfx.utils.MathUtils
import javafx.application.Platform
import javafx.scene.Cursor
import javafx.scene.input.MouseEvent

// Constants
private final val PointRadius = 5
private final val HitRadius = 8
private final val SegmentHitThreshold = 6

/** Interactive canvas for envelope editing with draggable control points. */
class JagEnvelopeEditorCanvas extends JagBaseCanvas:
  // Types
  private enum HoverTarget:
    case None
    case Point(index: Int)
    case Segment(index: Int)

  private object DragState:
    var primaryIndex: Int = -1
    var dragging: Boolean = false
    var lockTime: Boolean = false
    var startY: Double = 0
    var startPeaks: Map[Int, Int] = Map.empty
    var startDurations: Map[Int, Int] = Map.empty
    var startAbsTimes: Map[Int, Int] = Map.empty
    var totalTimeScale: Double = 0.0

    def capture(
        viewModel: EnvelopeViewModel,
        selection: Set[Int],
        y: Double,
        width: Double
    ): Unit =
      dragging = true
      startY = y
      val segments = viewModel.getFullSegments

      startPeaks = selection.flatMap { idx =>
        if idx < segments.length then Some(idx -> segments(idx).peak) else None
      }.toMap

      startDurations = segments.zipWithIndex.map { case (s, i) =>
        i -> s.duration
      }.toMap

      var t = 0
      val absTimes = segments.indices.map { i =>
        if i > 0 then t += segments(i).duration
        i -> t
      }.toMap

      startAbsTimes = selection.flatMap { idx =>
        absTimes.get(idx).map(idx -> _)
      }.toMap

      val totalDur =
        (1 until segments.length).map(k => segments(k).duration).sum.toDouble
      totalTimeScale = if totalDur > 0 then width / totalDur else 0.0

  private object MarqueeState:
    var isSelecting: Boolean = false
    var start: (Double, Double) = (0.0, 0.0)
    var end: (Double, Double) = (0.0, 0.0)

  // Fields
  private var viewModel: Option[EnvelopeViewModel] = None
  private var hoverTarget: HoverTarget = HoverTarget.None
  private var selection: Set[Int] = Set.empty
  private var graphColor: Int = Graph

  // Init: styling
  getStyleClass.add("jag-envelope-editor-canvas")

  /** Sets graph line color. */
  def setGraphColor(color: Int): Unit =
    graphColor = color
    requestRedraw()

  // Init: event handlers
  setOnMouseMoved((e: MouseEvent) =>
    val newHover = hitTest(e.getX, e.getY)
    if newHover != hoverTarget then
      hoverTarget = newHover
      setCursor(hoverTarget match
        case HoverTarget.Segment(_) => Cursor.V_RESIZE
        case HoverTarget.Point(_)   => Cursor.DEFAULT
        case _                      => Cursor.DEFAULT)
      requestRedraw()
  )

  setOnMousePressed((e: MouseEvent) =>
    val target = hitTest(e.getX, e.getY)
    DragState.dragging = false
    MarqueeState.isSelecting = false

    target match
      case HoverTarget.Point(idx) =>
        if e.isShortcutDown || e.isShiftDown then
          if selection.contains(idx) then selection -= idx
          else selection += idx
        else if !selection.contains(idx) then selection = Set(idx)

        DragState.primaryIndex = idx
        DragState.lockTime = false

        viewModel.foreach { vm =>
          DragState.capture(vm, selection, e.getY, getWidth)
        }

      case HoverTarget.Segment(idx) =>
        if !e.isShortcutDown && !e.isShiftDown then
          selection = Set(idx - 1, idx)
        else selection ++= Set(idx - 1, idx)

        DragState.primaryIndex = idx
        DragState.lockTime = true

        viewModel.foreach { vm =>
          DragState.capture(vm, selection, e.getY, getWidth)
        }

      case HoverTarget.None =>
        if !e.isShortcutDown && !e.isShiftDown then selection = Set.empty
        MarqueeState.isSelecting = true
        MarqueeState.start = (e.getX, e.getY)
        MarqueeState.end = (e.getX, e.getY)

    requestRedraw()
  )

  setOnMouseDragged((e: MouseEvent) =>
    if DragState.dragging then handleDrag(e)
    else if MarqueeState.isSelecting then
      MarqueeState.end = (e.getX, e.getY)
      requestRedraw()
  )

  setOnMouseReleased((e: MouseEvent) =>
    if MarqueeState.isSelecting then
      viewModel.foreach { vm =>
        val segments = vm.getFullSegments
        val range = Int16.Range.toDouble
        val height = getHeight.toInt
        val width = getWidth.toInt
        val xs = computePointXs(width, segments)

        val rx = math.min(MarqueeState.start._1, MarqueeState.end._1)
        val ry = math.min(MarqueeState.start._2, MarqueeState.end._2)
        val rw = math.abs(MarqueeState.end._1 - MarqueeState.start._1)
        val rh = math.abs(MarqueeState.end._2 - MarqueeState.start._2)

        val newSelection = segments.indices.filter { i =>
          val x = xs(i)
          val y = (1.0 - segments(i).peak / range) * height
          x >= rx && x <= rx + rw && y >= ry && y <= ry + rh
        }.toSet

        if e.isShiftDown || e.isShortcutDown then selection ++= newSelection
        else selection = newSelection
      }
      MarqueeState.isSelecting = false
      requestRedraw()
    else
      DragState.dragging = false
      setCursor(hitTest(e.getX, e.getY) match
        case HoverTarget.Segment(_) => Cursor.V_RESIZE
        case HoverTarget.Point(_)   => Cursor.DEFAULT
        case _                      => Cursor.DEFAULT)
  )

  setOnMouseExited((_: MouseEvent) =>
    if !DragState.dragging && !MarqueeState.isSelecting then
      hoverTarget = HoverTarget.None
      setCursor(Cursor.DEFAULT)
      requestRedraw()
  )

  /** Binds envelope view model. */
  def setViewModel(vm: EnvelopeViewModel): Unit =
    viewModel = Some(vm)
    vm.addChangeListener(() => Platform.runLater(() => requestRedraw()))
    requestRedraw()

  /** Returns bound view model. */
  def getViewModel: Option[EnvelopeViewModel] = viewModel

  override protected def drawContent(
      buffer: Array[Int],
      width: Int,
      height: Int
  ): Unit =
    drawGrid(buffer, width, height)
    drawCenterLine(buffer, width, height)
    viewModel.foreach { vm =>
      val segments = vm.getFullSegments
      val xs = computePointXs(width, segments)
      drawEnvelope(buffer, width, height, segments, xs)
      drawControlPoints(buffer, width, height, segments, xs)
      drawSelectionRect(buffer, width, height)
    }

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

  private def drawGrid(buffer: Array[Int], width: Int, height: Int): Unit =
    val majorCols = 8
    for i <- 1 until majorCols do
      val x = i * width / majorCols
      line(buffer, width, height, x, 0, x, height, GridLineFaint)
    val rows = 4
    for i <- 1 until rows do
      val y = i * height / rows
      line(buffer, width, height, 0, y, width, y, GridLineFaint)

  private def drawEnvelope(
      buffer: Array[Int],
      width: Int,
      height: Int,
      segments: Vector[EnvelopeSegment],
      xs: Vector[Int]
  ): Unit =
    if segments.nonEmpty && xs.length == segments.length then
      val range = Int16.Range.toDouble
      var prevX = xs(0)
      var prevY = ((1.0 - segments(0).peak / range) * height).toInt

      for i <- 1 until segments.length do
        val x = xs(i)
        val y = ((1.0 - segments(i).peak / range) * height).toInt

        val color = hoverTarget match
          case HoverTarget.Segment(idx) if idx == i => White
          case _                                    => graphColor
        if color == White then
          line(buffer, width, height, prevX, prevY - 1, x, y - 1, color)
          line(buffer, width, height, prevX, prevY + 1, x, y + 1, color)

        line(buffer, width, height, prevX, prevY, x, y, color)
        prevX = x
        prevY = y

  private def drawControlPoints(
      buffer: Array[Int],
      width: Int,
      height: Int,
      segments: Vector[EnvelopeSegment],
      xs: Vector[Int]
  ): Unit =
    if segments.nonEmpty && xs.length == segments.length then
      val range = Int16.Range.toDouble
      for i <- segments.indices do
        val x = xs(i)
        val y = ((1.0 - segments(i).peak / range) * height).toInt

        val color =
          if selection.contains(i) then PointSelected
          else
            hoverTarget match
              case HoverTarget.Point(idx) if idx == i => PointHover
              case _                                  => graphColor

        fillCircle(buffer, width, height, x, y, PointRadius, color)
        drawCircle(buffer, width, height, x, y, PointRadius, White)

  private def drawSelectionRect(
      buffer: Array[Int],
      width: Int,
      height: Int
  ): Unit =
    if MarqueeState.isSelecting then
      val rx = math.min(MarqueeState.start._1, MarqueeState.end._1).toInt
      val ry = math.min(MarqueeState.start._2, MarqueeState.end._2).toInt
      val rw = math.abs(MarqueeState.end._1 - MarqueeState.start._1).toInt
      val rh = math.abs(MarqueeState.end._2 - MarqueeState.start._2).toInt

      line(buffer, width, height, rx, ry, rx + rw, ry, White)
      line(buffer, width, height, rx, ry + rh, rx + rw, ry + rh, White)
      line(buffer, width, height, rx, ry, rx, ry + rh, White)
      line(buffer, width, height, rx + rw, ry, rx + rw, ry + rh, White)

  private def hitTest(mx: Double, my: Double): HoverTarget =
    viewModel match
      case None     => HoverTarget.None
      case Some(vm) =>
        val segments = vm.getFullSegments
        if segments.isEmpty then HoverTarget.None
        else
          val width = getWidth.toInt
          val height = getHeight.toInt
          val xs = computePointXs(width, segments)
          val range = Int16.Range.toDouble

          hitTestPoint(mx, my, segments, xs, height, range)
            .map(HoverTarget.Point(_))
            .orElse(
              hitTestSegment(mx, my, segments, xs, height, range)
                .map(HoverTarget.Segment(_))
            )
            .getOrElse(HoverTarget.None)

  private def hitTestPoint(
      mx: Double,
      my: Double,
      segments: Vector[EnvelopeSegment],
      xs: Vector[Int],
      height: Int,
      range: Double
  ): Option[Int] =
    val idx = segments.indices.indexWhere { i =>
      val x = xs(i)
      val y = (1.0 - segments(i).peak / range) * height
      val dist = math.sqrt((mx - x) * (mx - x) + (my - y) * (my - y))
      dist <= HitRadius
    }
    if idx >= 0 then Some(idx) else None

  private def hitTestSegment(
      mx: Double,
      my: Double,
      segments: Vector[EnvelopeSegment],
      xs: Vector[Int],
      height: Int,
      range: Double
  ): Option[Int] =
    val idx = (1 until segments.length).indexWhere { i =>
      val x1 = xs(i - 1)
      val y1 = (1.0 - segments(i - 1).peak / range) * height
      val x2 = xs(i)
      val y2 = (1.0 - segments(i).peak / range) * height
      MathUtils.distanceToSegment(mx, my, x1, y1, x2, y2) <= SegmentHitThreshold
    }
    if idx >= 0 then Some(idx + 1) else None

  private def handleDrag(e: MouseEvent): Unit =
    if DragState.primaryIndex >= 0 then
      viewModel.foreach { vm =>
        val segments = vm.getFullSegments
        if DragState.primaryIndex < segments.length then
          val peakDelta = calculatePeakDelta(e.getY, segments)
          val timeDelta = calculateTimeDelta(e.getX, segments)

          val updates = applyUpdates(segments, peakDelta, timeDelta)
          if updates.nonEmpty then vm.updateSegments(updates.toSeq)
          requestRedraw()
          e.consume()
      }

  private def calculatePeakDelta(
      y: Double,
      segments: Vector[EnvelopeSegment]
  ): Int =
    val newPrimaryPeak = calculatePeak(y)
    val startPeak = DragState.startPeaks.getOrElse(
      DragState.primaryIndex,
      segments(DragState.primaryIndex).peak
    )
    newPrimaryPeak - startPeak

  private def calculateTimeDelta(
      x: Double,
      segments: Vector[EnvelopeSegment]
  ): Int =
    if DragState.lockTime || DragState.totalTimeScale <= 0 then 0
    else
      val mouseTime = (x / DragState.totalTimeScale).toInt
      val startAbs =
        DragState.startAbsTimes.getOrElse(DragState.primaryIndex, 0)
      val rawDelta = mouseTime - startAbs

      var minDelta = -Int16.Range * segments.length
      var maxDelta = Int16.Range * segments.length

      selection.foreach { idx =>
        if idx > 0 && !selection.contains(idx - 1) then
          val d = DragState.startDurations.getOrElse(idx, 0)
          minDelta = math.max(minDelta, -d)
          maxDelta = math.min(maxDelta, Int16.Range - 1 - d)
        if idx < segments.length - 1 && !selection.contains(idx + 1) then
          val d = DragState.startDurations.getOrElse(idx + 1, 0)
          maxDelta = math.min(maxDelta, d)
          minDelta = math.max(minDelta, d - (Int16.Range - 1))
      }

      rawDelta.max(minDelta).min(maxDelta)

  private def applyUpdates(
      segments: Vector[EnvelopeSegment],
      peakDelta: Int,
      timeDelta: Int
  ): ListBuffer[(Int, EnvelopeSegment)] =
    val updates = ListBuffer[(Int, EnvelopeSegment)]()

    selection.foreach { idx =>
      if idx < segments.length then
        val originPeak =
          DragState.startPeaks.getOrElse(idx, segments(idx).peak)
        val peak = (originPeak + peakDelta).max(0).min(Int16.Range - 1)
        updates += (idx -> EnvelopeSegment(segments(idx).duration, peak))
    }

    val affectedIndices = selection ++ selection.map(_ + 1)
    affectedIndices.foreach { segIdx =>
      if segIdx > 0 && segIdx < segments.length then
        val isStartSelected = selection.contains(segIdx - 1)
        val isEndSelected = selection.contains(segIdx)

        val oldDur = DragState.startDurations.getOrElse(segIdx, 0)
        var newDur = oldDur

        if isEndSelected && !isStartSelected then newDur = oldDur + timeDelta
        else if isStartSelected && !isEndSelected then
          newDur = oldDur - timeDelta

        val index = updates.indexWhere(_._1 == segIdx)
        if index >= 0 then
          val current = updates(index)._2
          updates(index) = segIdx -> EnvelopeSegment(newDur, current.peak)
        else updates += segIdx -> EnvelopeSegment(newDur, segments(segIdx).peak)
    }
    updates

  private def calculatePeak(y: Double): Int =
    val height = getHeight
    val range = Int16.Range.toDouble
    val normalizedY = 1.0 - (y / height)
    (normalizedY * range).toInt.max(0).min(Int16.Range - 1)

object JagEnvelopeEditorCanvas:
  /** Creates envelope editor canvas. */
  def apply(): JagEnvelopeEditorCanvas = new JagEnvelopeEditorCanvas()
