package jagfx.io

import jagfx.model._
import java.nio.file._
import scala.collection.mutable.ListBuffer

private val MaxTones: Int = 10
private val MaxHarmonics: Int = 10

/** Parser for `.synth` binary format to `SynthFile` domain model. */
object SynthReader:
  /** Parse error with message and byte position. */
  case class ParseError(message: String, position: Int)

  /** Parses `.synth` binary data into `SynthFile`.
    *
    * Returns `Left` on parse failure.
    */
  def read(data: Array[Byte]): Either[ParseError, SynthFile] =
    new SynthParser(BinaryBuffer(data)).parse()

  /** Reads `.synth` file from filesystem path.
    *
    * Returns `Left` on IO or parse failure.
    */
  def readFromPath(path: Path): Either[ParseError, SynthFile] =
    try
      val data = Files.readAllBytes(path)
      read(data)
    catch
      case e: Exception =>
        scribe.error(s"IO error reading $path: ${e.getMessage}")
        Left(ParseError(s"IO Error: ${e.getMessage}", -1))

  private class SynthParser(buf: BinaryBuffer):
    private val warnings = ListBuffer[String]()

    def parse(): Either[ParseError, SynthFile] =
      try
        val tones = readTones()

        val loopParams =
          if buf.remaining >= 4 then
            LoopParams(buf.readU16BE(), buf.readU16BE())
          else
            warnings += "File truncated; defaulting loop parameters..."
            LoopParams(0, 0)

        Right(SynthFile(tones, loopParams, warnings.toList))
      catch
        case e: Exception =>
          scribe.error(
            s"Parse failed at pos ${buf.pos}: ${e.getClass.getName} ${e.getMessage}"
          )
          e.printStackTrace()
          Left(ParseError(e.getMessage, buf.pos))

    private def readTones(): Vector[Option[Tone]] =
      (0 until MaxTones).map { i =>
        if buf.remaining > 0 then
          if buf.peek() != 0 then Some(readTone())
          else
            buf.pos += 1
            None
        else None
      }.toVector

    private def readTone(): Tone =
      val pitchEnvelope = readEnvelope()
      val volumeEnvelope = readEnvelope()

      val (vibratoRate, vibratoDepth) = readOptionalEnvelopePair()
      val (tremoloRate, tremoloDepth) = readOptionalEnvelopePair()
      val (gateSilence, gateDuration) = readOptionalEnvelopePair()

      val harmonics = readHarmonics()
      val reverbDelay = buf.readSmartUnsigned()
      val reverbVolume = buf.readSmartUnsigned()
      val duration = buf.readU16BE()
      val start = buf.readU16BE()
      val filter = readFilter()

      def fixEnvelope(env: Envelope, dur: Int): Envelope =
        if env.segments.isEmpty && env.start != env.end then
          env.copy(segments = Vector(EnvelopeSegment(dur, env.end)))
        else env

      Tone(
        pitchEnvelope = fixEnvelope(pitchEnvelope, duration),
        volumeEnvelope = fixEnvelope(volumeEnvelope, duration),
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
        start = start,
        filter = filter
      )

    private def readFilter(): Option[Filter] =
      if buf.remaining == 0 then return None

      val wasTruncated = buf.truncated

      val packedPairs = buf.readU8()
      val pairCount0 = packedPairs >> 4
      val pairCount1 = packedPairs & 0xf

      if packedPairs == 0 then return None

      val unity0 = buf.readU16BE()
      val unity1 = buf.readU16BE()
      val modulationMask = buf.readU8()

      val frequencies = Array.ofDim[Int](2, 2, 4)
      val magnitudes = Array.ofDim[Int](2, 2, 4)

      for channel <- 0 until 2 do
        val pairs = if channel == 0 then pairCount0 else pairCount1
        for p <- 0 until pairs do
          frequencies(channel)(0)(p) = buf.readU16BE()
          magnitudes(channel)(0)(p) = buf.readU16BE()
      for channel <- 0 until 2 do
        val pairs = if channel == 0 then pairCount0 else pairCount1
        for p <- 0 until pairs do
          if (modulationMask & (1 << (channel * 4) << p)) != 0 then
            frequencies(channel)(1)(p) = buf.readU16BE()
            magnitudes(channel)(1)(p) = buf.readU16BE()
          else
            frequencies(channel)(1)(p) = frequencies(channel)(0)(p)
            magnitudes(channel)(1)(p) = magnitudes(channel)(0)(p)

      val envelope =
        if modulationMask != 0 || unity1 != unity0 then
          Some(readEnvelopeSegments())
        else None

      if buf.truncated then
        warnings += "Filter truncated (discarding partial data)"
        None
      else
        Some(
          Filter(
            Array(pairCount0, pairCount1),
            Array(unity0, unity1),
            frequencies,
            magnitudes,
            envelope
          )
        )

    private def readEnvelope(): Envelope =
      val formId = buf.readU8()
      val start = buf.readS32BE()
      val end = buf.readS32BE()
      val form = WaveForm.fromId(formId)

      val segmentLength = buf.readU8()
      val segments = (0 until segmentLength).map { _ =>
        EnvelopeSegment(buf.readU16BE(), buf.readU16BE())
      }.toVector

      Envelope(form, start, end, segments)

    private def readEnvelopeSegments(): Envelope =
      val length = buf.readU8()
      val segments = (0 until length).map { _ =>
        val dur = buf.readU16BE()
        val peak = buf.readU16BE()
        EnvelopeSegment(dur, peak)
      }.toVector

      Envelope(WaveForm.Off, 0, 0, segments)

    private def readOptionalEnvelopePair()
        : (Option[Envelope], Option[Envelope]) =
      val marker = buf.peek()
      if marker != 0 then
        val env1 = readEnvelope()
        val env2 = readEnvelope()
        (Some(env1), Some(env2))
      else
        buf.pos += 1 // eat '0' flag
        (None, None)

    private def readHarmonics(): Vector[Harmonic] =
      val builder = Vector.newBuilder[Harmonic]
      builder.sizeHint(MaxHarmonics)
      var continue = true
      var count = 0
      while continue && count < MaxHarmonics do
        val volume = buf.readSmartUnsigned()
        if volume == 0 then continue = false
        else
          val semitone = buf.readSmart()
          val delay = buf.readSmartUnsigned()
          builder += Harmonic(volume, semitone, delay)
          count += 1
      builder.result()
