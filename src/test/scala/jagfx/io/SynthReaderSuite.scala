package jagfx.io

import jagfx.TestFixtures.*
import jagfx.model.*
import jagfx.types.*

class SynthReaderSuite extends munit.FunSuite:
  test("reads cow_death (1 voice) correctly"):
    val result = SynthReader.read(cowDeathHex)
    assert(result.isRight)
    val file = result.toOption.get

    assertEquals(file.activeVoices.size, 1)
    assertEquals(file.loop.begin, 0)
    assertEquals(file.loop.end, 0)

  test("reads protect_from_magic (2 voices) correctly"):
    val result = SynthReader.read(protectFromMagicHex)
    assert(result.isRight)
    val file = result.toOption.get

    assertEquals(file.activeVoices.size, 2)
    val voiceIndices = file.activeVoices.map(_._1)
    assertEquals(voiceIndices, Vector(0, 1))

    assertEquals(file.loop.begin, 0)
    assertEquals(file.loop.end, 0)

  test("reads ice_cast (2 voices) correctly"):
    val result = SynthReader.read(iceCastHex)
    assert(result.isRight)
    val file = result.toOption.get

    assertEquals(file.activeVoices.size, 2)
    assertEquals(file.loop.begin, 0)
    assertEquals(file.loop.end, 0)

  test("parses envelope forms correctly"):
    val cow = SynthReader.read(cowDeathHex).toOption.get
    val (_, cowVoice) = cow.activeVoices.head
    assertEquals(cowVoice.pitchEnvelope.waveform, Waveform.Sine)

    val protect = SynthReader.read(protectFromMagicHex).toOption.get
    val (_, protectVoice1) = protect.activeVoices.head
    assertEquals(protectVoice1.pitchEnvelope.waveform, Waveform.Square)

  test("parses partials correctly"):
    val result = SynthReader.read(cowDeathHex).toOption.get
    val (_, voice) = result.activeVoices.head
    assertEquals(voice.partials.length, 2)
    assertEquals(voice.partials(0).volume, Percent(100))
