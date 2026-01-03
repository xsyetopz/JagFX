package jagfx.ui.components

import jagfx.ui.viewmodel.FilterViewModel
import jagfx.utils.ColorUtils._
import jagfx.utils.DrawingUtils._
import jagfx.utils.MathUtils
import jagfx.utils.MathUtils.{TwoPi, clamp, distance, linearToDb}

/** Canvas rendering frequency response curve from filter poles/zeros. */
class JagFrequencyResponseCanvas extends JagBaseCanvas:
  import JagFrequencyResponseCanvas._

  private var viewModel: Option[FilterViewModel] = None

  getStyleClass.add("jag-freq-response-canvas")

  def setViewModel(vm: FilterViewModel): Unit =
    viewModel = Some(vm)
    vm.addChangeListener(() =>
      javafx.application.Platform.runLater(() => requestRedraw())
    )
    requestRedraw()

  override protected def drawContent(buffer: Array[Int], w: Int, h: Int): Unit =
    drawGrid(buffer, w, h)
    drawCenterLine(buffer, w, h)
    viewModel.foreach(vm => drawResponseCurve(buffer, w, h, vm))

  private def drawGrid(buffer: Array[Int], w: Int, h: Int): Unit =
    for i <- 1 until GridCols do
      val x = i * w / GridCols
      line(buffer, w, h, x, 0, x, h, GridLineFaint)
    for i <- 1 until GridRows do
      val y = i * h / GridRows
      line(buffer, w, h, 0, y, w, y, GridLineFaint)

  private def drawResponseCurve(
      buffer: Array[Int],
      w: Int,
      h: Int,
      vm: FilterViewModel
  ): Unit =
    if vm.isEmpty then return
    val points = computeResponsePoints(w, h, vm)
    for x <- 1 until w do
      line(buffer, w, h, x - 1, points(x - 1), x, points(x), FilterResponse)

  private def computeResponsePoints(
      w: Int,
      h: Int,
      vm: FilterViewModel
  ): Array[Int] =
    val points = new Array[Int](w)
    for x <- 0 until w do
      val freq = x.toDouble / w
      val response = computeFrequencyResponse(vm, freq)
      val dB = clamp(linearToDb(response), MinDb, MaxDb)
      val y = decibelsToY(dB, h)
      points(x) = clamp(y, 0, h - 1)
    points

  private def computeFrequencyResponse(
      vm: FilterViewModel,
      normalizedFreq: Double
  ): Double =
    val omega = normalizedFreq * math.Pi
    val zeroContrib = computeZeroContribution(vm, omega)
    val poleContrib = computePoleContribution(vm, omega)
    val unity = vm.unity0.get / ValueScale + MinGain
    unity * zeroContrib / poleContrib

  private def computeZeroContribution(
      vm: FilterViewModel,
      omega: Double
  ): Double =
    var contrib = 1.0
    val (eReal, eImag) = (math.cos(omega), math.sin(omega))
    for i <- 0 until vm.pairCount0.get do
      val phase = vm.pairPhase(0)(i)(0).get / ValueScale * TwoPi
      val mag = vm.pairMagnitude(0)(i)(0).get / ValueScale
      val zReal = mag * math.cos(phase)
      val zImag = mag * math.sin(phase)
      contrib *= distance(eReal, eImag, zReal, zImag)
    contrib

  private def computePoleContribution(
      vm: FilterViewModel,
      omega: Double
  ): Double =
    var contrib = 1.0
    val (eReal, eImag) = (math.cos(omega), math.sin(omega))
    for i <- 0 until vm.pairCount1.get do
      val phase = vm.pairPhase(1)(i)(0).get / ValueScale * TwoPi
      val mag = vm.pairMagnitude(1)(i)(0).get / ValueScale
      val pReal = mag * math.cos(phase)
      val pImag = mag * math.sin(phase)
      contrib *= math.max(MinGain, distance(eReal, eImag, pReal, pImag))
    contrib

  private def decibelsToY(dB: Double, h: Int): Int =
    (h / 2 - (dB / MaxDb * h / 2)).toInt

object JagFrequencyResponseCanvas:
  private val GridCols = 8
  private val GridRows = 4
  private val ValueScale = 65535.0
  private val MinGain = 0.001
  private val MinDb = -24.0
  private val MaxDb = 24.0

  def apply(): JagFrequencyResponseCanvas = new JagFrequencyResponseCanvas()
