package jagfx.tools

import java.nio.file.*

import jagfx.Constants
import jagfx.io.BinaryBuffer
import jagfx.types.*

/** Debug tool for inspecting `.synth` binary file structure. */
object SynthInspector:
  def main(args: Array[String]): Unit =
    if args.isEmpty then
      println("Usage: SynthInspector <path-to-synth-file>")
      return

    val path = Paths.get(args(0))
    if !Files.exists(path) then
      println(s"File not found: $path")
      return

    println(s"INSPECTING: $path")
    val bytes = Files.readAllBytes(path)
    println(s"SIZE: ${bytes.length} bytes\n")

    val buf = new DebugBuffer(bytes)

    try
      inspectVoices(buf)

      if buf.remaining > 0 then
        println(s"\n[WARNING] ${buf.remaining} BYTES REMAINING:")
        println(buf.dumpRemaining())
      else println("\n[SUCCESS] EOF REACHED CLEANLY")

    catch
      case e: Exception =>
        println(
          s"\n[ERROR] PARSING FAILED AT OFFSET ${buf.position}: ${e.getMessage}"
        )
        e.printStackTrace()
        println("REMAINING CONTEXT:")
        println(buf.dumpRemaining(0, 64))

  private def inspectVoices(buf: DebugBuffer): Unit =
    for i <- 0 until 10 do
      if buf.remaining > 0 then
        buf.peek() != 0
        println(f"\n=== VOICE $i (OFFSET: ${buf.position}%04X) ===")

        val marker = buf.peek()
        println(f"HEADER MARKER: $marker (VALID: ${marker != 0})")
        if marker != 0 then inspectVoice(buf)
        else
          buf.readUInt8("EMPTY MARKER")
          println(s"Voice $i empty")

  private def inspectVoice(buf: DebugBuffer): Unit =
    println("--- PITCH ENVELOPE ---")
    inspectEnvelope(buf)
    println("--- VOLUME ENVELOPE ---")
    inspectEnvelope(buf)

    println("--- VIBRATO ---")
    inspectOptionalEnvelopePair(buf)
    println("--- TREMOLO ---")
    inspectOptionalEnvelopePair(buf)
    println("--- GATE ---")
    inspectOptionalEnvelopePair(buf)

    inspectPartials(buf)

    println("--- PARAMETERS ---")
    buf.readUSmart("ECHO DEL")
    buf.readUSmart("ECHO MIX")
    buf.readUInt16BE("DURATION")
    buf.readUInt16BE("START")

    inspectFilter(buf)

  private def inspectEnvelope(buf: DebugBuffer): Unit =
    buf.readUInt8("WAVEFORM ID")
    buf.readInt32BE("START VAL")
    buf.readInt32BE("END VAL")
    val segLen = buf.readUInt8("SEG COUNT")
    for i <- 0 until segLen do
      buf.readUInt16BE(s"SEG $i DUR")
      buf.readUInt16BE(s"SEG $i PEAK")

  private def inspectOptionalEnvelopePair(buf: DebugBuffer): Unit =
    val marker = buf.peek()
    println(s"  MARKER: $marker")
    if marker != 0 then
      println("  READING ENVELOPE 1:")
      inspectEnvelope(buf)
      println("  READING ENVELOPE 2:")
      inspectEnvelope(buf)
    else buf.readUInt8("EMPTY MARKER")

  private def inspectPartials(buf: DebugBuffer): Unit =
    println("--- PARTIALS ---")
    var continue = true
    var count = 0
    while continue && count < Constants.MaxPartials do
      val marker = buf.peek()
      if marker != 0 then
        buf.readSmart("H. TERM VOL")
        buf.readSmart("H. PIT")
        buf.readSmart("H. DEL")
        count += 1
      else
        buf.readSmart("H. END MARKER")
        continue = false

  private def inspectFilter(buf: DebugBuffer): Unit =
    println("--- FILTER ---")
    if buf.remaining == 0 then
      println("  EOF reached before Filter read")
      return

    val packed = buf.readUInt8("PACKED PAIRS")
    val pair0 = packed >> 4
    val pair1 = packed & 0xf
    println(s"  PAIRS: $pair0, $pair1")

    if packed == 0 then
      println("  FILTER EMPTY (PAIRS=0)")
      return

    buf.readUInt16BE("UNITY 0")
    buf.readUInt16BE("UNITY 1")
    val modMask = buf.readUInt8("MOD MASK")
    println(s"  MOD MASK: $modMask")

    for ch <- 0 until 2 do
      val pairs = if ch == 0 then pair0 else pair1
      println(s"  CHANNEL $ch (PAIRS: $pairs):")
      for p <- 0 until pairs do
        buf.readUInt16BE(s"    FREQ $p")
        buf.readUInt16BE(s"    MAG $p")

    for ch <- 0 until 2 do
      val pairs = if ch == 0 then pair0 else pair1
      println(s"  CHANNEL $ch MODULATION:")
      for p <- 0 until pairs do
        if (modMask & (1 << (ch * 4) << p)) != 0 then
          buf.readUInt16BE(s"    FREQ MOD $p")
          buf.readUInt16BE(s"    MAG MOD $p")
        else println(s"    MOD $p SKIPPED (MASK 0)")

    if modMask != 0 then
      println("READING FILTER ENVELOPE SEGMENTS:")
      inspectEnvelopeSegments(buf)
    else println("FILTER ENVELOPE SKIPPED")

  private def inspectEnvelopeSegments(buf: DebugBuffer): Unit =
    val length = buf.readUInt8("SEG COUNT")
    for i <- 0 until length do
      buf.readUInt16BE(s"SEG $i DUR")
      buf.readUInt16BE(s"SEG $i PEAK")

/** Debug buffer with logging for binary inspection. */
class DebugBuffer(data: Array[Byte]) extends BinaryBuffer(data):
  def readUInt8(label: String): Int =
    val v = super.readUInt8()
    log(1, label, v)
    v

  def readInt32BE(label: String): Int =
    val v = super.readInt32BE()
    log(4, label, v)
    v

  def readUInt16BE(label: String): Int =
    val v = super.readUInt16BE()
    log(2, label, v)
    v

  def readSmart(label: String): Smart =
    val startPos = position
    val v = super.readSmart()
    val len = position - startPos
    log(len, label, v)
    v

  def readUSmart(label: String): USmart =
    val startPos = position
    val v = super.readUSmart()
    val len = position - startPos
    log(len, label, v)
    v

  def dumpRemaining(offset: Int = 0, limit: Int = -1): String =
    val count = if limit < 0 then remaining else math.min(limit, remaining)
    val chunk = data.slice(position + offset, position + offset + count)
    chunk.map("%02X".format(_)).mkString(" ")

  private def log(len: Int, label: String, value: Any): Unit =
    val offsetStr = f"[${position - len}%04X]"
    val hex =
      (0 until len)
        .map { i =>
          val p = position - len + i
          if p >= 0 && p < data.length then "%02X".format(data(p))
          else "??"
        }
        .mkString(" ")
    val hexStr = if hex.length > 14 then hex.take(11) + "..." else hex
    println(f"$offsetStr $hexStr%-16s | $label%-20s : $value")
