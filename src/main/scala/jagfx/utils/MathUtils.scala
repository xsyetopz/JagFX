package jagfx.utils

/** Common mathematical constants and utilities. */
object MathUtils:
  /** 2π */
  val TwoPi: Double = 2 * math.Pi

  /** Half of π (90°) */
  val HalfPi: Double = math.Pi / 2

  /** Euclidean distance between two points. */
  inline def distance(x1: Double, y1: Double, x2: Double, y2: Double): Double =
    math.sqrt(math.pow(x2 - x1, 2) + math.pow(y2 - y1, 2))

  /** Clamp value between min and max. */
  inline def clamp(value: Double, min: Double, max: Double): Double =
    math.max(min, math.min(max, value))

  /** Clamp integer between min and max. */
  inline def clamp(value: Int, min: Int, max: Int): Int =
    math.max(min, math.min(max, value))

  /** Linear interpolation. */
  inline def lerp(a: Double, b: Double, t: Double): Double =
    a + (b - a) * t

  /** Map value from one range to another. */
  inline def mapRange(
      value: Double,
      inMin: Double,
      inMax: Double,
      outMin: Double,
      outMax: Double
  ): Double =
    outMin + (value - inMin) / (inMax - inMin) * (outMax - outMin)

  /** Convert decibels to linear gain. */
  inline def dBToLinear(dB: Double): Double =
    math.pow(10.0, dB / 20.0)

  /** Convert linear gain to decibels. */
  inline def linearToDb(linear: Double): Double =
    20.0 * math.log10(math.max(0.00001, linear))
