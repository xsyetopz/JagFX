package jagfx.ui.components.canvas

import java.nio.IntBuffer

import jagfx.Constants.MinFrameNanos
import jagfx.utils.ColorUtils.*
import jagfx.utils.DrawingUtils.*
import javafx.animation.AnimationTimer
import javafx.scene.canvas.Canvas
import javafx.scene.image.*
import javafx.scene.input.*

/** Base canvas with throttled rendering and buffer management. */
abstract class JagBaseCanvas extends Canvas:
  protected var image: WritableImage = null
  protected var buffer: Array[Int] = Array.empty
  protected val pixelFormat: PixelFormat[IntBuffer] =
    PixelFormat.getIntArgbInstance
  protected var zoomLevel: Int = 1
  protected var panOffset: Int = 0

  @volatile private var dirty = false
  @volatile private var resizePending = false

  private var dragStartX: Double = 0
  private var dragStartPan: Int = 0

  setWidth(200)
  setHeight(100)

  private val redrawTimer = new AnimationTimer:
    private var lastFrame = 0L

    /** Handles animation frame. */
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

  /** Returns current pan offset in pixels. */
  def getPanOffset: Int = panOffset

  /** Requests redraw on next animation frame. */
  def requestRedraw(): Unit = dirty = true

  private def performDraw(): Unit =
    val width = getWidth.toInt
    val height = getHeight.toInt
    if width <= 0 || height <= 0 then return
    if buffer.length != width * height then resizeBuffer(width, height)
    if buffer.isEmpty then return

    clear(buffer, BgBlack)
    drawContent(buffer, width, height)

    val pw = image.getPixelWriter
    pw.setPixels(0, 0, width, height, pixelFormat, buffer, 0, width)

    val gc = getGraphicsContext2D
    gc.drawImage(image, 0, 0)

  protected def resizeBuffer(width: Int, height: Int): Unit =
    if width > 0 && height > 0 then
      image = new WritableImage(width, height)
      buffer = new Array[Int](width * height)

  /** Draw specific content for this canvas type. */
  protected def drawContent(buffer: Array[Int], width: Int, height: Int): Unit

  /** Draw center line (zero crossing / midpoint). */
  protected def drawCenterLine(
      buffer: Array[Int],
      width: Int,
      height: Int
  ): Unit =
    val midY = height / 2
    line(buffer, width, height, 0, midY, width, midY, White)

  /** Max pan offset based on zoom level and width. */
  protected def maxPanOffset: Int =
    val width = getWidth.toInt
    math.max(0, (width * zoomLevel) - width)

  /** Update pan offset, clamping to valid range. */
  def setPan(offset: Int): Unit =
    val clamped = math.max(0, math.min(maxPanOffset, offset))
    if panOffset != clamped then
      panOffset = clamped
      requestRedraw()

  setOnScroll { (e: ScrollEvent) =>
    if zoomLevel > 1 then
      val delta = if e.getDeltaY > 0 then -20 else 20
      setPan(panOffset + delta)
      e.consume()
  }

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
