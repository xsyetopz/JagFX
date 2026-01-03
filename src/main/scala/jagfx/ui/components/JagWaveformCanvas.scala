package jagfx.ui.components

import jagfx.synth.AudioBuffer
import jagfx.utils.ColorUtils._
import jagfx.utils.DrawingUtils._

/** Canvas rendering synthesized audio waveform (no grid, just waveform + center
  * line).
  */
class JagWaveformCanvas extends JagBaseCanvas:
  private var audioSamples: Array[Int] = Array.empty

  getStyleClass.add("jag-waveform-canvas")
  zoomLevel = 4

  def setAudioBuffer(audio: AudioBuffer): Unit =
    audioSamples = audio.samples
    draw()

  def clearAudio(): Unit =
    audioSamples = Array.empty
    draw()

  override protected def drawContent(buffer: Array[Int], w: Int, h: Int): Unit =
    drawCenterLine(buffer, w, h)
    drawWaveform(buffer, w, h)

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

object JagWaveformCanvas:
  def apply(): JagWaveformCanvas = new JagWaveformCanvas()
