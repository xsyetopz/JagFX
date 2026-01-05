package jagfx.synth

import jagfx.model._
import jagfx.constants

class AudioBufferSuite extends munit.FunSuite:

  test("empty buffer has correct size"):
    val buf = AudioBuffer.empty(1000)
    assertEquals(buf.length, 1000)
    assertEquals(buf.sampleRate, constants.SampleRate)

  test("clip limits samples to 16-bit range"):
    val samples =
      Array(-50000, Short.MinValue.toInt, 0, Short.MaxValue.toInt, 50000)
    val buf = AudioBuffer(samples, constants.SampleRate).clip()
    assertEquals(buf.samples(0), Short.MinValue.toInt)
    assertEquals(buf.samples(1), Short.MinValue.toInt)
    assertEquals(buf.samples(2), 0)
    assertEquals(buf.samples(3), Short.MaxValue.toInt)
    assertEquals(buf.samples(4), Short.MaxValue.toInt)

  test("mix combines two buffers"):
    val buf1 = AudioBuffer(Array(100, 200, 300), constants.SampleRate)
    val buf2 = AudioBuffer(Array(10, 20), constants.SampleRate)
    val mixed = buf1.mix(buf2, 1)
    assertEquals(mixed.samples(0), 100)
    assertEquals(mixed.samples(1), 210)
    assertEquals(mixed.samples(2), 320)

  test("toBytesUnsigned converts to 8-bit unsigned"):
    val buf = AudioBuffer(Array(0, 256, -256), constants.SampleRate)
    val bytes = buf.toUBytes
    assertEquals(
      bytes(0) & 0xff,
      Byte.MaxValue + 1
    ) // 0 -> 128 (silence)
    assertEquals(bytes(1) & 0xff, Byte.MaxValue + 2) // +256 -> 129
    assertEquals(bytes(2) & 0xff, Byte.MaxValue.toInt) // -256 -> 127
