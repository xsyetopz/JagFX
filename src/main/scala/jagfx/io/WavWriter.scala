package jagfx.io

import java.nio.file.*

import jagfx.Constants

/** WAV file format writer for 8-bit mono PCM audio. */
object WavWriter:
  import Constants.Wav._

  /** Converts audio samples to complete WAV file bytes.
    *
    * @param samples
    *   Raw PCM byte data (8-bit or 16-bit LE)
    * @param bitsPerSample
    *   Bits per sample (`8` or `16`)
    */
  def write(samples: Array[Byte], bitsPerSample: Int = 8): Array[Byte] =
    val dataSize = samples.length
    val fileSize = HeaderSize - 8 + dataSize
    val buf = BinaryBuffer(HeaderSize + dataSize)

    buf.writeInt32BE(RiffMagic)
    buf.writeInt32LE(fileSize)
    buf.writeInt32BE(WaveMagic)
    buf.writeInt32BE(FmtMagic)
    buf.writeInt32LE(FmtChunkSize)
    buf.writeInt16LE(PcmFormat)
    buf.writeInt16LE(Constants.NumChannels)
    buf.writeInt32LE(Constants.SampleRate)
    buf.writeInt32LE(
      Constants.SampleRate * Constants.NumChannels * bitsPerSample / 8
    )
    buf.writeInt16LE(Constants.NumChannels * bitsPerSample / 8)
    buf.writeInt16LE(bitsPerSample)
    buf.writeInt32BE(DataMagic)
    buf.writeInt32LE(dataSize)
    System.arraycopy(samples, 0, buf.data, buf.position, dataSize)
    buf.data

  /** Writes audio samples to WAV file at specified path. */
  def writeToPath(samples: Array[Byte], path: Path): Unit =
    Files.write(path, write(samples))
