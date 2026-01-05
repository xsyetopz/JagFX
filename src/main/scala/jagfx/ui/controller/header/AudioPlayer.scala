package jagfx.ui.controller.header

import javax.sound.sampled._
import javafx.animation.AnimationTimer
import javafx.application.Platform
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.synth.TrackSynthesizer
import jagfx.Constants
import jagfx.utils.AudioUtils
import jagfx.synth.AudioBuffer
import java.util.concurrent.atomic.AtomicLong
import jagfx.model.SynthFile

/** Audio playback controller for synth. */
class AudioPlayer(viewModel: SynthViewModel):
  private var _currentClip: Option[Clip] = None
  private var _playheadTimer: Option[AnimationTimer] = None
  private val _generation = new AtomicLong(0)

  private case class RenderParams(
      model: SynthFile,
      loopCount: Int,
      toneFilter: Int
  )
  private var _currentParams: Option[RenderParams] = None

  /** Callback for playhead position updates (`0.0` to `1.0`, or `-1` when
    * stopped).
    */
  var onPlayheadUpdate: Double => Unit = _ => ()

  def play(): Unit =
    val toneFilter =
      if viewModel.isTargetAll then -1 else viewModel.getActiveToneIndex
    val loopCount =
      if viewModel.isLoopEnabled then viewModel.loopCountProperty.get else 1

    val modelSnapshot = viewModel.toModel()

    val newParams = RenderParams(modelSnapshot, loopCount, toneFilter)

    if _trySmartReplay(newParams) then return
    _startAsyncSynthesis(modelSnapshot, loopCount, toneFilter, newParams)

  def stop(): Unit =
    _stopInternal(resetGen = true)

  private def _trySmartReplay(params: RenderParams): Boolean =
    if _currentParams.contains(params) && _currentClip.exists(_.isOpen) then
      val clip = _currentClip.get
      _playheadTimer.foreach(_.stop())
      clip.stop()
      clip.setFramePosition(0)
      clip.start()
      _startTimer(clip)
      true
    else false

  private def _startAsyncSynthesis(
      model: SynthFile,
      loopCount: Int,
      toneFilter: Int,
      params: RenderParams
  ): Unit =
    _stopInternal(resetGen = false)
    val currentGen = _generation.incrementAndGet()

    new Thread(() => {
      try
        val audio =
          TrackSynthesizer.synthesize(model, loopCount, toneFilter)

        val clip = AudioSystem.getClip()
        val format = new AudioFormat(Constants.SampleRate, 16, 1, true, true)
        val bytes = audio.toBytes16BE
        clip.open(format, bytes, 0, bytes.length)

        Platform.runLater(() => {
          if _generation.get() == currentGen then
            _currentParams = Some(params)
            _startPlayback(clip)
          else clip.close()
        })
      catch case e: Exception => e.printStackTrace()
    }).start()

  private def _startPlayback(clip: Clip): Unit =
    try
      _currentClip = Some(clip)

      if viewModel.isLoopEnabled && _configureLoopPoints(clip) then
        val count = viewModel.loopCountProperty.get
        clip.loop(if count == 0 then Clip.LOOP_CONTINUOUSLY else count - 1)
      else clip.start()

      _startTimer(clip)
    catch
      case e: Exception =>
        e.printStackTrace()

  private def _startTimer(clip: Clip): Unit =
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
    _playheadTimer = Some(timer)

  private def _configureLoopPoints(clip: Clip): Boolean =
    val startMs = math.max(0, viewModel.loopStartProperty.get)
    val endMs = math.max(startMs, viewModel.loopEndProperty.get)
    if endMs <= startMs then return false

    val startFrames = AudioUtils.msToSamples(startMs)
    val endFrames = AudioUtils.msToSamples(endMs)
    val len = clip.getFrameLength

    val validEnd = math.min(endFrames, len - 1).toInt
    val validStart = math.min(startFrames, validEnd).toInt
    if validEnd > validStart then
      clip.setLoopPoints(validStart, validEnd)
      true
    else false

  private def _stopInternal(resetGen: Boolean): Unit =
    if resetGen then _generation.incrementAndGet()

    _playheadTimer.foreach(_.stop())
    _playheadTimer = None
    onPlayheadUpdate(-1)

    _currentClip.foreach { clip =>
      if clip.isRunning then clip.stop()
      clip.close()
    }
    _currentClip = None
    _currentParams = None
