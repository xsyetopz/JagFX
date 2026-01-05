package jagfx.synth

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.Arrays
import jagfx.Constants.{MaxPoolSize, MaxBufferSize}

/** Thread-safe pool for reusable integer arrays. */
object BufferPool:
  private val _pool = new ConcurrentLinkedQueue[(Int, Array[Int])]()

  /** Acquires buffer of at least `minSize` elements, zero-filled. */
  def acquire(minSize: Int): Array[Int] =
    if minSize > MaxBufferSize then return new Array[Int](minSize)

    var best: Option[Array[Int]] = None
    var bestSize = Int.MaxValue

    val iter = _pool.iterator()
    while iter.hasNext do
      val (size, buf) = iter.next()
      if size >= minSize && size < bestSize then
        if _pool.remove((size, buf)) then
          best = Some(buf)
          bestSize = size

    val result = best.getOrElse(new Array[Int](minSize))
    Arrays.fill(result, 0)
    result

  /** Returns buffer to pool for reuse. */
  def release(buffer: Array[Int]): Unit =
    if buffer.length <= MaxBufferSize && _pool.size() < MaxPoolSize then
      _pool.offer((buffer.length, buffer))

  /** Clears all pooled buffers. */
  def clear(): Unit = _pool.clear()
