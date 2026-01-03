package jagfx.synth

import jagfx.model._
import jagfx.Constants.Int16
import jagfx.Constants

class AudioBufferSuite extends munit.FunSuite:

  test("empty buffer has correct size"):
    val buf = AudioBuffer.empty(1000)
    assertEquals(buf.length, 1000)
    assertEquals(buf.sampleRate, Constants.SampleRate)

  test("clip limits samples to 16-bit range"):
    val samples = Array(-50000, Int16.Min, 0, Int16.Max, 50000)
    val buf = AudioBuffer(samples, Constants.SampleRate).clip()
    assertEquals(buf.samples(0), Int16.Min)
    assertEquals(buf.samples(1), Int16.Min)
    assertEquals(buf.samples(2), 0)
    assertEquals(buf.samples(3), Int16.Max)
    assertEquals(buf.samples(4), Int16.Max)

  test("mix combines two buffers"):
    val buf1 = AudioBuffer(Array(100, 200, 300), Constants.SampleRate)
    val buf2 = AudioBuffer(Array(10, 20), Constants.SampleRate)
    val mixed = buf1.mix(buf2, 1)
    assertEquals(mixed.samples(0), 100)
    assertEquals(mixed.samples(1), 210)
    assertEquals(mixed.samples(2), 320)

  test("toBytesUnsigned converts to 8-bit unsigned"):
    val buf = AudioBuffer(Array(0, 256, -256), Constants.SampleRate)
    val bytes = buf.toBytesUnsigned
    assertEquals(bytes(0) & 0xff, 128) // 0 -> 128 (silence)
    assertEquals(bytes(1) & 0xff, 129) // +256 -> 129
    assertEquals(bytes(2) & 0xff, 127) // -256 -> 127
