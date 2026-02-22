package jagfx.ui.viewmodel

import jagfx.model.Partial
import jagfx.types.*
import javafx.beans.property.*

/** View model for `Partial` data. */
class PartialViewModel extends ViewModelLike:
  // Fields

  /** Whether this partial is enabled. */
  val active = new SimpleBooleanProperty(false)

  /** Pitch offset in decicents. */
  val pitchOffset = new SimpleIntegerProperty(0)

  /** Volume level (`0-100`). */
  val volume = new SimpleIntegerProperty(0)

  /** Start delay in milliseconds. */
  val startDelay = new SimpleIntegerProperty(0)

  /** Loads partial data from model. */
  def load(height: Partial): Unit =
    active.set(true)
    pitchOffset.set(height.pitchOffset)
    volume.set(height.volume.value)
    startDelay.set(height.startDelay.value)
    notifyListeners()

  /** Resets all values to defaults. */
  def clear(): Unit =
    active.set(false)
    pitchOffset.set(0)
    volume.set(0)
    startDelay.set(0)
    notifyListeners()

  /** Converts view model state to model `Partial`. */
  def toModel(): Partial =
    Partial(Percent(volume.get), pitchOffset.get, Millis(startDelay.get))

  override protected def registerPropertyListeners(cb: () => Unit): Unit =
    active.addListener((_, _, _) => cb())
    pitchOffset.addListener((_, _, _) => cb())
    volume.addListener((_, _, _) => cb())
    startDelay.addListener((_, _, _) => cb())
