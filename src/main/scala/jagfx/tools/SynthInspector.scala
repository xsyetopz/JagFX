package jagfx.tools

import jagfx.io.BinaryBuffer
import jagfx.model._
import java.nio.file.{Files, Paths}
import jagfx.Constants

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
      inspectTones(buf)

      if buf.remaining > 0 then
        println(s"\n[WARNING] ${buf.remaining} BYTES REMAINING:")
        println(buf.dumpRemaining())
      else println("\n[SUCCESS] EOF REACHED CLEANLY")

    catch
      case e: Exception =>
        println(
          s"\n[ERROR] PARSING FAILED AT OFFSET ${buf.pos}: ${e.getMessage}"
        )
        e.printStackTrace()
        println("REMAINING CONTEXT:")
        println(buf.dumpRemaining(0, 64))

  def inspectTones(buf: DebugBuffer): Unit =
    for i <- 0 until 10 do
      if buf.remaining > 0 then
        val valid = buf.peek() != 0
        println(f"\n=== TONE $i (OFFSET: ${buf.pos}%04X) ===")

        val marker = buf.peek()
        println(f"HEADER MARKER: $marker (VALID: ${marker != 0})")
        if marker != 0 then inspectTone(buf)
        else
          buf.readU8("EMPTY MARKER")
          println(s"Tone $i empty")
      else
        // println(s"Tone $i: EOF reached unexpectedly (Truncated Tone Loop?)")
        (
      )

  def inspectTone(buf: DebugBuffer): Unit =
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

    inspectHarmonics(buf)

    println("--- PARAMETERS ---")
    buf.readSmartUnsigned("REVERB DELAY")
    buf.readSmartUnsigned("REVERB VOL")
    buf.readU16BE("DURATION")
    buf.readU16BE("START")

    inspectFilter(buf)

  def inspectEnvelope(buf: DebugBuffer): Unit =
    val formId = buf.readU8("FORM ID")
    val start = buf.readS32BE("START VAL")
    val end = buf.readS32BE("END VAL")
    val segLen = buf.readU8("SEG COUNT")
    for i <- 0 until segLen do
      buf.readU16BE(s"SEG $i DUR")
      buf.readU16BE(s"SEG $i PEAK")

  def inspectOptionalEnvelopePair(buf: DebugBuffer): Unit =
    val marker = buf.peek()
    println(s"  MARKER: $marker")
    if marker != 0 then
      println("  READING ENVELOPE 1:")
      inspectEnvelope(buf)
      println("  READING ENVELOPE 2:")
      inspectEnvelope(buf)
    else buf.readU8("EMPTY MARKER")

  def inspectHarmonics(buf: DebugBuffer): Unit =
    println("--- HARMONICS ---")
    var continue = true
    var count = 0
    while continue && count < Constants.MaxHarmonics do
      val marker = buf.peek()
      if marker != 0 then
        val term = buf.readSmart("H. TERM VOL")
        buf.readSmart("H. SEMITONE")
        buf.readSmart("H. DELAY")
        // println(s"  Harmonic $count read")
        count += 1
      else
        buf.readSmart("H. END MARKER")
        continue = false
        // println("  Harmonics ended")

  def inspectFilter(buf: DebugBuffer): Unit =
    println("--- FILTER ---")
    if buf.remaining == 0 then
      println("  EOF reached before Filter read")
      return

    val packed = buf.readU8("PACKED PAIRS")
    val pair0 = packed >> 4
    val pair1 = packed & 0xf
    println(s"  PAIRS: $pair0, $pair1")

    if packed == 0 then
      println("  FILTER EMPTY (PAIRS=0)")
      return

    val unity0 = buf.readU16BE("UNITY 0")
    val unity1 = buf.readU16BE("UNITY 1")
    val modMask = buf.readU8("MOD MASK")
    println(s"  MOD MASK: $modMask")

    for ch <- 0 until 2 do
      val pairs = if ch == 0 then pair0 else pair1
      println(s"  CHANNEL $ch (PAIRS: $pairs):")
      for p <- 0 until pairs do
        buf.readU16BE(s"    FREQ $p")
        buf.readU16BE(s"    MAG $p")

    for ch <- 0 until 2 do
      val pairs = if ch == 0 then pair0 else pair1
      println(s"  CHANNEL $ch MODULATION:")
      for p <- 0 until pairs do
        if (modMask & (1 << (ch * 4) << p)) != 0 then
          buf.readU16BE(s"    FREQ MOD $p")
          buf.readU16BE(s"    MAG MOD $p")
        else println(s"    MOD $p SKIPPED (MASK 0)")

    if modMask != 0 || unity1 != unity0 then
      println("READING FILTER ENVELOPE SEGMENTS:")
      inspectEnvelopeSegments(buf)
    else println("FILTER ENVELOPE SKIPPED")

  def inspectEnvelopeSegments(buf: DebugBuffer): Unit =
    val length = buf.readU8("SEG COUNT")
    for i <- 0 until length do
      buf.readU16BE(s"SEG $i DUR")
      buf.readU16BE(s"SEG $i PEAK")

class DebugBuffer(data: Array[Byte]) extends BinaryBuffer(data):
  def readU8(label: String): Int =
    val v = super.readU8()
    _log(1, label, v)
    v
  def readS32BE(label: String): Int =
    val v = super.readS32BE()
    _log(4, label, v)
    v
  def readU16BE(label: String): Int =
    val v = super.readU16BE()
    _log(2, label, v)
    v
  def readSmart(label: String): Int =
    val startPos = pos
    val v = super.readSmart()
    val len = pos - startPos
    _log(len, label, v)
    v
  def readSmartUnsigned(label: String): Int =
    val startPos = pos
    val v = super.readSmartUnsigned()
    val len = pos - startPos
    _log(len, label, v)
    v

  private def _log(len: Int, label: String, value: Any): Unit =
    val offsetStr = f"[${pos - len}%04X]"
    val hex =
      (0 until len)
        .map { i =>
          val p = pos - len + i
          if p >= 0 && p < data.length then "%02X".format(data(p))
          else "??"
        }
        .mkString(" ")
    val hexStr = if hex.length > 14 then hex.take(11) + "..." else hex

    println(f"$offsetStr $hexStr%-16s | $label%-20s : $value")

  def dumpRemaining(offset: Int = 0, limit: Int = -1): String =
    val count = if limit < 0 then remaining else math.min(limit, remaining)
    val chunk = data.slice(pos + offset, pos + offset + count)
    chunk.map("%02X".format(_)).mkString(" ")
