package jagfx.model

/** Single instrument voice within `.synth` file. Contains all envelopes,
  * harmonics, and timing parameters needed to generate audio.
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
  *   Optional: gate off (release) envelope
  * @param gateDuration
  *   Optional: gate on (attack) envelope
  * @param harmonics
  *   Additive synthesis partials (overtones)
  * @param reverbDelay
  *   Reverb delay in milliseconds
  * @param reverbVolume
  *   Reverb mix level `0`-`100`
  * @param duration
  *   Total tone length in milliseconds
  * @param start
  *   Offset from track start in milliseconds
  */
case class Tone(
    pitchEnvelope: Envelope,
    volumeEnvelope: Envelope,
    vibratoRate: Option[Envelope],
    vibratoDepth: Option[Envelope],
    tremoloRate: Option[Envelope],
    tremoloDepth: Option[Envelope],
    gateSilence: Option[Envelope],
    gateDuration: Option[Envelope],
    harmonics: Vector[Harmonic],
    reverbDelay: Int,
    reverbVolume: Int,
    duration: Int,
    start: Int
)
