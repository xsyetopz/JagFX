package jagfx.synth

import jagfx.model._

class AudioBufferSuite extends munit.FunSuite:

  test("empty buffer has correct size"):
    val buf = AudioBuffer.empty(1000)
    assertEquals(buf.length, 1000)
    assertEquals(buf.sampleRate, 22050)

  test("clip limits samples to 16-bit range"):
    val samples = Array(-50000, -32768, 0, 32767, 50000)
    val buf = AudioBuffer(samples, 22050).clip()
    assertEquals(buf.samples(0), -32768)
    assertEquals(buf.samples(1), -32768)
    assertEquals(buf.samples(2), 0)
    assertEquals(buf.samples(3), 32767)
    assertEquals(buf.samples(4), 32767)

  test("mix combines two buffers"):
    val buf1 = AudioBuffer(Array(100, 200, 300), 22050)
    val buf2 = AudioBuffer(Array(10, 20), 22050)
    val mixed = buf1.mix(buf2, 1)
    assertEquals(mixed.samples(0), 100)
    assertEquals(mixed.samples(1), 210)
    assertEquals(mixed.samples(2), 320)

  test("toBytes converts to 8-bit unsigned"):
    val buf = AudioBuffer(Array(0, 256, -256), 22050)
    val bytes = buf.toBytes
    assertEquals(bytes(0) & 0xff, 128)
    assertEquals(bytes(1) & 0xff, 129)
    assertEquals(bytes(2) & 0xff, 127)
