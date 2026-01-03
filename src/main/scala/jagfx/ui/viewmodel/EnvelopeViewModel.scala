package jagfx.ui.viewmodel

import javafx.beans.property._
import jagfx.model._

/** `ViewModel` for `Envelope` data. */
class EnvelopeViewModel extends IViewModel:
  val form = new SimpleObjectProperty[WaveForm](WaveForm.Square)
  val start = new SimpleIntegerProperty(0)
  val end = new SimpleIntegerProperty(0)
  val notes = new SimpleIntegerProperty(0)

  private var segments: Vector[EnvelopeSegment] = Vector.empty

  def getSegments: Vector[Int] = segments.map(_.peak)
  def getFullSegments: Vector[EnvelopeSegment] = segments
  def isEmpty: Boolean = segments.isEmpty && form.get == WaveForm.Off

  def load(env: Envelope): Unit =
    form.set(env.form)
    start.set(env.start)
    end.set(env.end)
    segments = env.segments
    notifyListeners()

  def clear(): Unit =
    form.set(WaveForm.Off)
    start.set(0)
    end.set(0)
    segments = Vector.empty
    notifyListeners()

  def toModel(): Envelope =
    Envelope(form.get, start.get, end.get, segments)

  override protected def registerPropertyListeners(cb: () => Unit): Unit =
    form.addListener((_, _, _) => cb())
    start.addListener((_, _, _) => cb())
    end.addListener((_, _, _) => cb())
