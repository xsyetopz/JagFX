package jagfx.synth

import jagfx.Constants
import jagfx.Constants.Int16
import jagfx.Constants.NoisePhaseDiv
import jagfx.Constants.PhaseMask
import jagfx.Constants.PhaseScale
import jagfx.model.*
import jagfx.utils.MathUtils.clipInt16

/** Synthesizes single `Voice` into audio samples using FM/AM modulation. */
object VoiceSynthesizer:
  /** Generates audio samples from `Voice` definition. */
  def synthesize(voice: Voice): AudioBuffer =
    val sampleCount = voice.duration * Constants.SampleRate / 1000
    if sampleCount <= 0 || voice.duration < 10 then return AudioBuffer.empty(0)

    val samplesPerStep = sampleCount.toDouble / voice.duration.toDouble
    val buffer = BufferPool.acquire(sampleCount)

    val state = initSynthState(voice, samplesPerStep)
    renderSamples(buffer, voice, state, sampleCount)

    applyGating(buffer, voice, sampleCount)
    applyEcho(buffer, voice, samplesPerStep, sampleCount)

    voice.filter.foreach { f =>
      FilterSynthesizer.apply(buffer, f, sampleCount)
    }

    clipInt16(buffer, sampleCount)

    val output = new Array[Int](sampleCount)
    System.arraycopy(buffer, 0, output, 0, sampleCount)
    BufferPool.release(buffer)
    AudioBuffer(output, Constants.SampleRate)

  // Types
  private case class SynthState(
      freqBaseEval: EnvelopeEvaluator,
      ampBaseEval: EnvelopeEvaluator,
      freqModRateEval: Option[EnvelopeEvaluator],
      freqModRangeEval: Option[EnvelopeEvaluator],
      ampModRateEval: Option[EnvelopeEvaluator],
      ampModRangeEval: Option[EnvelopeEvaluator],
      frequencyStart: Int,
      frequencyDuration: Int,
      amplitudeStart: Int,
      amplitudeDuration: Int,
      partialDelays: Array[Int],
      partialVolumes: Array[Int],
      partialSemitones: Array[Int],
      partialStarts: Array[Int]
  )

  private def initSynthState(voice: Voice, samplesPerStep: Double): SynthState =
    val freqBaseEval = EnvelopeEvaluator(voice.pitchEnvelope)
    val ampBaseEval = EnvelopeEvaluator(voice.volumeEnvelope)
    freqBaseEval.reset()
    ampBaseEval.reset()

    val (freqModRateEval, freqModRangeEval, vibratoLfoIncr, vibratoLfoBase) =
      initFrequencyModulation(voice, samplesPerStep)

    val (ampModRateEval, ampModRangeEval, amplitudeStart, amplitudeDuration) =
      initAmplitudeModulation(voice, samplesPerStep)

    val (delays, volumes, semitones, starts) =
      initPartials(voice, samplesPerStep)

    SynthState(
      freqBaseEval,
      ampBaseEval,
      freqModRateEval,
      freqModRangeEval,
      ampModRateEval,
      ampModRangeEval,
      vibratoLfoIncr,
      vibratoLfoBase,
      amplitudeStart,
      amplitudeDuration,
      delays,
      volumes,
      semitones,
      starts
    )

  private def initFrequencyModulation(
      voice: Voice,
      samplesPerStep: Double
  ): (Option[EnvelopeEvaluator], Option[EnvelopeEvaluator], Int, Int) =
    voice.vibratoRate match
      case Some(env) =>
        val rateEval = EnvelopeEvaluator(env)
        val rangeEval = voice.vibratoDepth.map(EnvelopeEvaluator(_))
        rateEval.reset()
        rangeEval.foreach(_.reset())
        val start = ((env.end - env.start) * PhaseScale / samplesPerStep).toInt
        val duration = (env.start * PhaseScale / samplesPerStep).toInt
        (Some(rateEval), rangeEval, start, duration)
      case None =>
        (None, None, 0, 0)

  private def initAmplitudeModulation(
      voice: Voice,
      samplesPerStep: Double
  ): (Option[EnvelopeEvaluator], Option[EnvelopeEvaluator], Int, Int) =
    voice.tremoloRate match
      case Some(env) =>
        val rateEval = EnvelopeEvaluator(env)
        val rangeEval = voice.tremoloDepth.map(EnvelopeEvaluator(_))
        rateEval.reset()
        rangeEval.foreach(_.reset())
        val start = ((env.end - env.start) * PhaseScale / samplesPerStep).toInt
        val duration = (env.start * PhaseScale / samplesPerStep).toInt
        (Some(rateEval), rangeEval, start, duration)
      case None =>
        (None, None, 0, 0)

  private def initPartials(
      voice: Voice,
      samplesPerStep: Double
  ): (Array[Int], Array[Int], Array[Int], Array[Int]) =
    val delays = new Array[Int](Constants.MaxPartials)
    val volumes = new Array[Int](Constants.MaxPartials)
    val semitones = new Array[Int](Constants.MaxPartials)
    val starts = new Array[Int](Constants.MaxPartials)

    for partial <- 0 until math.min(
        Constants.MaxPartials,
        voice.partials.length
      )
    do
      val height = voice.partials(partial)
      if height.volume.value != 0 then
        delays(partial) = (height.startDelay.value * samplesPerStep).toInt
        volumes(partial) = (height.volume.value << 14) / 100
        semitones(partial) =
          ((voice.pitchEnvelope.end - voice.pitchEnvelope.start) * PhaseScale *
            LookupTables.getPitchMultiplier(
              height.pitchOffset
            ) / samplesPerStep).toInt
        starts(partial) =
          (voice.pitchEnvelope.start * PhaseScale / samplesPerStep).toInt

    (delays, volumes, semitones, starts)

  private def renderSamples(
      buffer: Array[Int],
      voice: Voice,
      state: SynthState,
      sampleCount: Int
  ): Unit =
    val phases = new Array[Int](Constants.MaxPartials)
    var frequencyPhase = 0
    var amplitudePhase = 0

    for sample <- 0 until sampleCount do
      var frequency = state.freqBaseEval.evaluate(sampleCount)
      var amplitude = state.ampBaseEval.evaluate(sampleCount)

      val (newFreq, newFreqPhase) =
        applyVibrato(frequency, frequencyPhase, sampleCount, state, voice)
      frequency = newFreq
      frequencyPhase = newFreqPhase
      val (newAmp, newAmpPhase) =
        applyTremolo(amplitude, amplitudePhase, sampleCount, state, voice)
      amplitude = newAmp
      amplitudePhase = newAmpPhase

      renderPartials(
        buffer,
        voice,
        state,
        sample,
        sampleCount,
        frequency,
        amplitude,
        phases
      )

  private def applyVibrato(
      frequency: Int,
      phase: Int,
      sampleCount: Int,
      state: SynthState,
      voice: Voice
  ): (Int, Int) =
    (state.freqModRateEval, state.freqModRangeEval) match
      case (Some(rateEval), Some(rangeEval)) =>
        val rate = rateEval.evaluate(sampleCount)
        val range = rangeEval.evaluate(sampleCount)
        val mod =
          generateSample(range, phase, voice.vibratoRate.get.waveform) >> 1
        val nextPhase =
          phase + (rate * state.frequencyStart >> 16) + state.frequencyDuration
        (frequency + mod, nextPhase)
      case _ => (frequency, phase)

  private def applyTremolo(
      amplitude: Int,
      phase: Int,
      sampleCount: Int,
      state: SynthState,
      voice: Voice
  ): (Int, Int) =
    (state.ampModRateEval, state.ampModRangeEval) match
      case (Some(rateEval), Some(rangeEval)) =>
        val rate = rateEval.evaluate(sampleCount)
        val range = rangeEval.evaluate(sampleCount)
        val mod =
          generateSample(range, phase, voice.tremoloRate.get.waveform) >> 1
        val newAmp = amplitude * (mod + Constants.Int16.UnsignedMaxValue) >> 15
        val nextPhase =
          phase + (rate * state.amplitudeStart >> 16) + state.amplitudeDuration
        (newAmp, nextPhase)
      case _ => (amplitude, phase)

  private def renderPartials(
      buffer: Array[Int],
      voice: Voice,
      state: SynthState,
      sample: Int,
      sampleCount: Int,
      frequency: Int,
      amplitude: Int,
      phases: Array[Int]
  ): Unit =
    for partial <- 0 until math.min(
        Constants.MaxPartials,
        voice.partials.length
      )
    do
      if voice.partials(partial).volume.value != 0 then
        val position = sample + state.partialDelays(partial)
        if position >= 0 && position < sampleCount then
          buffer(position) += generateSample(
            amplitude * state.partialVolumes(partial) >> 15,
            phases(partial),
            voice.pitchEnvelope.waveform
          )
          phases(partial) += (frequency * state.partialSemitones(
            partial
          ) >> 16) +
            state.partialStarts(partial)

  private def generateSample(
      amplitude: Int,
      phase: Int,
      waveform: Waveform
  ): Int =
    waveform match
      case Waveform.Square =>
        if (phase & PhaseMask) < Int16.Quarter then amplitude else -amplitude
      case Waveform.Sine =>
        (LookupTables.sin(phase & PhaseMask) * amplitude) >> 14
      case Waveform.Saw =>
        (((phase & PhaseMask) * amplitude) >> 14) - amplitude
      case Waveform.Noise =>
        LookupTables.noise((phase / NoisePhaseDiv) & PhaseMask) * amplitude
      case Waveform.Off => 0

  private def applyGating(
      buffer: Array[Int],
      voice: Voice,
      sampleCount: Int
  ): Unit =
    (voice.gateSilence, voice.gateDuration) match
      case (Some(silence), Some(duration)) =>
        val silenceEval = EnvelopeEvaluator(silence)
        val durationEval = EnvelopeEvaluator(duration)
        silenceEval.reset()
        durationEval.reset()
        var counter = 0
        var muted = true
        for sample <- 0 until sampleCount do
          val stepOn = silenceEval.evaluate(sampleCount)
          val stepOff = durationEval.evaluate(sampleCount)
          val threshold =
            if muted then
              silence.start + ((silence.end - silence.start) * stepOn >> 8)
            else silence.start + ((silence.end - silence.start) * stepOff >> 8)
          counter += 256
          if counter >= threshold then
            counter = 0
            muted = !muted
          if muted then buffer(sample) = 0
      case _ => ()

  private def applyEcho(
      buffer: Array[Int],
      voice: Voice,
      samplesPerStep: Double,
      sampleCount: Int
  ): Unit =
    if voice.echoDelay > 0 && voice.echoMix > 0 then
      val start = (voice.echoDelay * samplesPerStep).toInt
      for sample <- start until sampleCount do
        buffer(sample) += buffer(sample - start) * voice.echoMix / 100
