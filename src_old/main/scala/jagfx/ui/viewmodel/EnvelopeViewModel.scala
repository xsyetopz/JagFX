package jagfx.ui.viewmodel

import jagfx.model.*
import javafx.beans.property.*

/** `View model for `Envelope` data. */
class EnvelopeViewModel extends ViewModelLike:
  // Fields

  /** Oscillator waveform type. */
  val waveform = new SimpleObjectProperty[Waveform](Waveform.Square)

  /** Envelope start value. */
  val start = new SimpleIntegerProperty(0)

  /** Envelope end value. */
  val end = new SimpleIntegerProperty(0)

  /** Number of notes (pitch offset). */
  val notes = new SimpleIntegerProperty(0)

  private var segments: Vector[EnvelopeSegment] = Vector.empty

  /** Returns segment peak values only. */
  def getSegments: Vector[Int] = segments.map(_.peak)

  /** Returns full segment data including duration and peak. */
  def getFullSegments: Vector[EnvelopeSegment] = segments

  /** Appends new segment with given duration and peak. */
  def addSegment(duration: Int, peak: Int): Unit =
    segments = segments :+ EnvelopeSegment(duration, peak)
    notifyListeners()

  /** Removes segment at given index. */
  def removeSegment(index: Int): Unit =
    if index >= 0 && index < segments.length then
      segments = segments.patch(index, Nil, 1)
      notifyListeners()

  /** Updates segment at given index with new duration and peak. */
  def updateSegment(index: Int, duration: Int, peak: Int): Unit =
    if index >= 0 && index < segments.length then
      segments = segments.updated(index, EnvelopeSegment(duration, peak))
      notifyListeners()

  /** Batch updates multiple segments. */
  def updateSegments(updates: Seq[(Int, EnvelopeSegment)]): Unit =
    var changed = false
    var newSegments = segments
    updates.foreach { case (index, newSeg) =>
      if index >= 0 && index < newSegments.length then
        newSegments = newSegments.updated(index, newSeg)
        changed = true
    }
    if changed then
      segments = newSegments
      notifyListeners()

  /** Returns `true` if envelope has no segments and waveform is `Off`. */
  def isEmpty: Boolean = segments.isEmpty && waveform.get == Waveform.Off

  /** Returns `true` if all values are zero (no audible effect). */
  def isZero: Boolean =
    start.get == 0 && end.get == 0 && segments.forall(_.peak == 0)

  /** Loads data from model into this view model. */
  def load(env: Envelope): Unit =
    waveform.set(env.waveform)
    start.set(env.start)
    end.set(env.end)
    segments = env.segments
    notifyListeners()

  /** Resets all values to defaults. */
  def clear(): Unit =
    waveform.set(Waveform.Off)
    start.set(0)
    end.set(0)
    segments = Vector.empty
    notifyListeners()

  /** Converts view model state to model `Envelope`. */
  def toModel(): Envelope =
    Envelope(waveform.get, start.get, end.get, segments)

  override protected def registerPropertyListeners(cb: () => Unit): Unit =
    waveform.addListener((_, _, _) => cb())
    start.addListener((_, _, _) => cb())
    end.addListener((_, _, _) => cb())
