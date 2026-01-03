package jagfx.ui.components

import jagfx.ui.viewmodel.FilterViewModel
import jagfx.utils.ColorUtils._
import jagfx.utils.DrawingUtils._
import jagfx.utils.MathUtils
import jagfx.synth.LookupTables
import jagfx.Constants.Int16

/** Canvas rendering pole-zero diagram on unit circle. */
class JagPoleZeroCanvas extends JagBaseCanvas:
  import JagPoleZeroCanvas._

  private var viewModel: Option[FilterViewModel] = None

  getStyleClass.add("jag-pole-zero-canvas")

  def setViewModel(vm: FilterViewModel): Unit =
    viewModel = Some(vm)
    vm.addChangeListener(() =>
      javafx.application.Platform.runLater(() => requestRedraw())
    )
    requestRedraw()

  override protected def drawContent(buffer: Array[Int], w: Int, h: Int): Unit =
    val geom = Geometry(w, h)
    drawGrid(buffer, w, h, geom)
    drawUnitCircle(buffer, w, h, geom)
    viewModel.foreach { vm =>
      drawFeedforwardPoles(buffer, w, h, vm, geom)
      drawFeedbackPoles(buffer, w, h, vm, geom)
    }

  private case class Geometry(w: Int, h: Int):
    val cx: Int = w >> 1
    val cy: Int = h >> 1
    val radius: Int = math.min(w, h) / 2 - CirclePadding

  private def drawGrid(buffer: Array[Int], w: Int, h: Int, g: Geometry): Unit =
    line(buffer, w, h, 0, g.cy, w, g.cy, GridLineFaint)
    line(buffer, w, h, g.cx, 0, g.cx, h, GridLineFaint)

  private def drawUnitCircle(
      buffer: Array[Int],
      w: Int,
      h: Int,
      g: Geometry
  ): Unit =
    for i <- 0 until CircleSegments do
      val x1 = g.cx + (g.radius * LookupTables.unitCircleX(i)).toInt
      val y1 = g.cy + (g.radius * LookupTables.unitCircleY(i)).toInt
      val x2 = g.cx + (g.radius * LookupTables.unitCircleX(i + 1)).toInt
      val y2 = g.cy + (g.radius * LookupTables.unitCircleY(i + 1)).toInt
      line(buffer, w, h, x1, y1, x2, y2, BorderDim)

  private def drawFeedforwardPoles(
      buffer: Array[Int],
      w: Int,
      h: Int,
      vm: FilterViewModel,
      g: Geometry
  ): Unit =
    for i <- 0 until vm.pairCount0.get do
      val (x, y) = polePosition(vm, 0, i, g)
      drawCircleMarker(buffer, w, h, x, y, FilterZero)

  private def drawFeedbackPoles(
      buffer: Array[Int],
      w: Int,
      h: Int,
      vm: FilterViewModel,
      g: Geometry
  ): Unit =
    for i <- 0 until vm.pairCount1.get do
      val (x, y) = polePosition(vm, 1, i, g)
      drawCrossMarker(buffer, w, h, x, y, FilterPole)

  private def polePosition(
      vm: FilterViewModel,
      dir: Int,
      idx: Int,
      g: Geometry
  ): (Int, Int) =
    val phase = vm.pairPhase(dir)(idx)(0).get / Int16.Range * MathUtils.TwoPi
    val mag = vm.pairMagnitude(dir)(idx)(0).get / Int16.Range
    val x = g.cx + (g.radius * mag * math.cos(phase)).toInt
    val y = g.cy - (g.radius * mag * math.sin(phase)).toInt
    (x, y)

  private def drawCrossMarker(
      buffer: Array[Int],
      w: Int,
      h: Int,
      x: Int,
      y: Int,
      color: Int
  ): Unit =
    line(
      buffer,
      w,
      h,
      x - MarkerSize,
      y - MarkerSize,
      x + MarkerSize,
      y + MarkerSize,
      color
    )
    line(
      buffer,
      w,
      h,
      x - MarkerSize,
      y + MarkerSize,
      x + MarkerSize,
      y - MarkerSize,
      color
    )

  private def drawCircleMarker(
      buffer: Array[Int],
      w: Int,
      h: Int,
      x: Int,
      y: Int,
      color: Int
  ): Unit =
    for i <- 0 until MarkerCircleSegments do
      val a1 = i * MathUtils.TwoPi / MarkerCircleSegments
      val a2 = (i + 1) * MathUtils.TwoPi / MarkerCircleSegments
      val x1 = x + (MarkerSize * math.cos(a1)).toInt
      val y1 = y + (MarkerSize * math.sin(a1)).toInt
      val x2 = x + (MarkerSize * math.cos(a2)).toInt
      val y2 = y + (MarkerSize * math.sin(a2)).toInt
      line(buffer, w, h, x1, y1, x2, y2, color)

object JagPoleZeroCanvas:
  private val CircleSegments = 64
  private val MarkerCircleSegments = 8
  private val CirclePadding = 4
  private val MarkerSize = 3

  def apply(): JagPoleZeroCanvas = new JagPoleZeroCanvas()
