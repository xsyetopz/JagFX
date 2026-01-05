package jagfx.ui.components.canvas

import jagfx.ui.viewmodel.FilterViewModel
import jagfx.utils.ColorUtils._
import jagfx.utils.DrawingUtils._
import jagfx.utils.MathUtils
import jagfx.Constants.Int16

/** Canvas rendering frequency response curve from filter poles/zeros. */
class JagFrequencyResponseCanvas extends JagBaseCanvas:
  import JagFrequencyResponseCanvas._

  private var _viewModel: Option[FilterViewModel] = None

  getStyleClass.add("jag-freq-response-canvas")

  def setViewModel(vm: FilterViewModel): Unit =
    _viewModel = Some(vm)
    vm.addChangeListener(() =>
      javafx.application.Platform.runLater(() => requestRedraw())
    )
    requestRedraw()

  override protected def drawContent(buffer: Array[Int], w: Int, h: Int): Unit =
    _drawGrid(buffer, w, h)
    drawCenterLine(buffer, w, h)
    _viewModel.foreach(vm => _drawResponseCurve(buffer, w, h, vm))

  private def _drawGrid(buffer: Array[Int], w: Int, h: Int): Unit =
    for i <- 1 until _GridCols do
      val x = i * w / _GridCols
      line(buffer, w, h, x, 0, x, h, GridLineFaint)
    for i <- 1 until _GridRows do
      val y = i * h / _GridRows
      line(buffer, w, h, 0, y, w, y, GridLineFaint)

  private def _drawResponseCurve(
      buffer: Array[Int],
      w: Int,
      h: Int,
      vm: FilterViewModel
  ): Unit =
    if vm.isEmpty then return
    val points = _computeResponsePoints(w, h, vm)
    for x <- 1 until w do
      line(buffer, w, h, x - 1, points(x - 1), x, points(x), FilterResponse)

  private def _computeResponsePoints(
      w: Int,
      h: Int,
      vm: FilterViewModel
  ): Array[Int] =
    val points = new Array[Int](w)
    for x <- 0 until w do
      val freq = x.toDouble / w
      val response = _computeFrequencyResponse(vm, freq)
      val dB = MathUtils.clamp(MathUtils.linearToDb(response), _MinDb, _MaxDb)
      val y = _decibelsToY(dB, h)
      points(x) = MathUtils.clamp(y, 0, h - 1)
    points

  private def _computeFrequencyResponse(
      vm: FilterViewModel,
      normalizedFreq: Double
  ): Double =
    val omega = normalizedFreq * math.Pi
    val zeroContrib = _computeZeroContrib(vm, omega)
    val poleContrib = _computePoleContrib(vm, omega)
    val unity = vm.unity0.get / Int16.Range + _MinGain
    unity * zeroContrib / poleContrib

  private def _computeZeroContrib(
      vm: FilterViewModel,
      omega: Double
  ): Double =
    var contrib = 1.0
    val (eReal, eImag) = (math.cos(omega), math.sin(omega))
    for i <- 0 until vm.pairCount0.get do
      val phase = vm.pairPhase(0)(i)(0).get / Int16.Range * MathUtils.TwoPi
      val mag = vm.pairMagnitude(0)(i)(0).get / Int16.Range
      val zReal = mag * math.cos(phase)
      val zImag = mag * math.sin(phase)
      contrib *= MathUtils.distance(eReal, eImag, zReal, zImag)
    contrib

  private def _computePoleContrib(
      vm: FilterViewModel,
      omega: Double
  ): Double =
    var contrib = 1.0
    val (eReal, eImag) = (math.cos(omega), math.sin(omega))
    for i <- 0 until vm.pairCount1.get do
      val phase = vm.pairPhase(1)(i)(0).get / Int16.Range * MathUtils.TwoPi
      val mag = vm.pairMagnitude(1)(i)(0).get / Int16.Range
      val pReal = mag * math.cos(phase)
      val pImag = mag * math.sin(phase)
      contrib *= math.max(
        _MinGain,
        MathUtils.distance(eReal, eImag, pReal, pImag)
      )
    contrib

  private def _decibelsToY(dB: Double, h: Int): Int =
    (h / 2 - (dB / _MaxDb * h / 2)).toInt

object JagFrequencyResponseCanvas:
  private val _GridCols = 8
  private val _GridRows = 4
  private val _MinGain = 0.001
  private val _MinDb = -24.0
  private val _MaxDb = 24.0

  def apply(): JagFrequencyResponseCanvas = new JagFrequencyResponseCanvas()
