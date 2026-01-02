package jagfx.io

import jagfx.model._
import jagfx.TestFixtures._

class SynthWriterSuite extends munit.FunSuite:

  private def assertByteIdentical(
      written: Array[Byte],
      original: Array[Byte],
      name: String
  ): Unit =
    assertEquals(written.length, original.length, s"$name: length mismatch")
    val diffs = written.zip(original).zipWithIndex.collect {
      case ((w, o), i) if w != o =>
        s"pos $i: wrote 0x${"%02x".format(w & 0xff)}, expected 0x${"%02x".format(o & 0xff)}"
    }
    assert(
      diffs.isEmpty,
      s"$name byte differences:\n${diffs.take(10).mkString("\n")}"
    )

  test("cow_death (1 tone) byte-identical roundtrip"):
    val original = SynthReader.read(cowDeathHex).toOption.get
    val written = SynthWriter.write(original)
    assertByteIdentical(written, cowDeathHex, "cow_death")

  test("protect_from_magic (2 tones) byte-identical roundtrip"):
    val original = SynthReader.read(protectFromMagicHex).toOption.get
    val written = SynthWriter.write(original)
    assertByteIdentical(written, protectFromMagicHex, "protect_from_magic")

  test("roundtrip preserves model equality"):
    val original = SynthReader.read(protectFromMagicHex).toOption.get
    val written = SynthWriter.write(original)
    val reread = SynthReader.read(written).toOption.get

    assertEquals(reread.activeTones.size, 2)
    assertEquals(reread.loop, original.loop)

  test("writes empty file correctly"):
    val emptyFile = SynthFile(Vector.fill(10)(None), LoopParams(100, 200))
    val written = SynthWriter.write(emptyFile)

    assertEquals(written.length, 14)
    val reread = SynthReader.read(written).toOption.get
    assertEquals(reread.activeTones.size, 0)
    assertEquals(reread.loop.begin, 100)
