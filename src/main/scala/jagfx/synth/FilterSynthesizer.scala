package jagfx.synth

import jagfx.model._
import jagfx.constants
import jagfx.constants.{Int16, FilterUpdateRate}
import jagfx.utils.MathUtils._

private case class FilterCoefs(count0: Int, count1: Int, inverseA0: Int)

/** IIR filter processor. */
object FilterSynthesizer:
  /** Applies IIR filter to buffer in-place. */
  def apply(buffer: Array[Int], filter: Filter, sampleCount: Int): Unit =
    val envelopeEval = filter.envelope.map(EnvelopeEvaluator(_))
    envelopeEval.foreach(_.reset())

    val state = FilterState(filter)
    val input = buffer.clone()

    var t = envelopeEval.map(_.evaluate(sampleCount)).getOrElse(Int16.Range)
    var envelopeFactor = t / Int16.Range.toFloat
    var FilterCoefs(count0, count1, inverseA0) = state.update(envelopeFactor)

    var i = 0
    while i < sampleCount do
      val chunkEnd = _computeChunkEnd(i, sampleCount, count0)

      while i < chunkEnd do
        buffer(i) = _processSample(
          input,
          buffer,
          i,
          count0,
          count1,
          inverseA0,
          sampleCount,
          state
        )

        envelopeEval.foreach { eval =>
          t = eval.evaluate(sampleCount)
          envelopeFactor = t / Int16.Range.toFloat
        }

        i += 1

      if i < sampleCount then
        val coefs = state.update(envelopeFactor)
        count0 = coefs.count0
        count1 = coefs.count1
        inverseA0 = coefs.inverseA0

  private def _computeChunkEnd(i: Int, sampleCount: Int, count0: Int): Int =
    val nextChunk = math.min(i + FilterUpdateRate, sampleCount)
    if nextChunk < sampleCount - count0 then nextChunk else sampleCount

  private def _processSample(
      input: Array[Int],
      buffer: Array[Int],
      i: Int,
      count0: Int,
      count1: Int,
      inverseA0: Int,
      sampleCount: Int,
      state: FilterState
  ): Int =
    val inputIdx = i + count0
    val x_curr = if inputIdx < sampleCount then input(inputIdx).toLong else 0L
    var output = (x_curr * inverseA0) >> 16

    var k = 0
    while k < count0 do
      val ffIdx = i + count0 - k - 1
      if ffIdx < sampleCount then
        output += (input(ffIdx).toLong * state.feedforward(k)) >> 16
      k += 1

    k = 0
    while k < count1 do
      val fbIdx = i - k - 1
      if fbIdx >= 0 then
        output -= (buffer(fbIdx).toLong * state.feedback(k)) >> 16
      k += 1

    output.toInt

  private class FilterState(filter: Filter):
    val feedforward: Array[Int] = Array.ofDim[Int](8)
    val feedback: Array[Int] = Array.ofDim[Int](8)
    private val _floatCoefs = Array.ofDim[Float](2, 8)

    def update(envelopeFactor: Float): FilterCoefs =
      val inverseA0 = _computeInverseA0(envelopeFactor)
      val floatInvA0 = inverseA0 / Int16.Range.toFloat
      val c0 = _computeCoefs(0, envelopeFactor, floatInvA0)
      val c1 = _computeCoefs(1, envelopeFactor, 1.0f)
      FilterCoefs(c0, c1, inverseA0)

    private def _computeInverseA0(envelopeFactor: Float): Int =
      val unityGain0 = filter.unity(0)
      val unityGain1 = filter.unity(1)
      val interpolatedGain =
        unityGain0.toFloat + (unityGain1 - unityGain0) * envelopeFactor
      val gainDb = interpolatedGain * 0.0030517578f
      val floatInvA0 = Math.pow(0.1, gainDb / 20.0).toFloat
      (floatInvA0 * Int16.Range).toInt

    private def _computeCoefs(
        dir: Int,
        envelopeFactor: Float,
        floatInvA0: Float
    ): Int =
      val pairCount = filter.pairCounts(dir)
      if pairCount == 0 then return 0

      _initFirstPair(dir, envelopeFactor)
      _cascadeRemainingPairs(dir, pairCount, envelopeFactor)
      _applyGainAndConvert(dir, pairCount, floatInvA0)

    private def _initFirstPair(dir: Int, envelopeFactor: Float): Unit =
      val amp = _getAmplitude(filter, dir, 0, envelopeFactor)
      val phase = _calculatePhase(filter, dir, 0, envelopeFactor)
      val cosPhase = Math.cos(phase).toFloat
      _floatCoefs(dir)(0) = -2.0f * amp * cosPhase
      _floatCoefs(dir)(1) = amp * amp

    private def _cascadeRemainingPairs(
        dir: Int,
        pairCount: Int,
        envelopeFactor: Float
    ): Unit =
      var p = 1
      while p < pairCount do
        val ampP = _getAmplitude(filter, dir, p, envelopeFactor)
        val phaseP = _calculatePhase(filter, dir, p, envelopeFactor)
        val cosPhaseP = Math.cos(phaseP).toFloat
        val term1 = -2.0f * ampP * cosPhaseP
        val term2 = ampP * ampP

        _floatCoefs(dir)(p * 2 + 1) = _floatCoefs(dir)(p * 2 - 1) * term2
        _floatCoefs(dir)(p * 2) = _floatCoefs(dir)(
          p * 2 - 1
        ) * term1 + _floatCoefs(dir)(p * 2 - 2) * term2

        var k = p * 2 - 1
        while k >= 2 do
          _floatCoefs(dir)(k) += _floatCoefs(dir)(k - 1) * term1 + _floatCoefs(
            dir
          )(k - 2) * term2
          k -= 1

        _floatCoefs(dir)(1) += _floatCoefs(dir)(0) * term1 + term2
        _floatCoefs(dir)(0) += term1
        p += 1

    private def _applyGainAndConvert(
        dir: Int,
        pairCount: Int,
        floatInvA0: Float
    ): Int =
      val iCoef = if dir == 0 then feedforward else feedback
      val coefCount = pairCount * 2

      if dir == 0 then
        var k = 0
        while k < coefCount do
          _floatCoefs(0)(k) *= floatInvA0
          k += 1

      var k = 0
      while k < coefCount do
        iCoef(k) = (_floatCoefs(dir)(k) * Int16.Range).toInt
        k += 1

      coefCount

  private def _getAmplitude(
      filter: Filter,
      dir: Int,
      pair: Int,
      factor: Float
  ): Float =
    val mag0 = filter.pairMagnitude(dir)(0)(pair)
    val mag1 = filter.pairMagnitude(dir)(1)(pair)
    val interpolatedMag = mag0.toFloat + factor * (mag1 - mag0)
    val dbValue = interpolatedMag * 0.0015258789f
    1.0f - dBToLinear(-dbValue).toFloat

  private def _calculatePhase(
      filter: Filter,
      dir: Int,
      pair: Int,
      factor: Float
  ): Float =
    val phase0 = filter.pairPhase(dir)(0)(pair)
    val phase1 = filter.pairPhase(dir)(1)(pair)
    val interpolatedPhase = phase0.toFloat + factor * (phase1 - phase0)
    val scaledPhase = interpolatedPhase * 1.2207031e-4f
    _getOctavePhase(scaledPhase)

  private def _getOctavePhase(pow2Value: Float): Float =
    val frequencyHz = Math.pow(2.0, pow2Value) * 32.703197
    (frequencyHz * TwoPi / constants.SampleRate).toFloat
