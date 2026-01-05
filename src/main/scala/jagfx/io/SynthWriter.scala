package jagfx.io

import java.nio.file.*

import jagfx.Constants
import jagfx.model.*
import jagfx.types.*

/** Serializer for `SynthFile` domain model to `.synth` binary format. */
object SynthWriter:
  /** Serializes `SynthFile` to `.synth` binary data. */
  def write(file: SynthFile): Array[Byte] =
    val buf = BinaryBuffer(4096)
    writeTones(buf, file.tones)
    buf.writeUInt16BE(file.loop.begin)
    buf.writeUInt16BE(file.loop.end)
    buf.data.take(buf.position)

  /** Writes `SynthFile` to filesystem path. */
  def writeToPath(file: SynthFile, path: Path): Unit =
    val bytes = write(file)
    Files.write(path, bytes)

  private def writeTones(
      buf: BinaryBuffer,
      tones: Vector[Option[Tone]]
  ): Unit =
    for tone <- tones.take(Constants.MaxTones) do
      tone match
        case Some(t) => writeTone(buf, t)
        case None    => buf.writeUInt8(0)

  private def writeTone(buf: BinaryBuffer, tone: Tone): Unit =
    writeEnvelope(buf, tone.pitchEnvelope)
    writeEnvelope(buf, tone.volumeEnvelope)
    writeOptionalEnvelopePair(buf, tone.vibratoRate, tone.vibratoDepth)
    writeOptionalEnvelopePair(buf, tone.tremoloRate, tone.tremoloDepth)
    writeOptionalEnvelopePair(buf, tone.gateSilence, tone.gateDuration)
    writePartials(buf, tone.partials)
    buf.writeUSmart(USmart(tone.echoDelay))
    buf.writeUSmart(USmart(tone.echoMix))
    buf.writeUInt16BE(tone.duration)
    buf.writeUInt16BE(tone.start)
    writeFilter(buf, tone.filter)

  private def writeEnvelope(buf: BinaryBuffer, env: Envelope): Unit =
    buf.writeUInt8(env.waveform.id)
    buf.writeInt32BE(env.start)
    buf.writeInt32BE(env.end)
    buf.writeUInt8(env.segments.length)
    for seg <- env.segments do
      buf.writeUInt16BE(seg.duration)
      buf.writeUInt16BE(seg.peak)

  private def writeOptionalEnvelopePair(
      buf: BinaryBuffer,
      env1: Option[Envelope],
      env2: Option[Envelope]
  ): Unit =
    (env1, env2) match
      case (Some(e1), Some(e2)) =>
        writeEnvelope(buf, e1)
        writeEnvelope(buf, e2)
      case _ =>
        buf.writeUInt8(0)

  private def writePartials(
      buf: BinaryBuffer,
      partials: Vector[Partial]
  ): Unit =
    val activePartials =
      partials.filter(_.volume.value > 0).take(Constants.MaxPartials)
    for height <- activePartials do
      buf.writeUSmart(USmart(height.volume.value))
      buf.writeSmart(Smart(height.pitchOffset))
      buf.writeUSmart(USmart(height.startDelay.value))
    buf.writeUSmart(USmart(0))

  private def writeFilter(buf: BinaryBuffer, filter: Option[Filter]): Unit =
    filter match
      case None    => buf.writeUInt8(0)
      case Some(f) =>
        val p0 = f.pairCounts(0)
        val p1 = f.pairCounts(1)
        buf.writeUInt8((p0 << 4) | p1)
        buf.writeUInt16BE(f.unity(0))
        buf.writeUInt16BE(f.unity(1))

        val migrated = determineMigrationFlags(f, p0, p1)
        buf.writeUInt8(migrated)

        writeBasePairs(buf, f, p0, p1)
        writeMigratedPairs(buf, f, migrated)

        f.envelope match
          case Some(env) => writeEnvelopeSegments(buf, env)
          case None      => // do nothing

  private def determineMigrationFlags(f: Filter, p0: Int, p1: Int): Int =
    var migrated = 0
    for dir <- 0 until 2 do
      val count = f.pairCounts(dir)
      for p <- 0 until count do
        val diffPhase = f.pairPhase(dir)(0)(p) != f.pairPhase(dir)(1)(p)
        val diffMag = f.pairMagnitude(dir)(0)(p) != f.pairMagnitude(dir)(1)(p)
        if diffPhase || diffMag then migrated |= (1 << (dir * 4 + p))

    if f.envelope.isDefined && migrated == 0 && f.unity(0) == f.unity(1) then
      if p0 > 0 then migrated |= 1
      else if p1 > 0 then migrated |= (1 << 4)

    migrated

  private def writeBasePairs(
      buf: BinaryBuffer,
      f: Filter,
      p0: Int,
      p1: Int
  ): Unit =
    for p <- 0 until p0 do
      buf.writeUInt16BE(f.pairPhase(0)(0)(p))
      buf.writeUInt16BE(f.pairMagnitude(0)(0)(p))
    for p <- 0 until p1 do
      buf.writeUInt16BE(f.pairPhase(1)(0)(p))
      buf.writeUInt16BE(f.pairMagnitude(1)(0)(p))

  private def writeMigratedPairs(
      buf: BinaryBuffer,
      f: Filter,
      migrated: Int
  ): Unit =
    for dir <- 0 until 2 do
      val count = f.pairCounts(dir)
      for p <- 0 until count do
        if (migrated & (1 << (dir * 4 + p))) != 0 then
          buf.writeUInt16BE(f.pairPhase(dir)(1)(p))
          buf.writeUInt16BE(f.pairMagnitude(dir)(1)(p))

  private def writeEnvelopeSegments(buf: BinaryBuffer, env: Envelope): Unit =
    buf.writeUInt8(env.segments.length)
    for seg <- env.segments do
      buf.writeUInt16BE(seg.duration)
      buf.writeUInt16BE(seg.peak)
