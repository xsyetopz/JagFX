package jagfx.io

import jagfx.Constants.Smart
import jagfx.Constants

/** Binary buffer for reading and writing primitive types with explicit
  * endianness. Tracks current position for sequential access.
  *
  * @param data
  *   Underlying byte array
  */
class BinaryBuffer(val data: Array[Byte]):
  /** Current read/write position. */
  var pos: Int = 0

  /** Creates buffer with specified size. */
  def this(size: Int) = this(new Array[Byte](size))

  /** Returns remaining bytes from current position. */
  def remaining: Int = data.length - pos

  /** Peeks at byte at current position without advancing. */
  def peek(): Int = data(pos) & 0xff

  /** Reads unsigned 8-bit integer, advances position by 1. */
  def readU8(): Int =
    val v = data(pos) & 0xff
    pos += 1
    v

  /** Reads signed 8-bit integer, advances position by 1. */
  def readS8(): Int =
    val v = data(pos)
    pos += 1
    v

  /** Reads unsigned 16-bit big-endian integer, advances position by 2. */
  def readU16BE(): Int =
    pos += 2
    ((data(pos - 2) & 0xff) << 8) + (data(pos - 1) & 0xff)

  /** Reads signed 16-bit big-endian integer, advances position by 2. */
  def readS16BE(): Int =
    import Constants._
    pos += 2
    var value = ((data(pos - 2) & 0xff) << 8) + (data(pos - 1) & 0xff)
    if value > Int16.Max then value -= Int16.Range
    value

  /** Reads signed 32-bit big-endian integer, advances position by 4. */
  def readS32BE(): Int =
    pos += 4
    ((data(pos - 4) & 0xff) << 24) +
      ((data(pos - 3) & 0xff) << 16) +
      ((data(pos - 2) & 0xff) << 8) +
      (data(pos - 1) & 0xff)

  /** Reads signed variable-length smart integer (1 or 2 bytes). */
  def readSmartSigned(): Int =
    val value = data(pos) & 0xff
    if value < Smart.Threshold then readU8() - Smart.SignedOffset
    else readU16BE() - Smart.SignedBaseOffset

  /** Reads unsigned variable-length smart integer (1 or 2 bytes). */
  def readSmartUnsigned(): Int =
    import Constants._
    val value = data(pos) & 0xff
    if value < Smart.Threshold then readU8()
    else readU16BE() - Int16.UnsignedMid

  /** Writes signed 32-bit big-endian integer, advances position by 4. */
  def writeS32BE(value: Int): Unit =
    data(pos) = (value >> 24).toByte
    data(pos + 1) = (value >> 16).toByte
    data(pos + 2) = (value >> 8).toByte
    data(pos + 3) = value.toByte
    pos += 4

  /** Writes signed 32-bit little-endian integer, advances position by 4. */
  def writeS32LE(value: Int): Unit =
    data(pos) = value.toByte
    data(pos + 1) = (value >> 8).toByte
    data(pos + 2) = (value >> 16).toByte
    data(pos + 3) = (value >> 24).toByte
    pos += 4

  /** Writes signed 16-bit little-endian integer, advances position by 2. */
  def writeS16LE(value: Int): Unit =
    data(pos) = value.toByte
    data(pos + 1) = (value >> 8).toByte
    pos += 2

  /** Writes unsigned 8-bit integer, advances position by 1. */
  def writeU8(value: Int): Unit =
    data(pos) = value.toByte
    pos += 1

  /** Writes unsigned 16-bit big-endian integer, advances position by 2. */
  def writeU16BE(value: Int): Unit =
    data(pos) = (value >> 8).toByte
    data(pos + 1) = value.toByte
    pos += 2

  /** Writes unsigned variable-length smart integer (1 or 2 bytes). */
  def writeSmartUnsigned(value: Int): Unit =
    if value < Smart.Threshold then writeU8(value)
    else
      writeU8((value >> 8) + Smart.Threshold)
      writeU8(value & 0xff)

  /** Writes signed variable-length smart integer (1 or 2 bytes). */
  def writeSmartSigned(value: Int): Unit =
    val adjusted = value + Smart.SignedOffset
    if adjusted >= 0 && adjusted < Smart.Threshold then writeU8(adjusted)
    else
      val v = value + Smart.SignedBaseOffset
      writeU8((v >> 8) & 0xff)
      writeU8(v & 0xff)
