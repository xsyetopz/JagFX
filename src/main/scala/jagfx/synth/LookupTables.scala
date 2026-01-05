package jagfx.synth

import jagfx.utils.MathUtils
import java.util.SplittableRandom
import jagfx.Constants.{Int16, SemitoneRange, SinTableDivisor, CircleSegments}

/** Precomputed lookup tables for DSP and rendering operations. */
object LookupTables:
  val SemitoneBase = 1.0057929410678534

  /** Noise table with deterministic random `-1`/`+1` values. */
  lazy val noise: Array[Int] =
    val rng = new SplittableRandom(0xdeadbeef)
    Array.tabulate(Int16.UnsignedMaxValue)(_ =>
      if rng.nextBoolean() then 1 else -1
    )

  /** Sine table with `16384`-amplitude range. */
  lazy val sin: Array[Int] =
    Array.tabulate(Int16.UnsignedMaxValue)(i =>
      (math.sin(i / SinTableDivisor) * Int16.Quarter).toInt
    )

  /** Semitone multipliers mapping index `0-240` to semitones `-120` to `+120`.
    */
  private lazy val _semitoneCache: Array[Double] =
    Array.tabulate(241)(i => math.pow(SemitoneBase, i - SemitoneRange))

  /** Returns multiplier for given semitone offset. Uses cache for
    * `[-120, 120]`, `math.pow` otherwise.
    */
  def getSemitoneMultiplier(semitone: Int): Double =
    if semitone >= -SemitoneRange && semitone <= SemitoneRange then
      _semitoneCache(semitone + SemitoneRange)
    else math.pow(SemitoneBase, semitone.toDouble)

  /** Unit circle X coordinates for `64`-segment rendering. */
  lazy val unitCircleX: Array[Double] =
    Array.tabulate(CircleSegments + 1)(i =>
      math.cos(i * MathUtils.TwoPi / CircleSegments)
    )

  /** Unit circle Y coordinates for `64`-segment rendering. */
  lazy val unitCircleY: Array[Double] =
    Array.tabulate(CircleSegments + 1)(i =>
      math.sin(i * MathUtils.TwoPi / CircleSegments)
    )
