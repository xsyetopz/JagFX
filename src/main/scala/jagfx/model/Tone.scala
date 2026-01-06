package jagfx.model

/** Single instrument voice within `.synth` file. Contains all envelopes,
  * partials, and timing parameters needed to generate audio.
  *
  * @param pitchEnvelope
  *   Fundamental frequency trajectory
  * @param volumeEnvelope
  *   Amplitude trajectory
  * @param vibratoRate
  *   Optional: frequency modulation rate envelope
  * @param vibratoDepth
  *   Optional: frequency modulation depth envelope
  * @param tremoloRate
  *   Optional: amplitude modulation rate envelope
  * @param tremoloDepth
  *   Optional: amplitude modulation depth envelope
  * @param gateSilence
  *   Optional: gate off (silence) envelope
  * @param gateDuration
  *   Optional: gate on (duration) envelope
  * @param partials
  *   Additive synthesis partials (overtones)
  * @param echoDelay
  *   Echo delay in milliseconds
  * @param echoMix
  *   Echo mix level `0-100`
  * @param duration
  *   Total voice length in milliseconds
  * @param start
  *   Offset from track start in milliseconds
  * @param filter
  *   Optional: IIR filter parameters
  */
case class Voice(
    pitchEnvelope: Envelope,
    volumeEnvelope: Envelope,
    vibratoRate: Option[Envelope],
    vibratoDepth: Option[Envelope],
    tremoloRate: Option[Envelope],
    tremoloDepth: Option[Envelope],
    gateSilence: Option[Envelope],
    gateDuration: Option[Envelope],
    partials: Vector[Partial],
    echoDelay: Int,
    echoMix: Int,
    duration: Int,
    start: Int,
    filter: Option[Filter] = None
)
