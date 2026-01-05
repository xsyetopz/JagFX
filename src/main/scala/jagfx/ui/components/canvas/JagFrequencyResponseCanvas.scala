package jagfx.ui.components.canvas

import jagfx.Constants.Int16
import jagfx.ui.viewmodel.FilterViewModel
import jagfx.utils.ColorUtils.*
import jagfx.utils.DrawingUtils.*
import jagfx.utils.MathUtils

/** Canvas rendering frequency response curve from filter poles/zeros. */
class JagFrequencyResponseCanvas extends JagBaseCanvas:
  import JagFrequencyResponseCanvas._

  // Fields
  private var viewModel: Option[FilterViewModel] = None

  // Init: styling
  getStyleClass.add("jag-freq-response-canvas")

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
    drawGrid(buffer, width, height)
    drawCenterLine(buffer, width, height)
    viewModel.foreach(vm => drawResponseCurve(buffer, width, height, vm))

  private def drawGrid(buffer: Array[Int], width: Int, height: Int): Unit =
    for i <- 1 until GridCols do
      val x = i * width / GridCols
      line(buffer, width, height, x, 0, x, height, GridLineFaint)
    for i <- 1 until GridRows do
      val y = i * height / GridRows
      line(buffer, width, height, 0, y, width, y, GridLineFaint)

  private def drawResponseCurve(
      buffer: Array[Int],
      width: Int,
      height: Int,
      vm: FilterViewModel
  ): Unit =
    if vm.isEmpty then return
    val points = computeResponsePoints(width, height, vm)
    for x <- 1 until width do
      line(
        buffer,
        width,
        height,
        x - 1,
        points(x - 1),
        x,
        points(x),
        FilterResponse
      )

  private def computeResponsePoints(
      width: Int,
      height: Int,
      vm: FilterViewModel
  ): Array[Int] =
    val points = new Array[Int](width)
    for x <- 0 until width do
      val freq = x.toDouble / width
      val response = computeFrequencyResponse(vm, freq)
      val dB = MathUtils.clamp(MathUtils.linearToDb(response), MinDb, MaxDb)
      val y = decibelsToY(dB, height)
      points(x) = MathUtils.clamp(y, 0, height - 1)
    points

  private def computeFrequencyResponse(
      vm: FilterViewModel,
      normalizedFreq: Double
  ): Double =
    val omega = normalizedFreq * math.Pi
    val zeroContrib = computeZeroContrib(vm, omega)
    val poleContrib = computePoleContrib(vm, omega)
    val unity = vm.unity0.get / Int16.Range + MinGain
    unity * zeroContrib / poleContrib

  private def computeZeroContrib(vm: FilterViewModel, omega: Double): Double =
    var contrib = 1.0
    val (eReal, eImag) = (math.cos(omega), math.sin(omega))
    for i <- 0 until vm.pairCount0.get do
      val phase = vm.pairPhase(0)(i)(0).get / Int16.Range * MathUtils.TwoPi
      val mag = vm.pairMagnitude(0)(i)(0).get / Int16.Range
      val zReal = mag * math.cos(phase)
      val zImag = mag * math.sin(phase)
      contrib *= MathUtils.distance(eReal, eImag, zReal, zImag)
    contrib

  private def computePoleContrib(vm: FilterViewModel, omega: Double): Double =
    var contrib = 1.0
    val (eReal, eImag) = (math.cos(omega), math.sin(omega))
    for i <- 0 until vm.pairCount1.get do
      val phase = vm.pairPhase(1)(i)(0).get / Int16.Range * MathUtils.TwoPi
      val mag = vm.pairMagnitude(1)(i)(0).get / Int16.Range
      val pReal = mag * math.cos(phase)
      val pImag = mag * math.sin(phase)
      contrib *= math.max(
        MinGain,
        MathUtils.distance(eReal, eImag, pReal, pImag)
      )
    contrib

  private def decibelsToY(dB: Double, height: Int): Int =
    (height / 2 - (dB / MaxDb * height / 2)).toInt

object JagFrequencyResponseCanvas:
  // Constants
  private final val GridCols = 8
  private final val GridRows = 4
  private final val MinGain = 0.001
  private final val MinDb = -24.0
  private final val MaxDb = 24.0

  /** Creates frequency response canvas. */
  def apply(): JagFrequencyResponseCanvas = new JagFrequencyResponseCanvas()
