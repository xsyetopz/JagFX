package jagfx.synth

import jagfx.Constants
import jagfx.TestFixtures.*
import jagfx.io.SynthReader
import jagfx.model.*
import jagfx.synth.TrackSynthesizer

class TrackSynthesizerSuite extends munit.FunSuite:

  test("synthesizes cow_death (1 tone)"):
    val file = SynthReader.read(cowDeathHex).toOption.get
    val audio = TrackSynthesizer.synthesize(file, 1)

    assert(audio.length > 0)
    assertEquals(audio.sampleRate, Constants.SampleRate)
    assertEquals(audio.length, 19889 - 44)

  test("synthesizes protect_from_magic (2 tones)"):
    val file = SynthReader.read(protectFromMagicHex).toOption.get
    val audio = TrackSynthesizer.synthesize(file, 1)

    assert(audio.length > 0)
    assertEquals(audio.sampleRate, Constants.SampleRate)
    assertEquals(audio.length, 33119 - 44)

  test("synthesizes ice_cast (2 tones)"):
    val file = SynthReader.read(iceCastHex).toOption.get
    val audio = TrackSynthesizer.synthesize(file, 1)

    assert(audio.length > 0)
    assertEquals(audio.sampleRate, Constants.SampleRate)

  test("empty file produces empty buffer"):
    val emptyFile = SynthFile(Vector.fill(10)(None), LoopParams(0, 0))
    val audio = TrackSynthesizer.synthesize(emptyFile, 1)
    assertEquals(audio.length, 0)
