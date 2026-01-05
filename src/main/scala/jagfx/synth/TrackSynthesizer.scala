package jagfx.synth

import jagfx.model._
import jagfx.constants
import jagfx.utils.MathUtils.clipInt16

/** Orchestrates synthesis of multiple tones with loop expansion. */
object TrackSynthesizer:
  /** Synthesizes complete `SynthFile` into audio samples. Mixes all active
    * tones and expands loop region `if loopCount > 1.
    */
  def synthesize(
      file: SynthFile,
      loopCount: Int,
      toneFilter: Int = -1
  ): AudioBuffer =
    val tonesToMix =
      if toneFilter < 0 then file.activeTones
      else file.activeTones.filter(_._1 == toneFilter)
    val maxDuration = _calculateMaxDurationFiltered(tonesToMix)
    if maxDuration == 0 then return AudioBuffer.empty(0)

    val sampleCount = maxDuration * constants.SampleRate / 1000
    val loopStart = file.loop.begin * constants.SampleRate / 1000
    val loopStop = file.loop.end * constants.SampleRate / 1000

    val effectiveLoopCount =
      _validateLoopRegion(file, sampleCount, loopStart, loopStop, loopCount)
    val totalSampleCount =
      sampleCount + (loopStop - loopStart) * math.max(0, effectiveLoopCount - 1)

    val buffer = _mixTonesFiltered(tonesToMix, sampleCount, totalSampleCount)
    if effectiveLoopCount > 1 then
      _applyLoopExpansion(
        buffer,
        sampleCount,
        loopStart,
        loopStop,
        effectiveLoopCount
      )
    clipInt16(buffer)

    val output = new Array[Int](totalSampleCount)
    System.arraycopy(buffer, 0, output, 0, totalSampleCount)
    BufferPool.release(buffer)

    AudioBuffer(output, constants.SampleRate)

  private def _calculateMaxDuration(file: SynthFile): Int =
    var maxDuration = 0
    for (_, tone) <- file.activeTones do
      val endTime = tone.duration + tone.start
      if endTime > maxDuration then maxDuration = endTime
    maxDuration

  private def _calculateMaxDurationFiltered(tones: Vector[(Int, Tone)]): Int =
    var maxDuration = 0
    for (_, tone) <- tones do
      val endTime = tone.duration + tone.start
      if endTime > maxDuration then maxDuration = endTime
    maxDuration

  private def _validateLoopRegion(
      file: SynthFile,
      sampleCount: Int,
      loopStart: Int,
      loopStop: Int,
      loopCount: Int
  ): Int =
    if loopStart < 0 || loopStop < 0 || loopStop > sampleCount || loopStart >= loopStop
    then
      if file.loop.begin != 0 || file.loop.end != 0 then
        scribe.warn(
          s"Invalid loop region ${file.loop.begin}->${file.loop.end}, ignoring..."
        )
      0
    else loopCount

  private def _mixTonesFiltered(
      tones: Vector[(Int, Tone)],
      sampleCount: Int,
      totalSampleCount: Int
  ): Array[Int] =
    val buffer = BufferPool.acquire(totalSampleCount)
    for (idx, tone) <- tones do
      val toneBuffer = ToneSynthesizer.synthesize(tone)
      val startOffset = tone.start * constants.SampleRate / 1000
      for i <- 0 until toneBuffer.length do
        val pos = i + startOffset
        if pos >= 0 && pos < sampleCount then
          buffer(pos) += toneBuffer.samples(i)
    buffer

  private def _applyLoopExpansion(
      buffer: Array[Int],
      sampleCount: Int,
      loopStart: Int,
      loopStop: Int,
      loopCount: Int
  ): Unit =
    val totalSampleCount = buffer.length
    val endOffset = totalSampleCount - sampleCount
    for sample <- (sampleCount - 1) to loopStop by -1 do
      buffer(sample + endOffset) = buffer(sample)

    for loop <- 1 until loopCount do
      val offset = (loopStop - loopStart) * loop
      for sample <- loopStart until loopStop do
        buffer(sample + offset) = buffer(sample)
