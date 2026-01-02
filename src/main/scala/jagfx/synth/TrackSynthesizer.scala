package jagfx.synth

import jagfx.model._
import jagfx.Constants

/** Orchestrates synthesis of multiple tones with loop expansion. */
object TrackSynthesizer:
  private val SampleRate = Constants.SampleRate

  /** Synthesizes complete `SynthFile` into audio samples. Mixes all active
    * tones and expands loop region if `loopCount > 1`.
    */
  def synthesize(file: SynthFile, loopCount: Int): AudioBuffer =
    var maxDuration = 0
    for (_, tone) <- file.activeTones do
      val endTime = tone.duration + tone.start
      if endTime > maxDuration then maxDuration = endTime

    if maxDuration == 0 then
      scribe.warn("No active tone(s) to synthesize")
      return AudioBuffer.empty(0)

    val sampleCount = maxDuration * SampleRate / 1000
    val loopStart = file.loop.begin * SampleRate / 1000
    val loopStop = file.loop.end * SampleRate / 1000

    val effectiveLoopCount =
      if loopStart < 0 || loopStop < 0 || loopStop > sampleCount || loopStart >= loopStop
      then
        if file.loop.begin != 0 || file.loop.end != 0 then
          scribe.warn(
            s"Invalid loop region ${file.loop.begin}->${file.loop.end}, ignoring..."
          )
        0
      else loopCount

    val totalSampleCount =
      sampleCount + (loopStop - loopStart) * math.max(0, effectiveLoopCount - 1)
    val buffer = Array.fill(totalSampleCount)(0)

    scribe.debug(
      s"Mixing ${file.activeTones.size} tone(s) into $totalSampleCount sample(s)..."
    )

    for (idx, tone) <- file.activeTones do
      scribe.debug(
        s"Synthesizing tone $idx: ${tone.duration}ms @ ${tone.start}ms offset..."
      )
      val toneBuffer = ToneSynthesizer.synthesize(tone)
      val startOffset = tone.start * SampleRate / 1000
      for i <- 0 until toneBuffer.length do
        val pos = i + startOffset
        if pos >= 0 && pos < sampleCount then
          buffer(pos) += toneBuffer.samples(i)

    if effectiveLoopCount > 1 then
      scribe.debug(
        s"Applying loop expansion: $effectiveLoopCount iteration(s)..."
      )
      val endOffset = totalSampleCount - sampleCount
      for sample <- (sampleCount - 1) to loopStop by -1 do
        buffer(sample + endOffset) = buffer(sample)

      for loop <- 1 until effectiveLoopCount do
        val offset = (loopStop - loopStart) * loop
        for sample <- loopStart until loopStop do
          buffer(sample + offset) = buffer(sample)

    for i <- 0 until totalSampleCount do
      if buffer(i) < Constants.Int16.Min then buffer(i) = Constants.Int16.Min
      if buffer(i) > Constants.Int16.Max then buffer(i) = Constants.Int16.Max

    AudioBuffer(buffer, SampleRate)
