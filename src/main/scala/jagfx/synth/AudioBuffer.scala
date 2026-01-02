package jagfx.synth

/** Container for synthesized audio samples with mixing and conversion
  * operations.
  *
  * @param samples
  *   16-bit signed PCM samples
  * @param sampleRate
  *   Samples per second (default `22050` Hz)
  */
class AudioBuffer(val samples: Array[Int], val sampleRate: Int = 22050):
  /** Returns number of samples in buffer. */
  def length: Int = samples.length

  /** Mixes another buffer into this one at specified sample offset. */
  def mix(other: AudioBuffer, offset: Int): AudioBuffer =
    val maxLen = math.max(samples.length, other.samples.length + offset)
    val result = new Array[Int](maxLen)
    System.arraycopy(samples, 0, result, 0, samples.length)
    for i <- 0 until other.samples.length do
      val pos = i + offset
      if pos >= 0 && pos < maxLen then result(pos) += other.samples(i)
    AudioBuffer(result, sampleRate)

  /** Clips samples to 16-bit signed range (`-32768` to `32767`). */
  def clip(): AudioBuffer =
    val clipped = samples.map { s =>
      if s < -32768 then -32768
      else if s > 32767 then 32767
      else s
    }
    AudioBuffer(clipped, sampleRate)

  /** Converts 16-bit samples to 8-bit unsigned WAV format. */
  def toBytes: Array[Byte] =
    samples.map(s => ((s >> 8) + 128).toByte)

/** AudioBuffer factory methods. */
object AudioBuffer:
  /** Creates zero-filled buffer with specified sample count. */
  def empty(sampleCount: Int, sampleRate: Int = 22050): AudioBuffer =
    AudioBuffer(new Array[Int](sampleCount), sampleRate)

  /** Creates silence-filled buffer (8-bit midpoint). */
  def silence(sampleCount: Int): AudioBuffer =
    AudioBuffer(Array.fill(sampleCount)(-128), 22050)
