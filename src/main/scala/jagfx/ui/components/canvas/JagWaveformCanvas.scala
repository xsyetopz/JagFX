package jagfx.ui.components.canvas

import jagfx.synth.AudioBuffer
import jagfx.utils.ColorUtils._
import jagfx.utils.DrawingUtils._
import jagfx.constants.Int16

/** Canvas rendering synthesized audio waveform with playhead.
  */
class JagWaveformCanvas extends JagBaseCanvas:
  private var _audioSamples: Array[Int] = Array.empty

  private var _playheadPosition: Double =
    -1.0 // `-1` = hidden, `0..1` = unitized pos

  getStyleClass.add("jag-waveform-canvas")
  zoomLevel = 4

  def setAudioBuffer(audio: AudioBuffer): Unit =
    _audioSamples = audio.samples
    requestRedraw()

  def clearAudio(): Unit =
    _audioSamples = Array.empty
    _playheadPosition = -1.0
    requestRedraw()

  /** Set playhead position (`0.0` = start, `1.0` = end, `-1.0` = hidden). */
  def setPlayheadPosition(position: Double): Unit =
    _playheadPosition = position
    javafx.application.Platform.runLater(() => requestRedraw())

  /** Hide playhead. */
  def hidePlayhead(): Unit =
    _playheadPosition = -1.0
    javafx.application.Platform.runLater(() => requestRedraw())

  override protected def drawContent(buffer: Array[Int], w: Int, h: Int): Unit =
    drawCenterLine(buffer, w, h)
    _drawWaveform(buffer, w, h)
    _drawPlayhead(buffer, w, h)

  private def _drawWaveform(buffer: Array[Int], w: Int, h: Int): Unit =
    if _audioSamples.isEmpty then return

    val midY = h / 2
    val zoomedWidth = w * zoomLevel

    var prevX = 0
    var prevY = midY

    for x <- 0 until w do
      val sampleIdx = ((x + panOffset) * _audioSamples.length) / zoomedWidth
      if sampleIdx < _audioSamples.length then
        val sample = _audioSamples(sampleIdx)
        val normalized = sample.toDouble / Int16.UnsignedMaxValue
        val y = midY - (normalized * (h / 2)).toInt

        if x > 0 then line(buffer, w, h, prevX, prevY, x, y, Output)

        prevX = x
        prevY = math.max(0, math.min(h - 1, y))

  private def _drawPlayhead(buffer: Array[Int], w: Int, h: Int): Unit =
    if _playheadPosition < 0 then return

    val zoomedWidth = w * zoomLevel
    val absoluteX = (_playheadPosition * zoomedWidth).toInt
    val visibleX = absoluteX - panOffset

    if visibleX >= 0 && visibleX < w then
      line(buffer, w, h, visibleX, 0, visibleX, h, White)

object JagWaveformCanvas:
  def apply(): JagWaveformCanvas = new JagWaveformCanvas()
