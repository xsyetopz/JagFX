package jagfx.io

import jagfx.Constants.Smart
import jagfx.Constants.Int16

class BinaryBufferSuite extends munit.FunSuite:

  test("readU8 reads unsigned byte"):
    val buf = BinaryBuffer(Array[Byte](0x00, 0x7f, 0x80.toByte, 0xff.toByte))
    assertEquals(buf.readU8(), 0)
    assertEquals(buf.readU8(), Byte.MaxValue.toInt)
    assertEquals(buf.readU8(), Byte.MaxValue + 1)
    assertEquals(buf.readU8(), 255)

  test("readS8 reads signed byte"):
    val buf = BinaryBuffer(Array[Byte](0x00, 0x7f, 0x80.toByte, 0xff.toByte))
    assertEquals(buf.readS8(), 0)
    assertEquals(buf.readS8(), Byte.MaxValue.toInt)
    assertEquals(buf.readS8(), Byte.MinValue.toInt)
    assertEquals(buf.readS8(), -1)

  test("readU16BE reads big-endian unsigned short"):
    val buf = BinaryBuffer(
      Array[Byte](0x00, 0x01, 0x01, 0x00, 0xff.toByte, 0xff.toByte)
    )
    assertEquals(buf.readU16BE(), 1)
    assertEquals(buf.readU16BE(), 256)
    assertEquals(buf.readU16BE(), 65535)

  test("readS16BE reads big-endian signed short"):
    val buf = BinaryBuffer(
      Array[Byte](0x00, 0x01, 0x7f, 0xff.toByte, 0x80.toByte, 0x00)
    )
    assertEquals(buf.readS16BE(), 1)
    assertEquals(buf.readS16BE(), Short.MaxValue.toInt)
    assertEquals(buf.readS16BE(), Short.MinValue.toInt)

  test("readS32BE reads big-endian signed int"):
    val buf = BinaryBuffer(
      Array[Byte](
        0x00,
        0x00,
        0x00,
        0x01,
        0xff.toByte,
        0xff.toByte,
        0xff.toByte,
        0xff.toByte
      )
    )
    assertEquals(buf.readS32BE(), 1)
    assertEquals(buf.readS32BE(), -1)

  test("readSmartUnsigned reads 1 or 2 bytes"):
    val buf = BinaryBuffer(Array[Byte](0x00, 0x7f, 0x80.toByte, 0x80.toByte))
    assertEquals(buf.readSmartUnsigned(), 0)
    assertEquals(buf.readSmartUnsigned(), Byte.MaxValue.toInt)
    assertEquals(buf.readSmartUnsigned(), Byte.MaxValue + 1)

  test("readSmart reads signed smart value"):
    val buf = BinaryBuffer(Array[Byte](0x40, 0x00, 0x7f))
    assertEquals(buf.readSmart(), 0)
    assertEquals(buf.readSmart(), -64)
    assertEquals(buf.readSmart(), 63)

  test("writeU8 writes unsigned byte"):
    val buf = BinaryBuffer(4)
    buf.writeU8(0)
    buf.writeU8(Byte.MaxValue)
    buf.writeU8(Byte.MaxValue + 1)
    buf.writeU8(255)
    assertEquals(
      buf.data.toSeq,
      Seq[Byte](0x00, 0x7f, 0x80.toByte, 0xff.toByte)
    )

  test("writeU16BE writes big-endian unsigned short"):
    val buf = BinaryBuffer(4)
    buf.writeU16BE(1)
    buf.writeU16BE(256)
    assertEquals(buf.data.toSeq, Seq[Byte](0x00, 0x01, 0x01, 0x00))

  test("writeS32BE writes big-endian signed int"):
    val buf = BinaryBuffer(8)
    buf.writeS32BE(1)
    buf.writeS32BE(-1)
    assertEquals(
      buf.data.toSeq,
      Seq[Byte](
        0x00,
        0x00,
        0x00,
        0x01,
        0xff.toByte,
        0xff.toByte,
        0xff.toByte,
        0xff.toByte
      )
    )

  test("writeS32LE writes little-endian signed int"):
    val buf = BinaryBuffer(4)
    buf.writeS32LE(1)
    assertEquals(buf.data.toSeq, Seq[Byte](0x01, 0x00, 0x00, 0x00))

  test("position tracking"):
    val buf = BinaryBuffer(Array[Byte](1, 2, 3, 4))
    assertEquals(buf.pos, 0)
    buf.readU8()
    assertEquals(buf.pos, 1)
    buf.readU16BE()
    assertEquals(buf.pos, 3)
    assertEquals(buf.remaining, 1)
