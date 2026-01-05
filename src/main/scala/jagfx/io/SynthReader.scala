package jagfx.io

import jagfx.types._
import jagfx.model._
import jagfx.constants.{MaxTones, MaxPartials}
import java.nio.file._
import scala.collection.mutable.ListBuffer

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
    private val _warnings = ListBuffer[String]()

    def parse(): Either[ParseError, SynthFile] =
      try
        val tones = _readTones()

        val loopParams =
          if buf.remaining >= 4 then
            LoopParams(buf.readUInt16BE(), buf.readUInt16BE())
          else
            _warnings += s"File truncated at 0x${buf.position.toHexString.toUpperCase}; defaulting loop parameters..."
            LoopParams(0, 0)

        Right(SynthFile(tones, loopParams, _warnings.toList))
      catch
        case e: Exception =>
          scribe.error(
            s"Parse failed at pos ${buf.position}: ${e.getClass.getName} ${e.getMessage}"
          )
          e.printStackTrace()
          Left(ParseError(e.getMessage, buf.position))

    private def _readTones(): Vector[Option[Tone]] =
      // Rev377 files have 0x00 padding bytes after each tone's data
      // Rev245 files pack tones consecutively with no padding
      (0 until MaxTones).map { _ =>
        if buf.remaining > 4 then
          val marker = buf.peek()
          if marker != 0 then
            val tone = _readTone()
            // skip Rev377's trailing 0x00 padding after each tone (if there)
            if buf.remaining > 0 && buf.peek() == 0 then buf.skip(1)
            Some(tone)
          else
            buf.skip(1)
            None
        else None
      }.toVector

    private def _readTone(): Tone =
      val pitchEnvelope = _readEnvelope()
      val volumeEnvelope = _readEnvelope()

      val (vibratoRate, vibratoDepth) = _readOptionalEnvelopePair()
      val (tremoloRate, tremoloDepth) = _readOptionalEnvelopePair()
      val (gateRelease, gateAttack) = _readOptionalEnvelopePair()

      val partials = _readPartials()
      val echoDelay = buf.readUSmart().value
      val echoMix = buf.readUSmart().value
      val duration = buf.readUInt16BE()
      val start = buf.readUInt16BE()
      val filter = _readFilter()

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
        gateRelease = gateRelease,
        gateAttack = gateAttack,
        partials = partials,
        echoDelay = echoDelay,
        echoMix = echoMix,
        duration = duration,
        start = start,
        filter = filter
      )

    private def _readFilter(): Option[Filter] =
      if buf.remaining == 0 then return None

      // Rev377 uses 0x00 as explicit "no filter" marker
      // Rev245 has no marker as tones end directly before next WaveformID
      val peeked = buf.peek()
      if peeked == 0 then
        buf.skip(1) // consume Rev377's "no filter" marker
        return None
      // PACKED_PAIRS can also be 1-4 (e.g. 0x01 = 0 pairs ch0, 1 pair ch1)
      // separate diffs by checking byte 9: env has seg count (small), filt has freq data (large)
      if peeked >= 1 && peeked <= 4 && buf.remaining >= 10 then
        val possibleSegCount = buf.peekAt(9)
        if possibleSegCount <= 15 then return None

      val wasTruncated = buf.isTruncated

      val packedPairs = buf.readUInt8()
      val pairCount0 = packedPairs >> 4
      val pairCount1 = packedPairs & 0xf

      val unity0 = buf.readUInt16BE()
      val unity1 = buf.readUInt16BE()
      val modulationMask = buf.readUInt8()

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

      val envelope =
        if modulationMask != 0 || unity1 != unity0 then
          Some(_readEnvelopeSegments())
        else None

      if buf.isTruncated then
        _warnings += "Filter truncated (discarding partial data)"
        None
      else
        val freqIArray = IArray.tabulate(2)(d =>
          IArray.tabulate(2)(p => IArray.tabulate(4)(i => frequencies(d)(p)(i)))
        )
        val magIArray = IArray.tabulate(2)(d =>
          IArray.tabulate(2)(p => IArray.tabulate(4)(i => magnitudes(d)(p)(i)))
        )
        Some(
          Filter(
            IArray(pairCount0, pairCount1),
            IArray(unity0, unity1),
            freqIArray,
            magIArray,
            envelope
          )
        )

    private def _readEnvelope(): Envelope =
      val waveformId = buf.readUInt8()
      val start = buf.readInt32BE()
      val end = buf.readInt32BE()
      val waveform = Waveform.fromId(waveformId)

      val segmentLength = buf.readUInt8()
      val segments = (0 until segmentLength).map { _ =>
        EnvelopeSegment(buf.readUInt16BE(), buf.readUInt16BE())
      }.toVector

      Envelope(waveform, start, end, segments)

    private def _readEnvelopeSegments(): Envelope =
      val length = buf.readUInt8()
      val segments = (0 until length).map { _ =>
        val dur = buf.readUInt16BE()
        val peak = buf.readUInt16BE()
        EnvelopeSegment(dur, peak)
      }.toVector

      Envelope(Waveform.Off, 0, 0, segments)

    private def _readOptionalEnvelopePair()
        : (Option[Envelope], Option[Envelope]) =
      val marker = buf.peek()
      if marker != 0 then
        val env1 = _readEnvelope()
        val env2 = _readEnvelope()
        (Some(env1), Some(env2))
      else
        buf.skip(1) // eat '0' flag
        (None, None)

    private def _readPartials(): Vector[Partial] =
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
