package jagfx.ui.components.field

import javafx.beans.property.*
import javafx.scene.control.TextField

/** Abstract base for numeric fields. */
abstract class JagBaseField(initial: Int) extends TextField:
  // Fields
  protected val value: SimpleIntegerProperty = SimpleIntegerProperty(initial)

  /** Value property. */
  def valueProperty: IntegerProperty = value

  /** Returns current value. */
  def getValue: Int = value.get

  /** Sets current value. */
  def setValue(v: Int): Unit = value.set(v)

  /** Disposes of resources used by this field. */
  def dispose(): Unit = {}
