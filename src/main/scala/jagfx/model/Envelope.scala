package jagfx.model

/** Single segment within envelope defining duration and peak level. */
case class EnvelopeSegment(duration: Int, peak: Int)

/** Envelope defining amplitude or frequency curve over time.
  *
  * @param form
  *   Waveform type for oscillator (`Sine`, `Square`, `Saw`, `Noise`)
  * @param start
  *   Base value at envelope start
  * @param end
  *   Target value at envelope end
  * @param segments
  *   Interpolation segments between start and end
  */
case class Envelope(
    form: WaveForm,
    start: Int,
    end: Int,
    segments: Vector[EnvelopeSegment]
)
