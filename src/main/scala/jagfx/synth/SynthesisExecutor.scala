package jagfx.synth

import javafx.application.Platform
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import jagfx.model._

/** Executes synthesis operations on background thread. */
object SynthesisExecutor:
  private val _executor = Executors.newSingleThreadExecutor { r =>
    val t = new Thread(r, "JagFX-Synth")
    t.setDaemon(true)
    t.setPriority(Thread.NORM_PRIORITY - 1)
    t
  }

  private val _generation = new AtomicLong(0)

  /** Synthesizes tone on background thread with auto-cancellation. */
  def synthesizeTone(tone: Tone)(onComplete: AudioBuffer => Unit): Unit =
    val thisGen = _generation.incrementAndGet()
    _executor.submit(
      (
          () =>
            if _generation.get() == thisGen then
              val audio = ToneSynthesizer.synthesize(tone)
              if _generation.get() == thisGen then
                Platform.runLater(() => onComplete(audio))
      ): Runnable
    )

  /** Synthesizes full track on background thread. */
  def synthesizeTrack(
      file: SynthFile,
      loopCount: Int,
      toneFilter: Int = -1
  )(onComplete: AudioBuffer => Unit): Unit =
    val thisGen = _generation.incrementAndGet()
    _executor.submit(
      (
          () =>
            if _generation.get() == thisGen then
              val audio =
                TrackSynthesizer.synthesize(file, loopCount, toneFilter)
              if _generation.get() == thisGen then
                Platform.runLater(() => onComplete(audio))
      ): Runnable
    )

  /** Cancels all pending synthesis operations. */
  def cancelPending(): Unit = _generation.incrementAndGet()

  /** Shuts down executor. */
  def shutdown(): Unit = _executor.shutdownNow()
