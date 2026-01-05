package jagfx.ui.components.group

import javafx.beans.property.*
import javafx.scene.layout.HBox

/** Abstract base for toggle groups with selection state. */
abstract class JagBaseGroup(initialId: String) extends HBox:
  // Fields
  protected val selected: SimpleStringProperty = SimpleStringProperty(initialId)

  /** Selection state property. */
  def selectedProperty: StringProperty = selected

  /** Returns selected group ID. */
  def getSelected: String = selected.get

  /** Sets selected group ID. */
  def setSelected(value: String): Unit = selected.set(value)

  /** Disposes of resources used by this group. */
  def dispose(): Unit = {}
