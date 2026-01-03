package jagfx.ui.components

import javafx.scene.canvas.Canvas
import javafx.scene.image._
import javafx.scene.input._
import javafx.animation.AnimationTimer
import jagfx.utils.ColorUtils._
import jagfx.utils.DrawingUtils._

private val MinFrameNanos = 16_666_666L

/** Base canvas with throttled rendering and buffer management. */
abstract class JagBaseCanvas extends Canvas:
  protected var image: WritableImage = null
  protected var buffer: Array[Int] = Array.empty
  protected val pixelFormat: PixelFormat[java.nio.IntBuffer] =
    PixelFormat.getIntArgbInstance
  protected var zoomLevel: Int = 1
  protected var panOffset: Int = 0

  @volatile private var dirty = false
  @volatile private var resizePending = false

  setWidth(200)
  setHeight(100)

  private val redrawTimer = new AnimationTimer:
    private var lastFrame = 0L
    def handle(now: Long): Unit =
      if dirty && now - lastFrame >= MinFrameNanos then
        dirty = false
        lastFrame = now
        performDraw()

  redrawTimer.start()

  /** Sets zoom level and resets pan. */
  def setZoom(level: Int): Unit =
    zoomLevel = level
    panOffset = 0
    requestRedraw()

  def getPanOffset: Int = panOffset

  /** Requests redraw on next animation frame. */
  def requestRedraw(): Unit = dirty = true

  private def performDraw(): Unit =
    val w = getWidth.toInt
    val h = getHeight.toInt
    if w <= 0 || h <= 0 then return
    if buffer.length != w * h then resizeBuffer(w, h)
    if buffer.isEmpty then return

    clear(buffer, BgBlack)
    drawContent(buffer, w, h)

    val pw = image.getPixelWriter
    pw.setPixels(0, 0, w, h, pixelFormat, buffer, 0, w)

    val gc = getGraphicsContext2D
    gc.drawImage(image, 0, 0)

  protected def resizeBuffer(w: Int, h: Int): Unit =
    if w > 0 && h > 0 then
      image = new WritableImage(w, h)
      buffer = new Array[Int](w * h)

  /** Draw specific content for this canvas type. */
  protected def drawContent(buffer: Array[Int], w: Int, h: Int): Unit

  /** Draw center line (zero crossing / midpoint). */
  protected def drawCenterLine(buffer: Array[Int], w: Int, h: Int): Unit =
    val midY = h / 2
    line(buffer, w, h, 0, midY, w, midY, White)

  /** Max pan offset based on zoom level and width. */
  protected def maxPanOffset: Int =
    val w = getWidth.toInt
    math.max(0, (w * zoomLevel) - w)

  /** Update pan offset, clamping to valid range. */
  protected def setPan(offset: Int): Unit =
    val clamped = math.max(0, math.min(maxPanOffset, offset))
    if panOffset != clamped then
      panOffset = clamped
      requestRedraw()

  setOnScroll { (e: ScrollEvent) =>
    if zoomLevel > 1 then
      val delta = if e.getDeltaY > 0 then 20 else -20
      setPan(panOffset + delta)
      e.consume()
  }

  private var dragStartX: Double = 0
  private var dragStartPan: Int = 0

  setOnMousePressed { (e: MouseEvent) =>
    if zoomLevel > 1 then
      dragStartX = e.getX
      dragStartPan = panOffset
  }

  setOnMouseDragged { (e: MouseEvent) =>
    if zoomLevel > 1 then
      val delta = (dragStartX - e.getX).toInt
      setPan(dragStartPan + delta)
  }

  private def scheduleResize(): Unit =
    if !resizePending then
      resizePending = true
      javafx.application.Platform.runLater { () =>
        resizePending = false
        resizeBuffer(getWidth.toInt, getHeight.toInt)
        requestRedraw()
      }

  widthProperty.addListener((_, _, _) => scheduleResize())
  heightProperty.addListener((_, _, _) => scheduleResize())
