package jagfx.synth

import jagfx.model._
import jagfx.Constants

object FilterSynthesizer:
  def apply(buffer: Array[Int], filter: Filter, sampleCount: Int): Unit =
    val envelopeEval = filter.envelope.map(EnvelopeEvaluator(_))
    envelopeEval.foreach(_.reset())

    val feedforwardCoefs = Array.ofDim[Int](8)
    val feedbackCoefs = Array.ofDim[Int](8)
    val floatCoefs = Array.ofDim[Float](2, 8)
    // clone input to separate X (feedforward) from Y (feedback)
    val input = buffer.clone()

    val FilterUpdateRate = 128
    var output = 0L

    var t =
      if envelopeEval.isDefined then envelopeEval.get.evaluate(sampleCount)
      else 65536
    var envelopeFactor = t / 65536.0f

    def updateCoefficients(): (Int, Int, Int) =
      val unityGain0 = filter.unity(0)
      val unityGain1 = filter.unity(1)
      val interpolatedGain =
        unityGain0.toFloat + (unityGain1 - unityGain0) * envelopeFactor
      val gainDb = interpolatedGain * 0.0030517578f
      val floatInvA0 = Math.pow(0.1, gainDb / 20.0).toFloat
      val inverseA0 = (floatInvA0 * 65536.0f).toInt

      val c0 = compute(
        filter,
        0,
        envelopeFactor,
        floatCoefs,
        feedforwardCoefs,
        floatInvA0
      )
      val c1 =
        compute(filter, 1, envelopeFactor, floatCoefs, feedbackCoefs, 1.0f)
      (c0, c1, inverseA0)

    var (count0, count1, inverseA0) = updateCoefficients()

    var i = 0
    while i < sampleCount do
      val nextChunk = math.min(i + FilterUpdateRate, sampleCount)
      val chunkEnd =
        if nextChunk < sampleCount - count0 then nextChunk else sampleCount

      while i < chunkEnd do
        val inputIdx = i + count0
        val x_curr =
          if inputIdx < sampleCount then input(inputIdx).toLong else 0L
        output = (x_curr * inverseA0) >> 16

        var k = 0
        while k < count0 do
          val ffIdx = i + count0 - k - 1
          if ffIdx < sampleCount then
            output += (input(ffIdx).toLong * feedforwardCoefs(k)) >> 16
          k += 1

        k = 0
        while k < count1 do
          val fbIdx = i - k - 1
          if fbIdx >= 0 then
            output -= (buffer(fbIdx).toLong * feedbackCoefs(k)) >> 16
          k += 1

        buffer(i) = output.toInt

        if envelopeEval.isDefined then
          t = envelopeEval.get.evaluate(sampleCount)
          envelopeFactor = t / 65536.0f

        i += 1

      if i < sampleCount then
        val res = updateCoefficients()
        count0 = res._1
        count1 = res._2
        inverseA0 = res._3

  private def compute(
      filter: Filter,
      dir: Int,
      envelopeFactor: Float,
      fCoef: Array[Array[Float]],
      iCoef: Array[Int],
      floatInvA0: Float
  ): Int =
    val pairCount = filter.pairCounts(dir)
    if pairCount == 0 then return 0

    val amp = getAmplitude(filter, dir, 0, envelopeFactor)
    val phase = calculatePhase(filter, dir, 0, envelopeFactor)
    val cosPhase = Math.cos(phase).toFloat

    fCoef(dir)(0) = -2.0f * amp * cosPhase
    fCoef(dir)(1) = amp * amp

    for p <- 1 until pairCount do
      val ampP = getAmplitude(filter, dir, p, envelopeFactor)
      val phaseP = calculatePhase(filter, dir, p, envelopeFactor)
      val cosPhaseP = Math.cos(phaseP).toFloat

      val term1 = -2.0f * ampP * cosPhaseP
      val term2 = ampP * ampP

      // Cascade/Convolve coefficients
      fCoef(dir)(p * 2 + 1) = fCoef(dir)(p * 2 - 1) * term2
      fCoef(dir)(p * 2) =
        fCoef(dir)(p * 2 - 1) * term1 + fCoef(dir)(p * 2 - 2) * term2

      for k <- (p * 2 - 1) to 2 by -1 do
        fCoef(dir)(k) += fCoef(dir)(k - 1) * term1 + fCoef(dir)(k - 2) * term2

      fCoef(dir)(1) += fCoef(dir)(0) * term1 + term2
      fCoef(dir)(0) += term1

    if dir == 0 then for k <- 0 until pairCount * 2 do fCoef(0)(k) *= floatInvA0

    for k <- 0 until pairCount * 2 do
      iCoef(k) = (fCoef(dir)(k) * 65536.0f).toInt

    pairCount * 2

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
    1.0f - Math.pow(10.0, -dbValue / 20.0).toFloat

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
    // 32.7032 Hz ~ C1
    val frequencyHz = Math.pow(2.0, pow2Value) * 32.703197
    (frequencyHz * 2.0 * Math.PI / Constants.SampleRate).toFloat
