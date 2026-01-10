package jagfx.synth

import jagfx.Constants
import jagfx.Constants.FilterUpdateRate
import jagfx.Constants.Int16
import jagfx.model.*
import jagfx.utils.MathUtils.*

private case class FilterCoefs(count0: Int, count1: Int, inverseA0: Int)

/** IIR filter processor. */
object FilterSynthesizer:
  /** Applies IIR filter to buffer in-place. */
  def apply(buffer: Array[Int], filter: Filter, sampleCount: Int): Unit =
    val envelopeEval = filter.envelope.map(EnvelopeEvaluator(_))
    envelopeEval.foreach(_.reset())

    val state = FilterState(filter)
    val tempInput = BufferPool.acquire(sampleCount)
    System.arraycopy(buffer, 0, tempInput, 0, sampleCount)

    var envelopeValue =
      envelopeEval.map(_.evaluate(sampleCount)).getOrElse(Int16.Range)
    var envelopeFactor = envelopeValue / Int16.Range.toFloat
    var FilterCoefs(count0, count1, inverseA0) = state.update(envelopeFactor)

    var sampleIndex = 0
    while sampleIndex < sampleCount do
      val chunkEnd = computeChunkEnd(sampleIndex, sampleCount, count0)

      while sampleIndex < chunkEnd do
        buffer(sampleIndex) = processSample(
          tempInput,
          buffer,
          sampleIndex,
          count0,
          count1,
          inverseA0,
          sampleCount,
          state
        )

        envelopeEval.foreach { eval =>
          envelopeValue = eval.evaluate(sampleCount)
          envelopeFactor = envelopeValue / Int16.Range.toFloat
        }

        sampleIndex += 1

      if sampleIndex < sampleCount then
        val coefs = state.update(envelopeFactor)
        count0 = coefs.count0
        count1 = coefs.count1
        inverseA0 = coefs.inverseA0

    BufferPool.release(tempInput)

  private inline def computeChunkEnd(i: Int, sampleCount: Int, count0: Int): Int =
    val nextChunk = math.min(i + FilterUpdateRate, sampleCount)
    if nextChunk < sampleCount - count0 then nextChunk else sampleCount

  private def processSample(
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
    val xCurr = if inputIdx < sampleCount then input(inputIdx).toLong else 0L
    var output = (xCurr * inverseA0) >> 16

    var coeffIndex = 0
    while coeffIndex < count0 do
      val ffIdx = i + count0 - coeffIndex - 1
      if ffIdx < sampleCount then
        output += (input(ffIdx).toLong * state.feedforward(coeffIndex)) >> 16
      coeffIndex += 1

    coeffIndex = 0
    while coeffIndex < count1 do
      val fbIdx = i - coeffIndex - 1
      if fbIdx >= 0 then
        output -= (buffer(fbIdx).toLong * state.feedback(coeffIndex)) >> 16
      coeffIndex += 1

    output.toInt

  private class FilterState(filter: Filter):
    // Fields
    val feedforward: Array[Int] = Array.ofDim[Int](8)
    val feedback: Array[Int] = Array.ofDim[Int](8)
    private val floatCoefs = Array.ofDim[Float](2, 8)

    def update(envelopeFactor: Float): FilterCoefs =
      val inverseA0 = computeInverseA0(envelopeFactor)
      val floatInvA0 = inverseA0 / Int16.Range.toFloat
      val c0 = computeCoefs(0, envelopeFactor, floatInvA0)
      val c1 = computeCoefs(1, envelopeFactor, 1.0f)
      FilterCoefs(c0, c1, inverseA0)

    private def computeInverseA0(envelopeFactor: Float): Int =
      val unityGain0 = filter.unity(0)
      val unityGain1 = filter.unity(1)
      val interpolatedGain =
        unityGain0.toFloat + (unityGain1 - unityGain0) * envelopeFactor
      val gainDb = interpolatedGain * 0.0030517578f
      val floatInvA0 = Math.pow(0.1, gainDb / 20.0).toFloat
      (floatInvA0 * Int16.Range).toInt

    private def computeCoefs(
        dir: Int,
        envelopeFactor: Float,
        floatInvA0: Float
    ): Int =
      val pairCount = filter.pairCounts(dir)
      if pairCount == 0 then return 0

      initFirstPair(dir, envelopeFactor)
      cascadeRemainingPairs(dir, pairCount, envelopeFactor)
      applyGainAndConvert(dir, pairCount, floatInvA0)

    private def initFirstPair(dir: Int, envelopeFactor: Float): Unit =
      val amp = getAmplitude(filter, dir, 0, envelopeFactor)
      val phase = calculatePhase(filter, dir, 0, envelopeFactor)
      val cosPhase = Math.cos(phase).toFloat
      floatCoefs(dir)(0) = -2.0f * amp * cosPhase
      floatCoefs(dir)(1) = amp * amp

    private def cascadeRemainingPairs(
        dir: Int,
        pairCount: Int,
        envelopeFactor: Float
    ): Unit =
      var pairIndex = 1
      while pairIndex < pairCount do
        val ampP = getAmplitude(filter, dir, pairIndex, envelopeFactor)
        val phaseP = calculatePhase(filter, dir, pairIndex, envelopeFactor)
        val cosPhaseP = Math.cos(phaseP).toFloat
        val term1 = -2.0f * ampP * cosPhaseP
        val term2 = ampP * ampP

        floatCoefs(dir)(pairIndex * 2 + 1) =
          floatCoefs(dir)(pairIndex * 2 - 1) * term2
        floatCoefs(dir)(pairIndex * 2) = floatCoefs(dir)(
          pairIndex * 2 - 1
        ) * term1 + floatCoefs(dir)(pairIndex * 2 - 2) * term2

        var coeffIndex = pairIndex * 2 - 1
        while coeffIndex >= 2 do
          floatCoefs(dir)(coeffIndex) += floatCoefs(dir)(
            coeffIndex - 1
          ) * term1 + floatCoefs(dir)(coeffIndex - 2) * term2
          coeffIndex -= 1

        floatCoefs(dir)(1) += floatCoefs(dir)(0) * term1 + term2
        floatCoefs(dir)(0) += term1
        pairIndex += 1

    private def applyGainAndConvert(
        dir: Int,
        pairCount: Int,
        floatInvA0: Float
    ): Int =
      val iCoef = if dir == 0 then feedforward else feedback
      val coefCount = pairCount * 2

      if dir == 0 then
        var coeffIndex = 0
        while coeffIndex < coefCount do
          floatCoefs(0)(coeffIndex) *= floatInvA0
          coeffIndex += 1

      var coeffIndex = 0
      while coeffIndex < coefCount do
        iCoef(coeffIndex) = (floatCoefs(dir)(coeffIndex) * Int16.Range).toInt
        coeffIndex += 1

      coefCount

  private def getAmplitude(
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

  private def calculatePhase(
      filter: Filter,
      dir: Int,
      pair: Int,
      factor: Float
  ): Float =
    val phase0 = filter.pairPhase(dir)(0)(pair)
    val phase1 = filter.pairPhase(dir)(1)(pair)
    val interpolatedPhase = phase0.toFloat + factor * (phase1 - phase0)
    val scaledPhase = interpolatedPhase * 1.2207031e-4f
    getOctavePhase(scaledPhase)

  private def getOctavePhase(pow2Value: Float): Float =
    val frequencyHz = Math.pow(2.0, pow2Value) * 32.703197
    (frequencyHz * TwoPi / Constants.SampleRate).toFloat
