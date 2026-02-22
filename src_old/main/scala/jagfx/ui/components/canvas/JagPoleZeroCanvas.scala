package jagfx.ui.components.canvas

import jagfx.Constants.Int16
import jagfx.synth.LookupTables
import jagfx.ui.viewmodel.FilterViewModel
import jagfx.utils.ColorUtils.*
import jagfx.utils.DrawingUtils.*
import jagfx.utils.MathUtils

/** Canvas rendering pole-zero diagram on unit circle. */
class JagPoleZeroCanvas extends JagBaseCanvas:
  import JagPoleZeroCanvas._

  // Types
  private case class Geometry(width: Int, height: Int):
    val cx: Int = width >> 1
    val cy: Int = height >> 1
    val radius: Int = math.min(width, height) / 2 - CirclePadding

  // Fields
  private var viewModel: Option[FilterViewModel] = None

  // Init: styling
  getStyleClass.add("jag-pole-zero-canvas")

  /** Binds filter view model. */
  def setViewModel(vm: FilterViewModel): Unit =
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
    val geom = Geometry(width, height)
    drawGrid(buffer, width, height, geom)
    drawUnitCircle(buffer, width, height, geom)
    viewModel.foreach { vm =>
      // end state (dimmed)
      val dimZero = dimColor(FilterZero)
      val dimPole = dimColor(FilterPole)
      drawFeedforwardPoles(buffer, width, height, vm, geom, 1, dimZero)
      drawFeedbackPoles(buffer, width, height, vm, geom, 1, dimPole)

      // start state (normal)
      drawFeedforwardPoles(buffer, width, height, vm, geom, 0, FilterZero)
      drawFeedbackPoles(buffer, width, height, vm, geom, 0, FilterPole)
    }

  private def drawGrid(
      buffer: Array[Int],
      width: Int,
      height: Int,
      g: Geometry
  ): Unit =
    line(buffer, width, height, 0, g.cy, width, g.cy, GridLineFaint)
    line(buffer, width, height, g.cx, 0, g.cx, height, GridLineFaint)

  private def drawUnitCircle(
      buffer: Array[Int],
      width: Int,
      height: Int,
      g: Geometry
  ): Unit =
    for i <- 0 until CircleSegments do
      val x1 = g.cx + (g.radius * LookupTables.unitCircleX(i)).toInt
      val y1 = g.cy + (g.radius * LookupTables.unitCircleY(i)).toInt
      val x2 = g.cx + (g.radius * LookupTables.unitCircleX(i + 1)).toInt
      val y2 = g.cy + (g.radius * LookupTables.unitCircleY(i + 1)).toInt
      line(buffer, width, height, x1, y1, x2, y2, BorderDim)

  private def drawFeedforwardPoles(
      buffer: Array[Int],
      width: Int,
      height: Int,
      vm: FilterViewModel,
      g: Geometry,
      point: Int,
      color: Int
  ): Unit =
    for i <- 0 until vm.pairCount0.get do
      val (x, y) = polePosition(vm, 0, i, g, point)
      drawCircleMarker(buffer, width, height, x, y, color)

  private def drawFeedbackPoles(
      buffer: Array[Int],
      width: Int,
      height: Int,
      vm: FilterViewModel,
      g: Geometry,
      point: Int,
      color: Int
  ): Unit =
    for i <- 0 until vm.pairCount1.get do
      val (x, y) = polePosition(vm, 1, i, g, point)
      drawCrossMarker(buffer, width, height, x, y, color)

  private def polePosition(
      vm: FilterViewModel,
      dir: Int,
      idx: Int,
      g: Geometry,
      point: Int
  ): (Int, Int) =
    val phase =
      vm.pairPhase(dir)(idx)(point).get / Int16.Range.toDouble * MathUtils.TwoPi
    val mag = vm.pairMagnitude(dir)(idx)(point).get / Int16.Range.toDouble
    val x = g.cx + (g.radius * mag * math.cos(phase)).toInt
    val y = g.cy - (g.radius * mag * math.sin(phase)).toInt
    (x, y)

  private def drawCrossMarker(
      buffer: Array[Int],
      width: Int,
      height: Int,
      x: Int,
      y: Int,
      color: Int
  ): Unit =
    line(
      buffer,
      width,
      height,
      x - MarkerSize,
      y - MarkerSize,
      x + MarkerSize,
      y + MarkerSize,
      color
    )
    line(
      buffer,
      width,
      height,
      x - MarkerSize,
      y + MarkerSize,
      x + MarkerSize,
      y - MarkerSize,
      color
    )

  private def drawCircleMarker(
      buffer: Array[Int],
      width: Int,
      height: Int,
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
      line(buffer, width, height, x1, y1, x2, y2, color)

object JagPoleZeroCanvas:
  // Constants
  private final val CircleSegments = 64
  private final val MarkerCircleSegments = 8
  private final val CirclePadding = 4
  private final val MarkerSize = 3

  /** Creates pole-zero canvas. */
  def apply(): JagPoleZeroCanvas = new JagPoleZeroCanvas()
