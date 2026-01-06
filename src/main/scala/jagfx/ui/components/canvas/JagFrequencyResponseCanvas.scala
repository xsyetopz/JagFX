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

    val endPoints = computeResponsePoints(width, height, vm, 1)
    drawCurve(buffer, width, height, endPoints, FilterResponse, dashed = true)
    val startPoints = computeResponsePoints(width, height, vm, 0)
    drawCurve(
      buffer,
      width,
      height,
      startPoints,
      FilterResponse,
      dashed = false
    )

  private def drawCurve(
      buffer: Array[Int],
      width: Int,
      height: Int,
      points: Array[Int],
      color: Int,
      dashed: Boolean
  ): Unit =
    for x <- 1 until width do
      if !dashed || (x % 4 < 2) then
        line(
          buffer,
          width,
          height,
          x - 1,
          points(x - 1),
          x,
          points(x),
          color
        )

  private def computeResponsePoints(
      width: Int,
      height: Int,
      vm: FilterViewModel,
      point: Int
  ): Array[Int] =
    val points = new Array[Int](width)
    for x <- 0 until width do
      val freq = x.toDouble / width
      val response = computeFrequencyResponse(vm, freq, point)
      val dB = MathUtils.clamp(MathUtils.linearToDb(response), MinDb, MaxDb)
      val y = decibelsToY(dB, height)
      points(x) = MathUtils.clamp(y, 0, height - 1)
    points

  private def computeFrequencyResponse(
      vm: FilterViewModel,
      normalizedFreq: Double,
      point: Int
  ): Double =
    val omega = normalizedFreq * math.Pi
    val zeroContrib = computeZeroContrib(vm, omega, point)
    val poleContrib = computePoleContrib(vm, omega, point)
    val unityVal = if point == 0 then vm.unity0.get else vm.unity1.get
    val unity = unityVal / Int16.Range.toDouble + MinGain
    unity * zeroContrib / poleContrib

  private def computeZeroContrib(
      vm: FilterViewModel,
      omega: Double,
      point: Int
  ): Double =
    var contrib = 1.0
    val (eReal, eImag) = (math.cos(omega), math.sin(omega))
    for i <- 0 until vm.pairCount0.get do
      val phase =
        vm.pairPhase(0)(i)(point).get / Int16.Range.toDouble * MathUtils.TwoPi
      val mag = vm.pairMagnitude(0)(i)(point).get / Int16.Range.toDouble
      val zReal = mag * math.cos(phase)
      val zImag = mag * math.sin(phase)
      contrib *= MathUtils.distance(eReal, eImag, zReal, zImag)
    contrib

  private def computePoleContrib(
      vm: FilterViewModel,
      omega: Double,
      point: Int
  ): Double =
    var contrib = 1.0
    val (eReal, eImag) = (math.cos(omega), math.sin(omega))
    for i <- 0 until vm.pairCount1.get do
      val phase =
        vm.pairPhase(1)(i)(point).get / Int16.Range.toDouble * MathUtils.TwoPi
      val mag = vm.pairMagnitude(1)(i)(point).get / Int16.Range.toDouble
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
