package jagfx.io

import jagfx.TestFixtures.*
import jagfx.model.*
import jagfx.types.*

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
    val toneIndices = file.activeTones.map(_._1)
    assertEquals(toneIndices, Vector(0, 1))

    assertEquals(file.loop.begin, 0)
    assertEquals(file.loop.end, 0)

  test("reads ice_cast (2 tones) correctly"):
    val result = SynthReader.read(iceCastHex)
    assert(result.isRight)
    val file = result.toOption.get

    assertEquals(file.activeTones.size, 2)
    assertEquals(file.loop.begin, 0)
    assertEquals(file.loop.end, 0)

  test("parses envelope forms correctly"):
    val cow = SynthReader.read(cowDeathHex).toOption.get
    val (_, cowTone) = cow.activeTones.head
    assertEquals(cowTone.pitchEnvelope.waveform, Waveform.Sine)

    val protect = SynthReader.read(protectFromMagicHex).toOption.get
    val (_, protectTone1) = protect.activeTones.head
    assertEquals(protectTone1.pitchEnvelope.waveform, Waveform.Square)

  test("parses partials correctly"):
    val result = SynthReader.read(cowDeathHex).toOption.get
    val (_, tone) = result.activeTones.head
    assertEquals(tone.partials.length, 2)
    assertEquals(tone.partials(0).volume, Percent(100))
