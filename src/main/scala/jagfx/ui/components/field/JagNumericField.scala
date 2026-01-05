package jagfx.ui.components.field

import java.util.function.UnaryOperator
import java.util.regex.Pattern

import javafx.geometry.Pos
import javafx.scene.control.TextFormatter
import javafx.util.converter.DefaultStringConverter

private val FieldSize = 40

/** Integer input field with min/max validation and scroll adjustment. */
class JagNumericField(
    min: Int,
    max: Int,
    initial: Int,
    scale: Double = 1.0,
    format: String = "%.0f"
) extends JagBaseField(initial):
  // Fields
  private val validPattern = Pattern.compile("-?(([0-9]*)|([0-9]*\\.[0-9]*))")

  private val filter: UnaryOperator[TextFormatter.Change] = change =>
    val newText = change.getControlNewText
    if validPattern.matcher(newText).matches() then
      if newText.isEmpty || newText == "-" || newText == "." || newText == "-."
      then change
      else
        try
          val parsed = newText.toDouble
          val scaled = parsed * scale
          val blockMax = max > 0 && scaled > max
          val blockMin = min < 0 && scaled < min
          if blockMax || blockMin then null else change
        catch case _: NumberFormatException => null
    else null

  private val formatter =
    new TextFormatter[String](DefaultStringConverter(), null, filter)

  // Init: styling
  getStyleClass.add("jag-input")
  setAlignment(Pos.CENTER_RIGHT)
  setPrefWidth(FieldSize)
  setMinWidth(FieldSize)
  setTextFormatter(formatter)
  setText(String.format(format, (initial / scale).asInstanceOf[Object]))

  // Init: event handlers
  setOnScroll(e =>
    if isFocused || isHover then
      val delta = if e.getDeltaY > 0 then 1 else -1
      val stepMultiplier =
        if e.isShiftDown then 10.0
        else if e.isShortcutDown then 0.01
        else 1.0

      val increment = (stepMultiplier * scale).round.toInt
      val effectiveInc =
        if increment == 0 then (if delta > 0 then 1 else -1)
        else increment * delta

      val newVal = math.max(min, math.min(max, value.get + effectiveInc))
      if newVal != value.get then
        value.set(newVal)
        val displayVal = newVal.intValue / scale
        val str = String.format(format, displayVal.asInstanceOf[Object])
        if getText != str then setText(str)
        selectAll()

      e.consume()
  )

  setOnAction(_ => if getParent != null then getParent.requestFocus())

  // Init: listeners
  textProperty.addListener((_, _, newText) =>
    if !newText.isEmpty && newText != "-" && newText != "." then
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
      val str = String.format(format, displayVal.asInstanceOf[Object])
      setText(str)
  )

object JagNumericField:
  /** Creates numeric field with default scale. */
  def apply(min: Int, max: Int, initial: Int): JagNumericField =
    new JagNumericField(min, max, initial)

  /** Creates numeric field with custom scale and format. */
  def apply(
      min: Int,
      max: Int,
      initial: Int,
      scale: Double,
      format: String
  ): JagNumericField =
    new JagNumericField(min, max, initial, scale, format)
