package jagfx.ui.controller.header

import java.util.concurrent.atomic.AtomicLong
import javax.sound.sampled.*

import jagfx.constants
import jagfx.model.SynthFile
import jagfx.synth.TrackSynthesizer
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.utils.AudioUtils
import javafx.animation.AnimationTimer
import javafx.application.Platform

/** Audio playback controller for synth. */
class AudioPlayer(viewModel: SynthViewModel):
  // Types
  private case class RenderParams(
      model: SynthFile,
      loopCount: Int,
      toneFilter: Int
  )

  // Fields
  private var currentClip: Option[Clip] = None
  private var playheadTimer: Option[AnimationTimer] = None
  private var currentParams: Option[RenderParams] = None
  private val generation = new AtomicLong(0)

  /** Callback for playhead position updates (`0.0` to `1.0`, or `-1` when
    * stopped).
    */
  var onPlayheadUpdate: Double => Unit = _ => ()

  /** Starts playback with current settings. */
  def play(): Unit =
    val toneFilter =
      if viewModel.isTargetAll then -1 else viewModel.getActiveToneIndex
    val loopCount =
      if viewModel.isLoopEnabled then viewModel.loopCountProperty.get else 1
    val modelSnapshot = viewModel.toModel()
    val newParams = RenderParams(modelSnapshot, loopCount, toneFilter)

    if trySmartReplay(newParams) then return
    startAsyncSynthesis(modelSnapshot, loopCount, toneFilter, newParams)

  /** Stops current playback. */
  def stop(): Unit =
    stopInternal(resetGen = true)

  private def trySmartReplay(params: RenderParams): Boolean =
    if currentParams.contains(params) && currentClip.exists(_.isOpen) then
      val clip = currentClip.get
      playheadTimer.foreach(_.stop())
      clip.stop()
      clip.setFramePosition(0)
      clip.start()
      startTimer(clip)
      true
    else false

  private def startAsyncSynthesis(
      model: SynthFile,
      loopCount: Int,
      toneFilter: Int,
      params: RenderParams
  ): Unit =
    stopInternal(resetGen = false)
    val currentGen = generation.incrementAndGet()

    new Thread(() => {
      try
        val audio = TrackSynthesizer.synthesize(model, loopCount, toneFilter)
        val clip = AudioSystem.getClip()
        val format = new AudioFormat(constants.SampleRate, 16, 1, true, true)
        val bytes = audio.toBytes16BE
        clip.open(format, bytes, 0, bytes.length)

        Platform.runLater(() => {
          if generation.get() == currentGen then
            currentParams = Some(params)
            startPlayback(clip)
          else clip.close()
        })
      catch case e: Exception => e.printStackTrace()
    }).start()

  private def startPlayback(clip: Clip): Unit =
    try
      currentClip = Some(clip)
      if viewModel.isLoopEnabled && configureLoopPoints(clip) then
        val count = viewModel.loopCountProperty.get
        clip.loop(if count == 0 then Clip.LOOP_CONTINUOUSLY else count - 1)
      else clip.start()
      startTimer(clip)
    catch case e: Exception => e.printStackTrace()

  private def startTimer(clip: Clip): Unit =
    val totalFrames = clip.getFrameLength.toDouble
    val timer = new AnimationTimer:
      def handle(now: Long): Unit =
        if clip.isRunning then
          val pos = clip.getFramePosition.toDouble / totalFrames
          onPlayheadUpdate(pos)
        else
          onPlayheadUpdate(-1)
          this.stop()
    timer.start()
    playheadTimer = Some(timer)

  private def configureLoopPoints(clip: Clip): Boolean =
    val startMs = math.max(0, viewModel.loopStartProperty.get)
    val endMs = math.max(startMs, viewModel.loopEndProperty.get)
    if endMs <= startMs then return false

    val startFrames = AudioUtils.msToSamples(startMs).value
    val endFrames = AudioUtils.msToSamples(endMs).value
    val len = clip.getFrameLength

    val validEnd = math.min(endFrames, len - 1).toInt
    val validStart = math.min(startFrames, validEnd).toInt
    if validEnd > validStart then
      clip.setLoopPoints(validStart, validEnd)
      true
    else false

  private def stopInternal(resetGen: Boolean): Unit =
    if resetGen then generation.incrementAndGet()

    playheadTimer.foreach(_.stop())
    playheadTimer = None
    onPlayheadUpdate(-1)

    currentClip.foreach { clip =>
      if clip.isRunning then clip.stop()
      clip.close()
    }
    currentClip = None
    currentParams = None
