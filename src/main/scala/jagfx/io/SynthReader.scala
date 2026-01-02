package jagfx.io

import jagfx.model._
import java.nio.file._

private val MaxTones: Int = 10
private val MaxHarmonics: Int = 10

/** Parser for `.synth` binary format to `SynthFile` domain model. */
object SynthReader:
  /** Parse error with message and byte position. */
  case class ParseError(message: String, position: Int)

  /** Parses `.synth` binary data into `SynthFile`. Returns `Left` on parse
    * failure.
    */
  def read(data: Array[Byte]): Either[ParseError, SynthFile] =
    try
      val buf = BinaryBuffer(data)
      val tones = readTones(buf)
      val loopBegin = buf.readU16BE()
      val loopEnd = buf.readU16BE()
      Right(SynthFile(tones, LoopParams(loopBegin, loopEnd)))
    catch
      case e: Exception =>
        scribe.error(s"Parse failed: ${e.getMessage}")
        Left(ParseError(e.getMessage, -1))

  /** Reads `.synth` file from filesystem path. Returns `Left` on IO or parse
    * failure.
    */
  def readFromPath(path: Path): Either[ParseError, SynthFile] =
    try
      val data = Files.readAllBytes(path)
      scribe.debug(s"Read ${data.length} byte(s) from $path")
      read(data)
    catch
      case e: Exception =>
        scribe.error(s"IO error reading $path: ${e.getMessage}")
        Left(ParseError(s"IO Error: ${e.getMessage}", -1))

  private def readTones(buf: BinaryBuffer): Vector[Option[Tone]] =
    (0 until MaxTones).map { _ =>
      if buf.peek() != 0 then Some(readTone(buf))
      else
        buf.pos += 1
        None
    }.toVector

  private def readTone(buf: BinaryBuffer): Tone =
    val pitchEnvelope = readEnvelope(buf)
    val volumeEnvelope = readEnvelope(buf)

    val (vibratoRate, vibratoDepth) = readOptionalEnvelopePair(buf)
    val (tremoloRate, tremoloDepth) = readOptionalEnvelopePair(buf)
    val (gateSilence, gateDuration) = readOptionalEnvelopePair(buf)

    val harmonics = readHarmonics(buf)
    val reverbDelay = buf.readSmartUnsigned()
    val reverbVolume = buf.readSmartUnsigned()
    val duration = buf.readU16BE()
    val start = buf.readU16BE()

    val t = Tone(
      pitchEnvelope = pitchEnvelope,
      volumeEnvelope = volumeEnvelope,
      vibratoRate = vibratoRate,
      vibratoDepth = vibratoDepth,
      tremoloRate = tremoloRate,
      tremoloDepth = tremoloDepth,
      gateSilence = gateSilence,
      gateDuration = gateDuration,
      harmonics = harmonics,
      reverbDelay = reverbDelay,
      reverbVolume = reverbVolume,
      duration = duration,
      start = start
    )
    harmonics.zipWithIndex.foreach { case (h, i) =>
      scribe.debug(
        s"  H$i: Vol=${h.volume}, Semi=${h.semitone}, Dly=${h.delay}"
      )
    }
    t

  private def readEnvelope(buf: BinaryBuffer): Envelope =
    val form = WaveForm.fromId(buf.readU8())
    val start = buf.readS32BE()
    val end = buf.readS32BE()
    val length = buf.readU8()
    val segments = (0 until length).map { _ =>
      EnvelopeSegment(buf.readU16BE(), buf.readU16BE())
    }.toVector
    Envelope(form, start, end, segments)

  private def readOptionalEnvelopePair(
      buf: BinaryBuffer
  ): (Option[Envelope], Option[Envelope]) =
    if buf.peek() != 0 then
      val env1 = readEnvelope(buf)
      val env2 = readEnvelope(buf)
      (Some(env1), Some(env2))
    else
      buf.pos += 1
      (None, None)

  private def readHarmonics(buf: BinaryBuffer): Vector[Harmonic] =
    var harmonics = Vector.empty[Harmonic]
    var continue = true
    var count = 0
    while continue && count < MaxHarmonics do
      val volume = buf.readSmartUnsigned()
      if volume == 0 then continue = false
      else
        val semitone = buf.readSmart()
        val delay = buf.readSmartUnsigned()
        harmonics = harmonics :+ Harmonic(volume, semitone, delay)
        count += 1
    harmonics
