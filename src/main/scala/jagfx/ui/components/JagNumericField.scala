package jagfx.ui.components

import javafx.beans.property._
import javafx.scene.control.TextField
import javafx.geometry.Pos

/** Integer input field with min/max validation. */
class JagNumericField(
    min: Int,
    max: Int,
    initial: Int,
    scale: Double = 1.0,
    format: String = "%.0f"
) extends TextField:
  private val value = SimpleIntegerProperty(initial)

  def valueProperty: IntegerProperty = value
  def getValue: Int = value.get
  def setValue(v: Int): Unit = value.set(math.max(min, math.min(max, v)))

  getStyleClass.add("jag-input")
  setAlignment(Pos.CENTER_RIGHT)
  setText(String.format(format, (initial / scale).asInstanceOf[Object]))
  setPrefWidth(40)
  setMinWidth(40)

  textProperty.addListener((_, _, newText) =>
    try
      val parsed = newText.toDouble
      val scaled = (parsed * scale).round.toInt
      val clamped = math.max(min, math.min(max, scaled))
      if value.get != clamped then value.set(clamped)
    catch case _: NumberFormatException => ()
  )

  value.addListener((_, _, newVal) =>
    if !isFocused then
      val displayVal = newVal.intValue / scale
      val str = String.format(format, displayVal.asInstanceOf[Object])
      if getText != str then setText(str)
  )

  focusedProperty.addListener((_, _, focused) =>
    if !focused then
      val displayVal = value.get / scale
      setText(String.format(format, displayVal.asInstanceOf[Object]))
  )

object JagNumericField:
  def apply(min: Int, max: Int, initial: Int): JagNumericField =
    new JagNumericField(min, max, initial)

  def apply(
      min: Int,
      max: Int,
      initial: Int,
      scale: Double,
      format: String
  ): JagNumericField =
    new JagNumericField(min, max, initial, scale, format)
