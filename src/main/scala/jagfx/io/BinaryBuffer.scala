package jagfx.io

import jagfx.types._
import jagfx.constants

/** Binary buffer for reading and writing primitive types with explicit
  * endianness. Tracks current position for sequential access.
  *
  * @param data
  *   Underlying byte array
  */
class BinaryBuffer(val data: Array[Byte]):
  private var _position: Int = 0
  private var _truncated: Boolean = false

  /** Current read/write position (read-only). */
  def position: Int = _position

  /** True if read operations have exceeded buffer length (EOF). */
  def isTruncated: Boolean = _truncated

  /** Advances position by `n` bytes without reading. */
  def skip(n: Int): Unit = _position += n

  /** Creates buffer with specified size. */
  def this(size: Int) = this(new Array[Byte](size))

  /** Returns remaining bytes from current position. */
  def remaining: Int = data.length - _position

  /** Peeks at byte at current position without advancing. */
  def peek(): Int =
    if _position >= data.length then 0 else data(_position) & 0xff

  /** Reads unsigned 8-bit integer, advances position by `1`. */
  def readUInt8(): Int =
    if _checkTruncation(1) then 0
    else
      val v = data(_position) & 0xff
      _position += 1
      v

  /** Reads signed 8-bit integer, advances position by `1`. */
  def readInt8(): Int =
    if _checkTruncation(1) then 0
    else
      val v = data(_position)
      _position += 1
      v

  /** Reads unsigned 16-bit big-endian integer, advances position by `2`. */
  def readUInt16BE(): Int =
    if _checkTruncation(2) then 0
    else
      _position += 2
      ((data(_position - 2) & 0xff) << 8) + (data(_position - 1) & 0xff)

  /** Reads unsigned 16-bit little-endian integer, advances position by `2`. */
  def readUInt16LE(): Int =
    if _checkTruncation(2) then 0
    else
      _position += 2
      (data(_position - 2) & 0xff) + ((data(_position - 1) & 0xff) << 8)

  /** Reads signed 16-bit big-endian integer, advances position by `2`. */
  def readInt16BE(): Int =
    if _checkTruncation(2) then 0
    else
      import constants._
      _position += 2
      var value =
        ((data(_position - 2) & 0xff) << 8) + (data(_position - 1) & 0xff)
      if value > Short.MaxValue then value -= Int16.Range
      value

  /** Reads signed 32-bit big-endian integer, advances position by `4`. */
  def readInt32BE(): Int =
    if _checkTruncation(4) then 0
    else
      _position += 4
      ((data(_position - 4) & 0xff) << 24) +
        ((data(_position - 3) & 0xff) << 16) +
        ((data(_position - 2) & 0xff) << 8) +
        (data(_position - 1) & 0xff)

  /** Reads signed variable-length smart integer (`1` or `2` bytes). */
  def readSmart(): Smart =
    if remaining == 0 then return Smart(0)
    val value = peek()
    if value < constants.Smart.Threshold then
      Smart(readUInt8() - constants.Smart.SignedOffset)
    else Smart(readUInt16BE() - constants.Smart.SignedBaseOffset)

  /** Reads unsigned variable-length smart integer (`1` or `2` bytes). */
  def readUSmart(): USmart =
    import constants._
    if remaining == 0 then return USmart(0)
    val value = peek()
    if value < constants.Smart.Threshold then USmart(readUInt8())
    else USmart(readUInt16BE() - Int16.UnsignedMaxValue)

  /** Writes signed 32-bit big-endian integer, advances position by `4`. */
  def writeInt32BE(value: Int): Unit =
    data(_position) = (value >> 24).toByte
    data(_position + 1) = (value >> 16).toByte
    data(_position + 2) = (value >> 8).toByte
    data(_position + 3) = value.toByte
    _position += 4

  /** Writes signed 32-bit little-endian integer, advances position by `4`. */
  def writeInt32LE(value: Int): Unit =
    data(_position) = value.toByte
    data(_position + 1) = (value >> 8).toByte
    data(_position + 2) = (value >> 16).toByte
    data(_position + 3) = (value >> 24).toByte
    _position += 4

  /** Writes signed 16-bit little-endian integer, advances position by `2`. */
  def writeInt16LE(value: Int): Unit =
    data(_position) = value.toByte
    data(_position + 1) = (value >> 8).toByte
    _position += 2

  /** Writes unsigned 8-bit integer, advances position by `1`. */
  def writeUInt8(value: Int): Unit =
    data(_position) = value.toByte
    _position += 1

  /** Writes unsigned 16-bit big-endian integer, advances position by `2`. */
  def writeUInt16BE(value: Int): Unit =
    data(_position) = (value >> 8).toByte
    data(_position + 1) = value.toByte
    _position += 2

  /** Writes unsigned variable-length smart integer (`1` or `2` bytes). */
  def writeUSmart(value: USmart): Unit =
    val v = value.value
    if v < constants.Smart.Threshold then writeUInt8(v)
    else
      writeUInt8((v >> 8) + constants.Smart.Threshold)
      writeUInt8(v & 0xff)

  /** Writes signed variable-length smart integer (`1` or `2` bytes). */
  def writeSmart(value: Smart): Unit =
    val v = value.value
    val adjusted = v + constants.Smart.SignedOffset
    if adjusted >= 0 && adjusted < constants.Smart.Threshold then
      writeUInt8(adjusted)
    else
      val enc = v + constants.Smart.SignedBaseOffset
      writeUInt8((enc >> 8) & 0xff)
      writeUInt8(enc & 0xff)

  private def _checkTruncation(bytes: Int): Boolean =
    if _position + bytes > data.length then
      _truncated = true
      _position += bytes
      true
    else false
