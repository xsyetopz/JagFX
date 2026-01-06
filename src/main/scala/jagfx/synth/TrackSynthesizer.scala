package jagfx.synth

import jagfx.Constants
import jagfx.model.*
import jagfx.utils.MathUtils.clipInt16

/** Orchestrates synthesis of multiple voices with loop expansion. */
object TrackSynthesizer:
  /** Synthesizes complete `SynthFile` into audio samples. */
  def synthesize(
      file: SynthFile,
      loopCount: Int,
      voiceFilter: Int = -1
  ): AudioBuffer =
    val voicesToMix =
      if voiceFilter < 0 then file.activeVoices
      else file.activeVoices.filter(_._1 == voiceFilter)
    val maxDuration = calculateMaxDuration(voicesToMix)
    if maxDuration == 0 then return AudioBuffer.empty(0)

    val sampleCount = maxDuration * Constants.SampleRate / 1000
    val loopStart = file.loop.begin * Constants.SampleRate / 1000
    val loopStop = file.loop.end * Constants.SampleRate / 1000

    val effectiveLoopCount =
      validateLoopRegion(file, sampleCount, loopStart, loopStop, loopCount)
    val totalSampleCount =
      sampleCount + (loopStop - loopStart) * math.max(0, effectiveLoopCount - 1)

    val buffer = mixVoices(voicesToMix, sampleCount, totalSampleCount)
    if effectiveLoopCount > 1 then
      applyLoopExpansion(
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

    AudioBuffer(output, Constants.SampleRate)

  private def calculateMaxDuration(voices: Vector[(Int, Voice)]): Int =
    var maxDuration = 0
    for (_, voice) <- voices do
      val endTime = voice.duration + voice.start
      if endTime > maxDuration then maxDuration = endTime
    maxDuration

  private def validateLoopRegion(
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

  private def mixVoices(
      voices: Vector[(Int, Voice)],
      sampleCount: Int,
      totalSampleCount: Int
  ): Array[Int] =
    val buffer = BufferPool.acquire(totalSampleCount)
    for (_, voice) <- voices do
      val voiceBuffer = VoiceSynthesizer.synthesize(voice)
      val startOffset = voice.start * Constants.SampleRate / 1000
      for i <- 0 until voiceBuffer.length do
        val pos = i + startOffset
        if pos >= 0 && pos < sampleCount then
          buffer(pos) += voiceBuffer.samples(i)
    buffer

  private def applyLoopExpansion(
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
