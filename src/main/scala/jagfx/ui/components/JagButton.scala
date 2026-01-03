package jagfx.ui.components

import javafx.beans.property._
import javafx.scene.control.Button

/** Styled button with active state property. */
class JagButton(text: String) extends Button(text):
  private val active = SimpleBooleanProperty(false)

  def activeProperty: BooleanProperty = active
  def isActive: Boolean = active.get
  def setActive(value: Boolean): Unit = active.set(value)

  getStyleClass.add("jag-btn")
  active.addListener((_, _, isActive) =>
    if isActive then getStyleClass.add("active")
    else getStyleClass.remove("active")
  )

object JagButton:
  def apply(text: String = ""): JagButton = new JagButton(text)
