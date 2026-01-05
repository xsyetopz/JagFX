package jagfx.ui.components.slider

import jagfx.ui.components.field.JagNumericField
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*

/** Horizontal bar slider with numeric input. */
class JagBarSlider(min: Int, max: Int, initial: Int, labelTxt: String = "")
    extends JagBaseSlider(min, max, initial):
  // Fields
  private val inputRow = HBox()
  private val input = JagNumericField(min, max, initial)
  private val barBox = VBox()
  private val barFill = Region()

  // Init: styling
  getStyleClass.add("jag-bar-slider")
  setSpacing(2)

  inputRow.setSpacing(4)
  inputRow.setAlignment(Pos.CENTER_LEFT)

  if labelTxt.nonEmpty then
    val lbl = new Label(labelTxt)
    lbl.getStyleClass.add("label")
    lbl.setStyle(
      "-fx-text-fill: #888; -fx-font-size: 9px; -fx-font-weight: bold;"
    )
    val spacer = new Region()
    HBox.setHgrow(spacer, Priority.ALWAYS)
    inputRow.getChildren.addAll(lbl, spacer)

  input.setTooltip(new Tooltip(labelTxt match
    case "VOL:" => "Echo mix level (0-100%)"
    case "DEL:" => "Echo delay in samples"
    case other  => other))

  barBox.getStyleClass.add("bar-box")
  barBox.setPrefHeight(4)
  barBox.setMaxHeight(4)

  barFill.getStyleClass.add("bar-fill")
  barFill.setPrefHeight(4)
  barFill.setMaxHeight(4)

  // Init: bindings
  value.bindBidirectional(input.valueProperty)

  // Init: listeners
  barBox.widthProperty.addListener((_, _, newWidth) =>
    val range = max - min
    val ratio = if range > 0 then (value.get - min).toDouble / range else 0
    barFill.setPrefWidth(newWidth.doubleValue * ratio)
  )

  value.addListener((_, _, newVal) =>
    val range = max - min
    val ratio =
      if range > 0 then (newVal.intValue - min).toDouble / range else 0
    barFill.setPrefWidth(barBox.getWidth * ratio)
  )

  // Init: build hierarchy
  inputRow.getChildren.add(input)
  barBox.getChildren.add(barFill)
  getChildren.addAll(inputRow, barBox)

object JagBarSlider:
  /** Creates bar slider with optional label. */
  def apply(
      min: Int,
      max: Int,
      initial: Int,
      label: String = ""
  ): JagBarSlider =
    new JagBarSlider(min, max, initial, label)
