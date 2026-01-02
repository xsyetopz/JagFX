package jagfx.model

/** Loop region parameters for sample playback.
  *
  * @param begin
  *   Loop start position in milliseconds
  * @param end
  *   Loop end position in milliseconds
  */
case class LoopParams(begin: Int, end: Int):
  /** Returns `true` if loop region is valid (`begin < end`). */
  def isActive: Boolean = begin < end

/** Top-level `.synth` file representation containing up to `10` tones.
  *
  * @param tones
  *   Vector of optional tones (indices `0`-`9`)
  * @param loop
  *   Loop region parameters
  */
case class SynthFile(
    tones: Vector[Option[Tone]],
    loop: LoopParams
):
  /** Returns active tones with their indices. */
  def activeTones: Vector[(Int, Tone)] =
    tones.zipWithIndex.collect { case (Some(tone), idx) => (idx, tone) }
