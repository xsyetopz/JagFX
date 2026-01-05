package jagfx.io

import jagfx.types._

class BinaryBufferSuite extends munit.FunSuite:

  test("readInt8 reads unsigned byte"):
    val buf = BinaryBuffer(Array[Byte](0x00, 0x7f, 0x80.toByte, 0xff.toByte))
    assertEquals(buf.readUInt8(), 0)
    assertEquals(buf.readUInt8(), Byte.MaxValue.toInt)
    assertEquals(buf.readUInt8(), Byte.MaxValue + 1)
    assertEquals(buf.readUInt8(), 255)

  test("readUInt8 reads signed byte"):
    val buf = BinaryBuffer(Array[Byte](0x00, 0x7f, 0x80.toByte, 0xff.toByte))
    assertEquals(buf.readInt8(), 0)
    assertEquals(buf.readInt8(), Byte.MaxValue.toInt)
    assertEquals(buf.readInt8(), Byte.MinValue.toInt)
    assertEquals(buf.readInt8(), -1)

  test("readUInt16BE reads big-endian unsigned short"):
    val buf = BinaryBuffer(
      Array[Byte](0x00, 0x01, 0x01, 0x00, 0xff.toByte, 0xff.toByte)
    )
    assertEquals(buf.readUInt16BE(), 1)
    assertEquals(buf.readUInt16BE(), 256)
    assertEquals(buf.readUInt16BE(), 65535)

  test("readInt16BE reads big-endian signed short"):
    val buf = BinaryBuffer(
      Array[Byte](0x00, 0x01, 0x7f, 0xff.toByte, 0x80.toByte, 0x00)
    )
    assertEquals(buf.readInt16BE(), 1)
    assertEquals(buf.readInt16BE(), Short.MaxValue.toInt)
    assertEquals(buf.readInt16BE(), Short.MinValue.toInt)

  test("readInt32BE reads big-endian signed int"):
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
    assertEquals(buf.readInt32BE(), 1)
    assertEquals(buf.readInt32BE(), -1)

  test("readSmartUnsigned reads 1 or 2 bytes"):
    val buf = BinaryBuffer(Array[Byte](0x00, 0x7f, 0x80.toByte, 0x80.toByte))
    assertEquals(buf.readUSmart(), USmart(0))
    assertEquals(buf.readUSmart(), USmart(Byte.MaxValue.toInt))
    assertEquals(buf.readUSmart(), USmart(Byte.MaxValue + 1))

  test("readSmart reads signed smart value"):
    val buf = BinaryBuffer(Array[Byte](0x40, 0x00, 0x7f))
    assertEquals(buf.readSmart(), Smart(0))
    assertEquals(buf.readSmart(), Smart(-64))
    assertEquals(buf.readSmart(), Smart(63))

  test("writeInt8 writes unsigned byte"):
    val buf = BinaryBuffer(4)
    buf.writeUInt8(0)
    buf.writeUInt8(Byte.MaxValue)
    buf.writeUInt8(Byte.MaxValue + 1)
    buf.writeUInt8(255)
    assertEquals(
      buf.data.toSeq,
      Seq[Byte](0x00, 0x7f, 0x80.toByte, 0xff.toByte)
    )

  test("writeUInt16BE writes big-endian unsigned short"):
    val buf = BinaryBuffer(4)
    buf.writeUInt16BE(1)
    buf.writeUInt16BE(256)
    assertEquals(buf.data.toSeq, Seq[Byte](0x00, 0x01, 0x01, 0x00))

  test("writeInt32BE writes big-endian signed int"):
    val buf = BinaryBuffer(8)
    buf.writeInt32BE(1)
    buf.writeInt32BE(-1)
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

  test("writeInt32LE writes little-endian signed int"):
    val buf = BinaryBuffer(4)
    buf.writeInt32LE(1)
    assertEquals(buf.data.toSeq, Seq[Byte](0x01, 0x00, 0x00, 0x00))

  test("position tracking"):
    val buf = BinaryBuffer(Array[Byte](1, 2, 3, 4))
    assertEquals(buf.position, 0)
    buf.readUInt8()
    assertEquals(buf.position, 1)
    buf.readUInt16BE()
    assertEquals(buf.position, 3)
    assertEquals(buf.remaining, 1)
