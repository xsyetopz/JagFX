package jagfx.ui.components.button

import javafx.beans.property._
import javafx.scene.control.Button

/** Styled button with active state property. */
class JagButton(text: String) extends Button(text):
  private val _active = SimpleBooleanProperty(false)

  def activeProperty: BooleanProperty = _active
  def isActive: Boolean = _active.get
  def setActive(value: Boolean): Unit = _active.set(value)

  getStyleClass.add("jag-btn")
  _active.addListener((_, _, isActive) =>
    if isActive then getStyleClass.add("active")
    else getStyleClass.remove("active")
  )

object JagButton:
  def apply(text: String = ""): JagButton = new JagButton(text)
