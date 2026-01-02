package jagfx.synth

import jagfx.model._
import jagfx.Constants

private val SemitoneBase: Double = 1.0057929410678534
private val PhaseScale: Double = 32.768
private val NoisePhaseDiv: Int = 2607
private val MaxHarmonics: Int = 5
private val PhaseMask: Int = 0x7fff
private val HalfPhase: Int = 16384

/** Synthesizes single `Tone` into audio samples using FM/AM modulation and
  * additive synthesis.
  */
object ToneSynthesizer:
  private val SampleRate = Constants.SampleRate

  /** Generates audio samples from `Tone` definition. Returns `AudioBuffer` with
    * rendered samples.
    */
  def synthesize(tone: Tone): AudioBuffer =
    val sampleCount = tone.duration * SampleRate / 1000
    if sampleCount <= 0 || tone.duration < 10 then
      scribe.debug(s"Skipping short tone: ${tone.duration}ms...")
      return AudioBuffer.empty(0)

    scribe.debug(
      s"Rendering ${tone.harmonics.length} harmonic(s) over $sampleCount sample(s)..."
    )

    val samplesPerStep = sampleCount.toDouble / tone.duration.toDouble
    val buffer = new Array[Int](sampleCount)

    val state = initializeSynthState(tone, samplesPerStep, sampleCount)
    renderSamples(buffer, tone, state, sampleCount)

    if tone.gateSilence.isDefined then scribe.debug("Applying gate effect...")
    applyGating(buffer, tone, sampleCount)

    if tone.reverbDelay > 0 && tone.reverbVolume > 0 then
      scribe.debug(
        s"Applying reverb: delay=${tone.reverbDelay}ms, volume=${tone.reverbVolume}%..."
      )
    applyReverb(buffer, tone, samplesPerStep, sampleCount)

    clipBuffer(buffer, sampleCount)

    AudioBuffer(buffer, SampleRate)

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
      harmonicDelays: Array[Int],
      harmonicVolumes: Array[Int],
      harmonicSemitones: Array[Int],
      harmonicStarts: Array[Int]
  )

  private def initializeSynthState(
      tone: Tone,
      samplesPerStep: Double,
      sampleCount: Int
  ): SynthState =
    val freqBaseEval = EnvelopeEvaluator(tone.pitchEnvelope)
    val ampBaseEval = EnvelopeEvaluator(tone.volumeEnvelope)
    freqBaseEval.reset()
    ampBaseEval.reset()

    val (freqModRateEval, freqModRangeEval, frequencyStart, frequencyDuration) =
      initializeFrequencyModulation(tone, samplesPerStep)

    val (ampModRateEval, ampModRangeEval, amplitudeStart, amplitudeDuration) =
      initializeAmplitudeModulation(tone, samplesPerStep)

    val (delays, volumes, semitones, starts) =
      initializeHarmonics(tone, samplesPerStep)

    SynthState(
      freqBaseEval,
      ampBaseEval,
      freqModRateEval,
      freqModRangeEval,
      ampModRateEval,
      ampModRangeEval,
      frequencyStart,
      frequencyDuration,
      amplitudeStart,
      amplitudeDuration,
      delays,
      volumes,
      semitones,
      starts
    )

  private def initializeFrequencyModulation(
      tone: Tone,
      samplesPerStep: Double
  ): (Option[EnvelopeEvaluator], Option[EnvelopeEvaluator], Int, Int) =
    tone.vibratoRate match
      case Some(env) =>
        val rateEval = EnvelopeEvaluator(env)
        val rangeEval = tone.vibratoDepth.map(EnvelopeEvaluator(_))
        rateEval.reset()
        rangeEval.foreach(_.reset())
        val start = ((env.end - env.start) * PhaseScale / samplesPerStep).toInt
        val duration = (env.start * PhaseScale / samplesPerStep).toInt
        (Some(rateEval), rangeEval, start, duration)
      case None =>
        (None, None, 0, 0)

  private def initializeAmplitudeModulation(
      tone: Tone,
      samplesPerStep: Double
  ): (Option[EnvelopeEvaluator], Option[EnvelopeEvaluator], Int, Int) =
    tone.tremoloRate match
      case Some(env) =>
        val rateEval = EnvelopeEvaluator(env)
        val rangeEval = tone.tremoloDepth.map(EnvelopeEvaluator(_))
        rateEval.reset()
        rangeEval.foreach(_.reset())
        val start = ((env.end - env.start) * PhaseScale / samplesPerStep).toInt
        val duration = (env.start * PhaseScale / samplesPerStep).toInt
        (Some(rateEval), rangeEval, start, duration)
      case None =>
        (None, None, 0, 0)

  private def initializeHarmonics(
      tone: Tone,
      samplesPerStep: Double
  ): (Array[Int], Array[Int], Array[Int], Array[Int]) =
    val delays = new Array[Int](MaxHarmonics)
    val volumes = new Array[Int](MaxHarmonics)
    val semitones = new Array[Int](MaxHarmonics)
    val starts = new Array[Int](MaxHarmonics)

    for harmonic <- 0 until math.min(MaxHarmonics, tone.harmonics.length) do
      val h = tone.harmonics(harmonic)
      if h.volume != 0 then
        delays(harmonic) = (h.delay * samplesPerStep).toInt
        volumes(harmonic) = (h.volume << 14) / 100
        semitones(harmonic) =
          ((tone.pitchEnvelope.end - tone.pitchEnvelope.start) * PhaseScale * math
            .pow(
              SemitoneBase,
              h.semitone
            ) / samplesPerStep).toInt
        starts(harmonic) =
          (tone.pitchEnvelope.start * PhaseScale / samplesPerStep).toInt

    (delays, volumes, semitones, starts)

  private def renderSamples(
      buffer: Array[Int],
      tone: Tone,
      state: SynthState,
      sampleCount: Int
  ): Unit =
    import Constants._
    val phases = new Array[Int](MaxHarmonics)
    var frequencyPhase = 0
    var amplitudePhase = 0

    for sample <- 0 until sampleCount do
      var frequency = state.freqBaseEval.evaluate(sampleCount)
      var amplitude = state.ampBaseEval.evaluate(sampleCount)

      (state.freqModRateEval, state.freqModRangeEval) match
        case (Some(rateEval), Some(rangeEval)) =>
          val rate = rateEval.evaluate(sampleCount)
          val range = rangeEval.evaluate(sampleCount)
          frequency += generateSample(
            range,
            frequencyPhase,
            tone.vibratoRate.get.form
          ) >> 1
          frequencyPhase += (rate * state.frequencyStart >> 16) + state.frequencyDuration
        case _ => ()

      (state.ampModRateEval, state.ampModRangeEval) match
        case (Some(rateEval), Some(rangeEval)) =>
          val rate = rateEval.evaluate(sampleCount)
          val range = rangeEval.evaluate(sampleCount)
          amplitude = amplitude * ((generateSample(
            range,
            amplitudePhase,
            tone.tremoloRate.get.form
          ) >> 1) + Int16.UnsignedMid) >> 15
          amplitudePhase += (rate * state.amplitudeStart >> 16) + state.amplitudeDuration
        case _ => ()

      renderHarmonics(
        buffer,
        tone,
        state,
        sample,
        sampleCount,
        frequency,
        amplitude,
        phases
      )

  private def renderHarmonics(
      buffer: Array[Int],
      tone: Tone,
      state: SynthState,
      sample: Int,
      sampleCount: Int,
      frequency: Int,
      amplitude: Int,
      phases: Array[Int]
  ): Unit =
    for harmonic <- 0 until math.min(MaxHarmonics, tone.harmonics.length) do
      if tone.harmonics(harmonic).volume != 0 then
        val position = sample + state.harmonicDelays(harmonic)
        if position >= 0 && position < sampleCount then
          buffer(position) += generateSample(
            amplitude * state.harmonicVolumes(harmonic) >> 15,
            phases(harmonic),
            tone.pitchEnvelope.form
          )
          phases(harmonic) += (frequency * state.harmonicSemitones(
            harmonic
          ) >> 16) + state.harmonicStarts(harmonic)

  private def generateSample(amplitude: Int, phase: Int, form: WaveForm): Int =
    form match
      case WaveForm.Square =>
        if (phase & PhaseMask) < HalfPhase then amplitude else -amplitude
      case WaveForm.Sine =>
        (WaveTables.sin(phase & PhaseMask) * amplitude) >> 14
      case WaveForm.Saw => (((phase & PhaseMask) * amplitude) >> 14) - amplitude
      case WaveForm.Noise =>
        WaveTables.noise((phase / NoisePhaseDiv) & PhaseMask) * amplitude
      case WaveForm.Off => 0

  private def applyGating(
      buffer: Array[Int],
      tone: Tone,
      sampleCount: Int
  ): Unit =
    (tone.gateSilence, tone.gateDuration) match
      case (Some(release), Some(attack)) =>
        val releaseEval = EnvelopeEvaluator(release)
        val attackEval = EnvelopeEvaluator(attack)
        releaseEval.reset()
        attackEval.reset()
        var counter = 0
        var muted = true
        for sample <- 0 until sampleCount do
          val releaseValue = releaseEval.evaluate(sampleCount)
          val attackValue = attackEval.evaluate(sampleCount)
          val threshold =
            if muted then
              release.start + ((release.end - release.start) * releaseValue >> 8)
            else
              release.start + ((release.end - release.start) * attackValue >> 8)
          counter += 256
          if counter >= threshold then
            counter = 0
            muted = !muted
          if muted then buffer(sample) = 0
      case _ => ()

  private def applyReverb(
      buffer: Array[Int],
      tone: Tone,
      samplesPerStep: Double,
      sampleCount: Int
  ): Unit =
    if tone.reverbDelay > 0 && tone.reverbVolume > 0 then
      val start = (tone.reverbDelay * samplesPerStep).toInt
      for sample <- start until sampleCount do
        buffer(sample) += buffer(sample - start) * tone.reverbVolume / 100

  private def clipBuffer(buffer: Array[Int], sampleCount: Int): Unit =
    import Constants._
    for sample <- 0 until sampleCount do
      if buffer(sample) < Int16.Min then buffer(sample) = Int16.Min
      if buffer(sample) > Int16.Max then buffer(sample) = Int16.Max
