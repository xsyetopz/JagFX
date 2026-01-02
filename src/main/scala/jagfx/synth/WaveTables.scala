package jagfx.synth

import jagfx.Constants

private val WaveTableSize: Int = 32768
private val SinTableDivisor: Double = 5215.1903

/** Pre-computed lookup tables for waveform generation. */
object WaveTables:
  /** Noise lookup table with random `-1`/`+1` values. */
  val noise: Array[Int] =
    Array.tabulate(WaveTableSize)(_ => if math.random() > 0.5 then 1 else -1)

  /** Sine wave lookup table with `16384`-amplitude range. */
  val sin: Array[Int] = Array.tabulate(WaveTableSize)(i =>
    (math.sin(i / SinTableDivisor) * 16384.0).toInt
  )
