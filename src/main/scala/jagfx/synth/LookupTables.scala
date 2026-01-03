package jagfx.synth

import jagfx.utils.MathUtils.TwoPi
import java.util.SplittableRandom

private val WaveTableSize = 32768
private val SinTableDivisor = 5215.1903
private val SemitoneBase = 1.0057929410678534
private val CircleSegments = 64

/** Precomputed lookup tables for DSP and rendering operations. */
object LookupTables:
  /** Noise table with deterministic random `-1`/`+1` values. */
  lazy val noise: Array[Int] =
    val rng = new SplittableRandom(0xdeadbeef)
    Array.tabulate(WaveTableSize)(_ => if rng.nextBoolean() then 1 else -1)

  /** Sine table with `16384`-amplitude range. */
  lazy val sin: Array[Int] =
    Array.tabulate(WaveTableSize)(i =>
      (math.sin(i / SinTableDivisor) * 16384.0).toInt
    )

  /** Semitone multipliers mapping index `0-240` to semitones `-120` to `+120`.
    */
  lazy val semitoneMultiplier: Array[Double] =
    Array.tabulate(241)(i => math.pow(SemitoneBase, i - 120))

  /** Unit circle X coordinates for `64`-segment rendering. */
  lazy val unitCircleX: Array[Double] =
    Array.tabulate(CircleSegments + 1)(i =>
      math.cos(i * TwoPi / CircleSegments)
    )

  /** Unit circle Y coordinates for `64`-segment rendering. */
  lazy val unitCircleY: Array[Double] =
    Array.tabulate(CircleSegments + 1)(i =>
      math.sin(i * TwoPi / CircleSegments)
    )
