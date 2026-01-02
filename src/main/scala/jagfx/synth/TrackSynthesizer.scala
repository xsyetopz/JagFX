package jagfx.synth

import jagfx.model._
import jagfx.Constants

/** Orchestrates synthesis of multiple tones with loop expansion. */
object TrackSynthesizer:
  /** Synthesizes complete `SynthFile` into audio samples. Mixes all active
    * tones and expands loop region if `loopCount` > `1`.
    */
  def synthesize(file: SynthFile, loopCount: Int): AudioBuffer =
    val maxDuration = calculateMaxDuration(file)
    if maxDuration == 0 then
      scribe.warn("No active tone(s) to synthesize")
      return AudioBuffer.empty(0)

    val sampleCount = maxDuration * Constants.SampleRate / 1000
    val loopStart = file.loop.begin * Constants.SampleRate / 1000
    val loopStop = file.loop.end * Constants.SampleRate / 1000

    val effectiveLoopCount =
      validateLoopRegion(file, sampleCount, loopStart, loopStop, loopCount)
    val totalSampleCount =
      sampleCount + (loopStop - loopStart) * math.max(0, effectiveLoopCount - 1)

    scribe.debug(
      s"Mixing ${file.activeTones.size} tone(s) into $totalSampleCount sample(s)..."
    )

    val buffer = mixTones(file, sampleCount, totalSampleCount)
    if effectiveLoopCount > 1 then
      applyLoopExpansion(
        buffer,
        sampleCount,
        loopStart,
        loopStop,
        effectiveLoopCount
      )
    clipBuffer(buffer)

    AudioBuffer(buffer, Constants.SampleRate)

  private def calculateMaxDuration(file: SynthFile): Int =
    var maxDuration = 0
    for (_, tone) <- file.activeTones do
      val endTime = tone.duration + tone.start
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

  private def mixTones(
      file: SynthFile,
      sampleCount: Int,
      totalSampleCount: Int
  ): Array[Int] =
    val buffer = Array.fill(totalSampleCount)(0)
    for (idx, tone) <- file.activeTones do
      scribe.debug(
        s"Synthesizing tone $idx: ${tone.duration}ms @ ${tone.start}ms offset..."
      )
      val toneBuffer = ToneSynthesizer.synthesize(tone)
      val startOffset = tone.start * Constants.SampleRate / 1000
      for i <- 0 until toneBuffer.length do
        val pos = i + startOffset
        if pos >= 0 && pos < sampleCount then
          buffer(pos) += toneBuffer.samples(i)
    buffer

  private def applyLoopExpansion(
      buffer: Array[Int],
      sampleCount: Int,
      loopStart: Int,
      loopStop: Int,
      loopCount: Int
  ): Unit =
    scribe.debug(s"Applying loop expansion: $loopCount iteration(s)...")
    val totalSampleCount = buffer.length
    val endOffset = totalSampleCount - sampleCount
    for sample <- (sampleCount - 1) to loopStop by -1 do
      buffer(sample + endOffset) = buffer(sample)

    for loop <- 1 until loopCount do
      val offset = (loopStop - loopStart) * loop
      for sample <- loopStart until loopStop do
        buffer(sample + offset) = buffer(sample)

  private def clipBuffer(buffer: Array[Int]): Unit =
    import Constants._
    for i <- buffer.indices do
      if buffer(i) < Int16.Min then buffer(i) = Int16.Min
      if buffer(i) > Int16.Max then buffer(i) = Int16.Max
