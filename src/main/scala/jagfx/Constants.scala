package jagfx

/** Public constants for external use (e.g., GUI, plugins). */
object Constants:
  /** Audio sample rate in Hz. */
  val SampleRate: Int = 22050

  /** Bits per audio sample. */
  val BitsPerSample: Int = 8

  /** Number of audio channels (mono). */
  val NumChannels: Int = 1

  /** Maximum tones per synth file. */
  val MaxTones: Int = 10

  /** 16-bit signed integer range constants. */
  object Int16:
    val Min: Int = -32768
    val Max: Int = 32767
    val Range: Int = 65536
    val UnsignedMid: Int = 32768

  /** WAV file format constants. */
  object Wav:
    val RiffMagic: Int = 0x52494646
    val WaveMagic: Int = 0x57415645
    val FmtMagic: Int = 0x666d7420
    val DataMagic: Int = 0x64617461
    val HeaderSize: Int = 44
    val FmtChunkSize: Int = 16
    val PcmFormat: Int = 1

  /** Smart variable-length integer encoding constants. */
  object Smart:
    val Threshold: Int = 128
    val SignedOffset: Int = 64
    val SignedBaseOffset: Int = 49152
