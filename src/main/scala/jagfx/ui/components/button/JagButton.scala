package jagfx.ui.components.button

import javafx.beans.property.*
import javafx.scene.control.Button

/** Styled button with active state property. */
class JagButton(text: String) extends Button(text):
  // Fields
  private val active = SimpleBooleanProperty(false)

  // Init: styling
  getStyleClass.add("jag-btn")

  // Init: listeners
  active.addListener((_, _, isActive) =>
    if isActive then getStyleClass.add("active")
    else getStyleClass.remove("active")
  )

  /** Active state property. */
  def activeProperty: BooleanProperty = active

  /** Returns `true` if button is active. */
  def isActive: Boolean = active.get

  /** Sets active state. */
  def setActive(value: Boolean): Unit = active.set(value)

object JagButton:
  /** Creates button with optional text. */
  def apply(text: String = ""): JagButton = new JagButton(text)
