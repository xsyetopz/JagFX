package jagfx.model

/** Additive synthesis partial (harmonic overtone).
  *
  * @param volume
  *   Amplitude `0`-`100`
  * @param semitone
  *   Pitch offset in decicents (`10` = `1` semitone)
  * @param delay
  *   Phase offset in milliseconds
  */
case class Harmonic(
    volume: Int,
    semitone: Int,
    delay: Int
)
