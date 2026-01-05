package jagfx.io

import java.nio.file.*

import scala.collection.mutable.ListBuffer

import jagfx.Constants
import jagfx.Constants.MaxPartials
import jagfx.Constants.MaxTones
import jagfx.model.*
import jagfx.types.*
import jagfx.utils.ArrayUtils

/** Parser for `.synth` binary format to `SynthFile` domain model. */
object SynthReader:
  /** Parse error with message and byte position. */
  case class ParseError(message: String, position: Int)

  /** Parses `.synth` binary data into `SynthFile`. */
  def read(data: Array[Byte]): Either[ParseError, SynthFile] =
    new SynthParser(BinaryBuffer(data)).parse()

  /** Reads `.synth` file from filesystem path. */
  def readFromPath(path: Path): Either[ParseError, SynthFile] =
    try
      val data = Files.readAllBytes(path)
      read(data)
    catch
      case e: Exception =>
        scribe.error(s"IO error reading $path: ${e.getMessage}")
        Left(ParseError(s"IO Error: ${e.getMessage}", -1))

  private class SynthParser(buf: BinaryBuffer):
    // Fields
    private val warnings = ListBuffer[String]()

    /** Parses `.synth` binary data into `SynthFile`. */
    def parse(): Either[ParseError, SynthFile] =
      try
        val tones = readTones()
        val loopParams =
          if buf.remaining >= 4 then
            LoopParams(buf.readUInt16BE(), buf.readUInt16BE())
          else
            warnings += s"File truncated at 0x${buf.position.toHexString.toUpperCase}; defaulting loop parameters..."
            LoopParams(0, 0)
        Right(SynthFile(tones, loopParams, warnings.toList))
      catch
        case e: Exception =>
          scribe.error(
            s"Parse failed at pos ${buf.position}: ${e.getClass.getName} ${e.getMessage}"
          )
          e.printStackTrace()
          Left(ParseError(e.getMessage, buf.position))

    private def readTones(): Vector[Option[Tone]] =
      (0 until MaxTones).map { _ =>
        if buf.remaining > 4 then
          val marker = buf.peek()
          if marker != 0 then
            val tone = readTone()
            applyRev377TonePadding()
            Some(tone)
          else
            buf.skip(1)
            None
        else None
      }.toVector

    private def readTone(): Tone =
      val pitchEnvelope = readEnvelope()
      val volumeEnvelope = readEnvelope()

      val (vibratoRate, vibratoDepth) = readOptionalEnvelopePair()
      val (tremoloRate, tremoloDepth) = readOptionalEnvelopePair()
      val (gateSilence, gateDuration) = readOptionalEnvelopePair()

      val partials = readPartials()
      val echoDelay = buf.readUSmart().value
      val echoMix = buf.readUSmart().value
      val duration = buf.readUInt16BE()
      val start = buf.readUInt16BE()
      val filter = readFilter()

      Tone(
        pitchEnvelope = applyRev245EnvelopePatch(pitchEnvelope, duration),
        volumeEnvelope = applyRev245EnvelopePatch(volumeEnvelope, duration),
        vibratoRate = vibratoRate,
        vibratoDepth = vibratoDepth,
        tremoloRate = tremoloRate,
        tremoloDepth = tremoloDepth,
        gateSilence = gateSilence,
        gateDuration = gateDuration,
        partials = partials,
        echoDelay = echoDelay,
        echoMix = echoMix,
        duration = duration,
        start = start,
        filter = filter
      )

    private def readEnvelope(): Envelope =
      val waveformId = buf.readUInt8()
      val start = buf.readInt32BE()
      val end = buf.readInt32BE()
      val waveform = Waveform.fromId(waveformId)
      val segmentLength = buf.readUInt8()
      val segments = (0 until segmentLength).map { _ =>
        EnvelopeSegment(buf.readUInt16BE(), buf.readUInt16BE())
      }.toVector
      Envelope(waveform, start, end, segments)

    private def readOptionalEnvelopePair()
        : (Option[Envelope], Option[Envelope]) =
      val marker = buf.peek()
      if marker != 0 then
        val env1 = readEnvelope()
        val env2 = readEnvelope()
        (Some(env1), Some(env2))
      else
        buf.skip(1)
        (None, None)

    private def readPartials(): Vector[Partial] =
      val builder = Vector.newBuilder[Partial]
      builder.sizeHint(MaxPartials)
      var continue = true
      var count = 0
      while continue && count < MaxPartials do
        val volume = buf.readUSmart().value
        if volume == 0 then continue = false
        else
          val pitchOffset = buf.readSmart().value
          val startDelay = buf.readUSmart().value
          builder += Partial(Percent(volume), pitchOffset, Millis(startDelay))
          count += 1
      builder.result()

    private def readFilter(): Option[Filter] =
      if buf.remaining == 0 then return None
      if !detectFilterPresent() then return None

      val (pairCount0, pairCount1) = readFilterHeader()
      val unity0 = buf.readUInt16BE()
      val unity1 = buf.readUInt16BE()
      val modulationMask = buf.readUInt8()

      val (frequencies, magnitudes) =
        readFilterCoefficients(pairCount0, pairCount1, modulationMask)

      val envelope =
        if modulationMask != 0 || unity1 != unity0 then
          Some(readEnvelopeSegments())
        else None

      if buf.isTruncated then
        warnings += "Filter truncated (discarding partial data)"
        None
      else
        Some(
          buildFilter(
            pairCount0,
            pairCount1,
            unity0,
            unity1,
            frequencies,
            magnitudes,
            envelope
          )
        )

    private def readEnvelopeSegments(): Envelope =
      val length = buf.readUInt8()
      val segments = (0 until length).map { _ =>
        val dur = buf.readUInt16BE()
        val peak = buf.readUInt16BE()
        EnvelopeSegment(dur, peak)
      }.toVector
      Envelope(Waveform.Off, 0, 0, segments)

    private def detectFilterPresent(): Boolean =
      val peeked = buf.peek()
      if peeked == 0 then
        buf.skip(1)
        return false
      if peeked >= 1 && peeked <= 4 && buf.remaining >= Constants.MaxTones then
        val possibleSegCount = buf.peekAt(9)
        if possibleSegCount <= 15 then return false
      true

    private def readFilterHeader(): (Int, Int) =
      val packedPairs = buf.readUInt8()
      val pairCount0 = packedPairs >> 4
      val pairCount1 = packedPairs & 0xf
      (pairCount0, pairCount1)

    private def readFilterCoefficients(
        pairCount0: Int,
        pairCount1: Int,
        modulationMask: Int
    ): (Array[Array[Array[Int]]], Array[Array[Array[Int]]]) =
      val frequencies = Array.ofDim[Int](2, 2, 4)
      val magnitudes = Array.ofDim[Int](2, 2, 4)

      for channel <- 0 until 2 do
        val pairs = if channel == 0 then pairCount0 else pairCount1
        for p <- 0 until pairs do
          frequencies(channel)(0)(p) = buf.readUInt16BE()
          magnitudes(channel)(0)(p) = buf.readUInt16BE()

      for channel <- 0 until 2 do
        val pairs = if channel == 0 then pairCount0 else pairCount1
        for p <- 0 until pairs do
          if (modulationMask & (1 << (channel * 4) << p)) != 0 then
            frequencies(channel)(1)(p) = buf.readUInt16BE()
            magnitudes(channel)(1)(p) = buf.readUInt16BE()
          else
            frequencies(channel)(1)(p) = frequencies(channel)(0)(p)
            magnitudes(channel)(1)(p) = magnitudes(channel)(0)(p)

      (frequencies, magnitudes)

    private def buildFilter(
        pairCount0: Int,
        pairCount1: Int,
        unity0: Int,
        unity1: Int,
        frequencies: Array[Array[Array[Int]]],
        magnitudes: Array[Array[Array[Int]]],
        envelope: Option[Envelope]
    ): Filter =
      val freqIArray = ArrayUtils.toFilterIArray3D(frequencies)
      val magIArray = ArrayUtils.toFilterIArray3D(magnitudes)
      Filter(
        IArray(pairCount0, pairCount1),
        IArray(unity0, unity1),
        freqIArray,
        magIArray,
        envelope
      )

    private def applyRev377TonePadding(): Unit =
      if buf.remaining > 0 && buf.peek() == 0 then buf.skip(1)

    private def applyRev245EnvelopePatch(
        env: Envelope,
        duration: Int
    ): Envelope =
      if env.segments.isEmpty && env.start != env.end then
        env.copy(segments = Vector(EnvelopeSegment(duration, env.end)))
      else env
