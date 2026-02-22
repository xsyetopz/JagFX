package jagfx.utils

import jagfx.Constants

/** Audio time conversion utilities. */
object AudioUtils:
  import jagfx.types._
  import Constants._

  /** Converts milliseconds to sample count using current sample rate. */
  def msToSamples(ms: Millis): Samples =
    ms.toSamples

  /** Converts milliseconds (as `Double`) to sample count. */
  def msToSamples(ms: Double): Samples =
    Samples((ms * SampleRate / 1000.0).toInt)
