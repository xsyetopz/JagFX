package jagfx.io

import jagfx.model._
import jagfx.TestFixtures._

class SynthReaderSuite extends munit.FunSuite:

  test("reads cow_death (1 tone) correctly"):
    val result = SynthReader.read(cowDeathHex)
    assert(result.isRight)
    val file = result.toOption.get

    assertEquals(file.activeTones.size, 1)
    assertEquals(file.loop.begin, 0)
    assertEquals(file.loop.end, 0)

  test("reads protect_from_magic (2 tones) correctly"):
    val result = SynthReader.read(protectFromMagicHex)
    assert(result.isRight)
    val file = result.toOption.get

    assertEquals(file.activeTones.size, 2)
    assertEquals(file.loop.begin, 0)
    assertEquals(file.loop.end, 0)

    val toneIndices = file.activeTones.map(_._1)
    assertEquals(toneIndices, Vector(0, 1))

  test("parses envelope forms correctly"):
    val cow = SynthReader.read(cowDeathHex).toOption.get
    val (_, cowTone) = cow.activeTones.head
    assertEquals(cowTone.pitchEnvelope.form, WaveForm.Sine)

    val protect = SynthReader.read(protectFromMagicHex).toOption.get
    val (_, protectTone1) = protect.activeTones.head
    assertEquals(protectTone1.pitchEnvelope.form, WaveForm.Square)

  test("parses harmonics correctly"):
    val result = SynthReader.read(cowDeathHex).toOption.get
    val (_, tone) = result.activeTones.head
    assertEquals(tone.harmonics.length, 2)
    assertEquals(tone.harmonics(0).volume, 100)
