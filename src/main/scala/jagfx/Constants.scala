package jagfx

/** Public constants for external use (e.g., GUI, plugins). */
object constants:
  /** Audio sample rate in Hz. */
  val SampleRate: Int = 22050

  /** Bits per audio sample. */
  val BitsPerSample: Int = 8

  /** Number of audio channels (mono). */
  val NumChannels: Int = 1

  /** Maximum tones per synth file. */
  val MaxTones: Int = 10

  /** Maximum partials per tone. */
  val MaxPartials: Int = 10

  /** Maximum filter pole/zero pairs per direction. */
  val MaxFilterPairs: Int = 4

  /** Buffer pool constants. */
  val MaxBufferSize: Int = 1048576 // 1MB
  val MaxPoolSize: Int = 20

  /** Filter coefficient update rate (samples per update). */
  val FilterUpdateRate: Int = Byte.MaxValue + 1

  /** Minimum frame duration for canvas rendering (60 FPS). */
  val MinFrameNanos: Long = 16_666_666L

  /** Phase accumulator constants for synthesis. */
  val PhaseScale: Double = 32.768
  val NoisePhaseDiv: Int = 2607
  val PhaseMask: Int = 0x7fff

  /** Lookup table sizing constants. */
  val SemitoneRange: Int = 120
  val SinTableDivisor: Double = 5215.1903
  val CircleSegments: Int = 64

  /** 16-bit signed integer range constants. */
  object Int16:
    val Range: Int = 65536
    val UnsignedMaxValue: Int = 32768
    val Quarter: Int = 16384

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
    val Threshold: Int = Byte.MaxValue + 1
    val SignedOffset: Int = 64
    val SignedBaseOffset: Int = 49152
