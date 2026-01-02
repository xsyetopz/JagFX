package jagfx.io

import jagfx.model._
import jagfx.Constants
import java.nio.file._

/** Serializer for `SynthFile` domain model to `.synth` binary format. */
object SynthWriter:
  /** Serializes `SynthFile` to `.synth` binary data. */
  def write(file: SynthFile): Array[Byte] =
    val buf = BinaryBuffer(4096)
    writeTones(buf, file.tones)
    buf.writeU16BE(file.loop.begin)
    buf.writeU16BE(file.loop.end)
    scribe.debug(s"Serialized ${buf.pos} byte(s)")
    buf.data.take(buf.pos)

  /** Writes `SynthFile` to filesystem path. */
  def writeToPath(file: SynthFile, path: Path): Unit =
    val bytes = write(file)
    Files.write(path, bytes)
    scribe.info(s"Wrote ${bytes.length} byte(s) to $path")

  private def writeTones(buf: BinaryBuffer, tones: Vector[Option[Tone]]): Unit =
    for tone <- tones.take(Constants.MaxTones) do
      tone match
        case Some(t) => writeTone(buf, t)
        case None    => buf.writeU8(0)

  private def writeTone(buf: BinaryBuffer, tone: Tone): Unit =
    writeEnvelope(buf, tone.pitchEnvelope)
    writeEnvelope(buf, tone.volumeEnvelope)
    writeOptionalEnvelopePair(buf, tone.vibratoRate, tone.vibratoDepth)
    writeOptionalEnvelopePair(buf, tone.tremoloRate, tone.tremoloDepth)
    writeOptionalEnvelopePair(buf, tone.gateSilence, tone.gateDuration)
    writeHarmonics(buf, tone.harmonics)
    buf.writeSmartUnsigned(tone.reverbDelay)
    buf.writeSmartUnsigned(tone.reverbVolume)
    buf.writeU16BE(tone.duration)
    buf.writeU16BE(tone.start)

  private def writeEnvelope(buf: BinaryBuffer, env: Envelope): Unit =
    buf.writeU8(env.form.id)
    buf.writeS32BE(env.start)
    buf.writeS32BE(env.end)
    buf.writeU8(env.segments.length)
    for seg <- env.segments do
      buf.writeU16BE(seg.duration)
      buf.writeU16BE(seg.peak)

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
        buf.writeU8(0)

  private def writeHarmonics(
      buf: BinaryBuffer,
      harmonics: Vector[Harmonic]
  ): Unit =
    for h <- harmonics.take(Constants.MaxTones) do
      buf.writeSmartUnsigned(h.volume)
      buf.writeSmartSigned(h.semitone)
      buf.writeSmartUnsigned(h.delay)
    buf.writeSmartUnsigned(0)
