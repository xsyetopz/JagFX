package jagfx.io

import jagfx.Constants
import java.nio.file._

/** WAV file format writer for 8-bit mono PCM audio. */
object WavWriter:
  import Constants.Wav._

  /** Converts audio samples to complete WAV file bytes.
    *
    * @param samples
    *   Raw PCM byte data (8-bit or 16-bit LE)
    * @param bitsPerSample
    *   Bits per sample (8 or 16)
    */
  def write(samples: Array[Byte], bitsPerSample: Int = 8): Array[Byte] =
    val dataSize = samples.length
    val fileSize = HeaderSize - 8 + dataSize
    val buf = BinaryBuffer(HeaderSize + dataSize)

    buf.writeS32BE(RiffMagic)
    buf.writeS32LE(fileSize)
    buf.writeS32BE(WaveMagic)
    buf.writeS32BE(FmtMagic)
    buf.writeS32LE(FmtChunkSize)
    buf.writeS16LE(PcmFormat)
    buf.writeS16LE(Constants.NumChannels)
    buf.writeS32LE(Constants.SampleRate)
    buf.writeS32LE(
      Constants.SampleRate * Constants.NumChannels * bitsPerSample / 8
    )
    buf.writeS16LE(Constants.NumChannels * bitsPerSample / 8)
    buf.writeS16LE(bitsPerSample)
    buf.writeS32BE(DataMagic)
    buf.writeS32LE(dataSize)
    System.arraycopy(samples, 0, buf.data, buf.pos, dataSize)
    buf.data

  /** Writes audio samples to WAV file at specified path. */
  def writeToPath(samples: Array[Byte], path: Path): Unit =
    Files.write(path, write(samples))
