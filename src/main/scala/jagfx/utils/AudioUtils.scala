package jagfx.utils

import jagfx.constants

object AudioUtils:
  import jagfx.types._
  import constants._

  def msToSamples(ms: Millis): Samples =
    ms.toSamples

  def msToSamples(ms: Double): Samples =
    Samples((ms * SampleRate / 1000.0).toInt)
