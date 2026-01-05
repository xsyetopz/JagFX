package jagfx.synth

import jagfx.Constants
import jagfx.Constants.SampleRate
import jagfx.utils.MathUtils

/** Container for synthesized audio samples with mixing and conversion
  * operations.
  *
  * @param samples
  *   16-bit signed PCM samples
  * @param sampleRate
  *   Samples per second (default `22050` Hz)
  */
class AudioBuffer(val samples: Array[Int], val sampleRate: Int = SampleRate):
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
    val newSamples = samples.clone()
    MathUtils.clipInt16(newSamples)
    AudioBuffer(newSamples, sampleRate)

  /** Converts 16-bit samples to 8-bit unsigned bytes. */
  def toBytesUnsigned: Array[Byte] =
    samples.map(s => ((s >> 8) + Byte.MaxValue + 1).toByte)

  /** Converts 16-bit samples to 8-bit signed bytes. */
  def toBytes: Array[Byte] =
    samples.map(s => (s >> 8).toByte)

  /** Converts 16-bit samples to 16-bit signed bytes (big-endian). */
  def toBytes16BE: Array[Byte] =
    val result = new Array[Byte](samples.length * 2)
    for i <- samples.indices do
      result(i * 2) = (samples(i) >> 8).toByte
      result(i * 2 + 1) = samples(i).toByte
    result

  /** Converts 16-bit samples to 16-bit signed bytes (little-endian). */
  def toBytes16LE: Array[Byte] =
    val result = new Array[Byte](samples.length * 2)
    for i <- samples.indices do
      result(i * 2) = samples(i).toByte
      result(i * 2 + 1) = (samples(i) >> 8).toByte
    result

/** AudioBuffer factory methods. */
object AudioBuffer:
  /** Creates zero-filled buffer with specified sample count. */
  def empty(sampleCount: Int, sampleRate: Int = SampleRate): AudioBuffer =
    AudioBuffer(new Array[Int](sampleCount), sampleRate)

  /** Creates silence-filled buffer (8-bit midpoint). */
  def silence(sampleCount: Int): AudioBuffer =
    AudioBuffer(Array.fill(sampleCount)(0), SampleRate)
