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

/** Top-level `.synth` file representation containing up to `10` voices.
  *
  * @param voices
  *   Vector of optional voices (indices `0-9`)
  * @param loop
  *   Loop region parameters
  */
case class SynthFile(
    voices: Vector[Option[Voice]],
    loop: LoopParams,
    warnings: List[String] = Nil
):
  /** Returns active voices with their indices. */
  def activeVoices: Vector[(Int, Voice)] =
    voices.zipWithIndex.collect { case (Some(voice), idx) => (idx, voice) }
