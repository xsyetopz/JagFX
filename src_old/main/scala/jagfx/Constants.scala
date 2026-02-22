package jagfx

/** Global constants for synthesis engine and file format. */
object Constants:
  // Audio
  final val SampleRate: Int = 22050
  final val BitsPerSample: Int = 8
  final val NumChannels: Int = 1

  // Limits
  final val MaxVoices: Int = 10
  final val MaxPartials: Int = 10
  final val MaxFilterPairs: Int = 4

  // Buffer pool
  final val MaxBufferSize: Int = 1048576
  final val MaxPoolSize: Int = 20

  // Synthesis
  final val FilterUpdateRate: Int = Byte.MaxValue + 1
  final val MinFrameNanos: Long = 16_666_666L
  final val PhaseScale: Double = 32.768
  final val NoisePhaseDiv: Int = 2607
  final val PhaseMask: Int = 0x7fff

  // Lookup tables
  final val SemitoneRange: Int = 120
  final val SinTableDivisor: Double = 5215.1903
  final val CircleSegments: Int = 64

  /** 16-bit signed integer range constants. */
  object Int16:
    final val Range: Int = 65536
    final val UnsignedMaxValue: Int = 32768
    final val Quarter: Int = 16384

  /** WAV file format magic numbers and sizes. */
  object Wav:
    final val RiffMagic: Int = 0x52494646
    final val WaveMagic: Int = 0x57415645
    final val FmtMagic: Int = 0x666d7420
    final val DataMagic: Int = 0x64617461
    final val HeaderSize: Int = 44
    final val FmtChunkSize: Int = 16
    final val PcmFormat: Int = 1

  /** Smart variable-length integer encoding constants. */
  object Smart:
    final val Threshold: Int = Byte.MaxValue + 1
    final val SignedOffset: Int = 64
    final val SignedBaseOffset: Int = 49152
