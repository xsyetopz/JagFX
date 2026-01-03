package jagfx.ui.components

import jagfx.synth.AudioBuffer
import jagfx.utils.ColorUtils._
import jagfx.utils.DrawingUtils._

/** Canvas rendering synthesized audio waveform with playhead.
  */
class JagWaveformCanvas extends JagBaseCanvas:
  private var audioSamples: Array[Int] = Array.empty

  private var playheadPosition: Double =
    -1.0 // `-1` = hidden, `0..1` = unitized pos

  getStyleClass.add("jag-waveform-canvas")
  zoomLevel = 4

  def setAudioBuffer(audio: AudioBuffer): Unit =
    audioSamples = audio.samples
    requestRedraw()

  def clearAudio(): Unit =
    audioSamples = Array.empty
    playheadPosition = -1.0
    requestRedraw()

  /** Set playhead position (`0.0` = start, `1.0` = end, `-1.0` = hidden). */
  def setPlayheadPosition(position: Double): Unit =
    playheadPosition = position
    javafx.application.Platform.runLater(() => requestRedraw())

  /** Hide playhead. */
  def hidePlayhead(): Unit =
    playheadPosition = -1.0
    javafx.application.Platform.runLater(() => requestRedraw())

  override protected def drawContent(buffer: Array[Int], w: Int, h: Int): Unit =
    drawCenterLine(buffer, w, h)
    drawWaveform(buffer, w, h)
    drawPlayhead(buffer, w, h)

  private def drawWaveform(buffer: Array[Int], w: Int, h: Int): Unit =
    if audioSamples.isEmpty then return

    val midY = h / 2
    val zoomedWidth = w * zoomLevel

    var prevX = 0
    var prevY = midY

    for x <- 0 until w do
      val sampleIdx = ((x + panOffset) * audioSamples.length) / zoomedWidth
      if sampleIdx < audioSamples.length then
        val sample = audioSamples(sampleIdx)
        val normalized = sample.toDouble / 32768.0
        val y = midY - (normalized * (h / 2)).toInt

        if x > 0 then line(buffer, w, h, prevX, prevY, x, y, Graph)

        prevX = x
        prevY = math.max(0, math.min(h - 1, y))

  private def drawPlayhead(buffer: Array[Int], w: Int, h: Int): Unit =
    if playheadPosition < 0 then return

    val zoomedWidth = w * zoomLevel
    val absoluteX = (playheadPosition * zoomedWidth).toInt
    val visibleX = absoluteX - panOffset

    if visibleX >= 0 && visibleX < w then
      line(buffer, w, h, visibleX, 0, visibleX, h, White)

object JagWaveformCanvas:
  def apply(): JagWaveformCanvas = new JagWaveformCanvas()
