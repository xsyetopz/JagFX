package jagfx.ui.components.slider

import javafx.beans.property.*
import javafx.scene.layout.VBox

/** Abstract base for sliders with value property. */
abstract class JagBaseSlider(min: Int, max: Int, initial: Int) extends VBox:
  // Fields
  protected val value: SimpleIntegerProperty = SimpleIntegerProperty(initial)

  /** Value property. */
  def valueProperty: IntegerProperty = value

  /** Returns current value. */
  def getValue: Int = value.get

  /** Sets current value with clamping. */
  def setValue(v: Int): Unit = value.set(clamp(v))

  /** Disposes of resources used by this slider. */
  def dispose(): Unit = {}

  protected def clamp(v: Int): Int = math.max(min, math.min(max, v))
