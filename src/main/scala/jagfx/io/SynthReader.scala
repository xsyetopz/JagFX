package jagfx.io

import jagfx.model._
import java.nio.file._
import jagfx.Constants.Int16

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
    val buf = BinaryBuffer(data)
    try
      val tones = readTones(buf)

      val loopParams =
        if buf.remaining >= 4 then LoopParams(buf.readU16BE(), buf.readU16BE())
        else
          scribe.warn("File truncated; defaulting Loop parameters to 0...")
          LoopParams(0, 0)

      Right(SynthFile(tones, loopParams))
    catch
      case e: Exception =>
        scribe.error(
          s"Parse failed at pos ${buf.pos}: ${e.getClass.getName} ${e.getMessage}"
        )
        e.printStackTrace()
        Left(ParseError(e.getMessage, buf.pos))

  /** Reads `.synth` file from filesystem path.
    *
    * Returns `Left` on IO or parse failure.
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
    (0 until MaxTones).map { i =>
      if buf.remaining > 0 then
        if buf.peek() != 0 then Some(readTone(buf))
        else
          buf.pos += 1
          None
      else None
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
    val filter = readFilter(buf)

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

  private def readFilter(buf: BinaryBuffer): Option[Filter] =
    if buf.remaining == 0 then return None

    val startPos = buf.pos
    val count = buf.readU8()
    if count == 0 then return None

    // 2x unity + migrated = 5 bytes
    if buf.remaining < 5 then
      buf.pos = startPos
      return None

    val pairCount0 = count >> 4
    val pairCount1 = count & 0xf
    // IF see high pair counts, THEN likely not filter but start of next tone
    if pairCount0 > 3 || pairCount1 > 3 then
      buf.pos = startPos
      return None

    val unity0 = buf.readU16BE()
    val unity1 = buf.readU16BE()
    val migrated = buf.readU8()

    // (cnt0 + cnt1) * 2 fields * 2 bytes (u16) = (cnt0+cnt1) * 4
    val baseBytes = (pairCount0 + pairCount1) * 4
    if buf.remaining < baseBytes then
      buf.pos = startPos
      return None

    // count bits set in 'migrated' for active pairs
    var migratedPairsCount = 0
    for dir <- 0 until 2 do
      val pairs = if dir == 0 then pairCount0 else pairCount1
      for p <- 0 until pairs do
        if (migrated & (1 << (dir * 4) << p)) != 0 then migratedPairsCount += 1

    val migratedBytes = migratedPairsCount * 4
    if buf.remaining < baseBytes + migratedBytes then
      buf.pos = startPos
      return None

    val hasEnvelope = migrated != 0 || unity1 != unity0
    if hasEnvelope then
      // need at least 1 byte for envelope len
      val dataPending = baseBytes + migratedBytes
      if buf.remaining < dataPending + 1 then
        buf.pos = startPos
        return None

      val savedPos = buf.pos
      buf.pos += dataPending // skip pairs temp
      val envLen = buf.readU8()
      val envBytes = envLen * 4 // dur(2) + peak(2)

      if buf.remaining < envBytes then
        buf.pos = startPos
        return None

      buf.pos = savedPos // restore to start of pairs

    val maxPairs = math.max(pairCount0, pairCount1)
    val pairPhase = Array.ofDim[Int](2, 2, maxPairs)
    val pairMagnitude = Array.ofDim[Int](2, 2, maxPairs)

    readBasePairs(buf, pairCount0, pairCount1, pairPhase, pairMagnitude)
    readMigratedPairs(
      buf,
      pairCount0,
      pairCount1,
      migrated,
      pairPhase,
      pairMagnitude
    )

    val envelope =
      if hasEnvelope then
        val env = readEnvelopeSegments(buf)
        Some(env.copy(start = Int16.Range, end = Int16.Range))
      else None

    Some(
      Filter(
        Array(pairCount0, pairCount1),
        Array(unity0, unity1),
        pairPhase,
        pairMagnitude,
        envelope
      )
    )

  private def readBasePairs(
      buf: BinaryBuffer,
      p0: Int,
      p1: Int,
      phs: Array[Array[Array[Int]]],
      mag: Array[Array[Array[Int]]]
  ): Unit =
    for p <- 0 until p0 do
      phs(0)(0)(p) = buf.readU16BE()
      mag(0)(0)(p) = buf.readU16BE()
    for p <- 0 until p1 do
      phs(1)(0)(p) = buf.readU16BE()
      mag(1)(0)(p) = buf.readU16BE()

  private def readMigratedPairs(
      buf: BinaryBuffer,
      p0: Int,
      p1: Int,
      migrated: Int,
      phs: Array[Array[Array[Int]]],
      mag: Array[Array[Array[Int]]]
  ): Unit =
    for dir <- 0 until 2 do
      val pairs = if dir == 0 then p0 else p1
      for p <- 0 until pairs do
        if (migrated & (1 << (dir * 4) << p)) != 0 then
          phs(dir)(1)(p) = buf.readU16BE()
          mag(dir)(1)(p) = buf.readU16BE()
        else
          phs(dir)(1)(p) = phs(dir)(0)(p)
          mag(dir)(1)(p) = mag(dir)(0)(p)

  private def readEnvelopeSegments(buf: BinaryBuffer): Envelope =
    val length = buf.readU8()
    val segments = (0 until length).map { _ =>
      val dur = buf.readU16BE()
      val peak = buf.readU16BE()
      EnvelopeSegment(dur, peak)
    }.toVector

    Envelope(WaveForm.Off, 0, 0, segments)

  private def readEnvelope(buf: BinaryBuffer): Envelope =
    val formId = buf.readU8()
    val start = buf.readS32BE()
    val end = buf.readS32BE()
    val form = WaveForm.fromId(formId)

    val segmentLength = buf.readU8()
    // each segment = 4 bytes (2x u16)
    val maxSegments = buf.remaining / 4
    val actualSegmentCount = math.min(segmentLength, maxSegments)
    if actualSegmentCount < segmentLength then
      scribe.warn(
        s"Segment count truncated from $segmentLength to $actualSegmentCount (buffer exhausted)"
      )

    val segments = (0 until actualSegmentCount).map { _ =>
      EnvelopeSegment(buf.readU16BE(), buf.readU16BE())
    }.toVector

    Envelope(form, start, end, segments)

  private def readOptionalEnvelopePair(
      buf: BinaryBuffer
  ): (Option[Envelope], Option[Envelope]) =
    val marker = buf.peek()
    if marker != 0 then
      val env1 = readEnvelope(buf)
      val env2 = readEnvelope(buf)
      (Some(env1), Some(env2))
    else
      buf.pos += 1 // eat '0' flag
      (None, None)

  private def readHarmonics(buf: BinaryBuffer): Vector[Harmonic] =
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
