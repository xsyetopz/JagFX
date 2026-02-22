package jagfx

import jagfx.Constants.SampleRate

/** Opaque types for type-safe value handling with zero runtime overhead. */
object types:
  // Variable-length integers
  opaque type Smart = Int
  opaque type USmart = Int

  object Smart:
    inline def apply(v: Int): Smart = v
    extension (s: Smart)
      inline def value: Int = s
      inline def isSmall: Boolean = s >= -64 && s < 64

  object USmart:
    inline def apply(v: Int): USmart = v
    extension (s: USmart)
      inline def value: Int = s
      inline def isSmall: Boolean = s < Byte.MaxValue + 1

  // Time/sample types
  opaque type Millis = Int
  opaque type Samples = Int

  object Millis:
    inline def apply(v: Int): Millis = v
    extension (m: Millis)
      inline def value: Int = m
      inline def toSamples: Samples = Samples((m * SampleRate) / 1000)

  object Samples:
    inline def apply(v: Int): Samples = v
    extension (s: Samples)
      inline def value: Int = s
      inline def toMillis: Millis = Millis((s * 1000) / SampleRate)

  // Percentage (`0-100`)
  opaque type Percent = Int

  object Percent:
    inline def apply(v: Int): Percent = v
    extension (p: Percent) inline def value: Int = p
