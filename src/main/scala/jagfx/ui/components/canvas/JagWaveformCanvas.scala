package jagfx.ui.components.canvas

import jagfx.constants.Int16
import jagfx.synth.AudioBuffer
import jagfx.utils.ColorUtils.*
import jagfx.utils.DrawingUtils.*

/** Canvas rendering synthesized audio waveform with playhead. */
class JagWaveformCanvas extends JagBaseCanvas:
  // Fields
  private var audioSamples: Array[Int] = Array.empty
  private var playheadPosition: Double = -1.0

  // Init: styling
  getStyleClass.add("jag-waveform-canvas")
  zoomLevel = 4

  /** Sets audio buffer for display. */
  def setAudioBuffer(audio: AudioBuffer): Unit =
    audioSamples = audio.samples
    requestRedraw()

  /** Clears audio display. */
  def clearAudio(): Unit =
    audioSamples = Array.empty
    playheadPosition = -1.0
    requestRedraw()

  /** Sets playhead position (`0.0` = start, `1.0` = end, `-1.0` = hidden). */
  def setPlayheadPosition(position: Double): Unit =
    playheadPosition = position
    javafx.application.Platform.runLater(() => requestRedraw())

  /** Hides playhead. */
  def hidePlayhead(): Unit =
    playheadPosition = -1.0
    javafx.application.Platform.runLater(() => requestRedraw())

  override protected def drawContent(
      buffer: Array[Int],
      width: Int,
      height: Int
  ): Unit =
    drawCenterLine(buffer, width, height)
    drawWaveform(buffer, width, height)
    drawPlayhead(buffer, width, height)

  private def drawWaveform(buffer: Array[Int], width: Int, height: Int): Unit =
    if audioSamples.isEmpty then return

    val midY = height / 2
    val zoomedWidth = width * zoomLevel

    var prevX = 0
    var prevY = midY

    for x <- 0 until width do
      val sampleIdx = ((x + panOffset) * audioSamples.length) / zoomedWidth
      if sampleIdx < audioSamples.length then
        val sample = audioSamples(sampleIdx)
        val normalized = sample.toDouble / Int16.UnsignedMaxValue
        val y = midY - (normalized * (height / 2)).toInt

        if x > 0 then line(buffer, width, height, prevX, prevY, x, y, Output)

        prevX = x
        prevY = math.max(0, math.min(height - 1, y))

  private def drawPlayhead(buffer: Array[Int], width: Int, height: Int): Unit =
    if playheadPosition < 0 then return

    val zoomedWidth = width * zoomLevel
    val absoluteX = (playheadPosition * zoomedWidth).toInt
    val visibleX = absoluteX - panOffset

    if visibleX >= 0 && visibleX < width then
      line(buffer, width, height, visibleX, 0, visibleX, height, White)

object JagWaveformCanvas:
  /** Creates waveform canvas. */
  def apply(): JagWaveformCanvas = new JagWaveformCanvas()
