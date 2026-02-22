package jagfx.utils

import jagfx.Constants.Int16

/** Common mathematical constants and utilities. */
object MathUtils:
  // Constants
  final val TwoPi: Double = 2 * math.Pi
  final val HalfPi: Double = math.Pi / 2

  /** Euclidean distance between two points. */
  inline def distance(x1: Double, y1: Double, x2: Double, y2: Double): Double =
    math.sqrt(math.pow(x2 - x1, 2) + math.pow(y2 - y1, 2))

  /** Clamps value between min and max. */
  inline def clamp(value: Double, min: Double, max: Double): Double =
    math.max(min, math.min(max, value))

  /** Clamps integer between min and max. */
  inline def clamp(value: Int, min: Int, max: Int): Int =
    math.max(min, math.min(max, value))

  /** Clips array values in-place to 16-bit signed range. */
  inline def clipInt16(buffer: Array[Int], len: Int = -1): Unit =
    val end = if len < 0 then buffer.length else len
    var i = 0
    while i < end do
      if buffer(i) < Short.MinValue then buffer(i) = Short.MinValue
      else if buffer(i) > Short.MaxValue then buffer(i) = Short.MaxValue
      i += 1

  /** Linear interpolation between two values. */
  inline def lerp(a: Double, b: Double, t: Double): Double =
    a + (b - a) * t

  /** Maps value from one range to another. */
  inline def mapRange(
      value: Double,
      inMin: Double,
      inMax: Double,
      outMin: Double,
      outMax: Double
  ): Double =
    outMin + (value - inMin) / (inMax - inMin) * (outMax - outMin)

  /** Converts decibels to linear gain. */
  inline def dBToLinear(dB: Double): Double =
    math.pow(10.0, dB / 20.0)

  /** Converts linear gain to decibels. */
  inline def linearToDb(linear: Double): Double =
    20.0 * math.log10(math.max(0.00001, linear))

  /** Unit types for value conversion. */
  enum UnitType:
    case Raw16
    case Percent
    case Normalized
    case Decicents

  /** Converts value between unit types. */
  def convert(value: Double, from: UnitType, to: UnitType): Double =
    if from == to then value
    else
      val normalized = from match
        case UnitType.Raw16      => value / Int16.Range.toDouble
        case UnitType.Percent    => value / 100.0
        case UnitType.Normalized => value
        case UnitType.Decicents  => value / 1200.0
      to match
        case UnitType.Raw16      => normalized * Int16.Range.toDouble
        case UnitType.Percent    => normalized * 100.0
        case UnitType.Normalized => normalized
        case UnitType.Decicents  => normalized * 1200.0

  /** Formats value with unit suffix. */
  def format(value: Double, unit: UnitType, decimals: Int = 1): String =
    val fmt = s"%.${decimals}f"
    unit match
      case UnitType.Raw16      => value.toInt.toString
      case UnitType.Percent    => s"${fmt.format(value)}%"
      case UnitType.Normalized => fmt.format(value)
      case UnitType.Decicents  => s"${fmt.format(value / 10.0)} st"

  /** Calculates distance from point to line segment. */
  def distanceToSegment(
      px: Double,
      py: Double,
      x1: Double,
      y1: Double,
      x2: Double,
      y2: Double
  ): Double =
    val lengthSquared = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)
    if lengthSquared == 0 then distance(px, py, x1, y1)
    else
      var t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / lengthSquared
      t = math.max(0, math.min(1, t))
      val projx = x1 + t * (x2 - x1)
      val projy = y1 + t * (y2 - y1)
      distance(px, py, projx, projy)
