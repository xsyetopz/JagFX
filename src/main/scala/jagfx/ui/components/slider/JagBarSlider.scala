package jagfx.ui.components.slider

import javafx.beans.property._
import javafx.scene.layout._
import javafx.scene.shape.Rectangle
import javafx.geometry.Pos
import javafx.scene.control._
import jagfx.ui.components.field.JagNumericField

/** Horizontal bar slider with numeric input. */
class JagBarSlider(min: Int, max: Int, initial: Int, labelText: String = "")
    extends JagBaseSlider(min, max, initial):

  getStyleClass.add("jag-bar-slider")
  setSpacing(2)

  private val _inputRow = HBox()
  _inputRow.setSpacing(4)
  _inputRow.setAlignment(Pos.CENTER_LEFT)

  if labelText.nonEmpty then
    val lbl = new Label(labelText)
    lbl.getStyleClass.add("label")
    lbl.setStyle(
      "-fx-text-fill: #888; -fx-font-size: 9px; -fx-font-weight: bold;"
    )

    val spacer = new Region()
    HBox.setHgrow(spacer, Priority.ALWAYS)
    _inputRow.getChildren.addAll(lbl, spacer)

  private val _input = JagNumericField(min, max, initial)
  _input.setTooltip(new Tooltip(labelText match
    case "VOL:" => "Reverb mix level (0-100%)"
    case "DEL:" => "Reverb delay in samples"
    case other  => other))
  value.bindBidirectional(_input.valueProperty)
  _inputRow.getChildren.add(_input)

  private val _barBox = VBox()
  _barBox.getStyleClass.add("bar-box")
  _barBox.setPrefHeight(4)
  _barBox.setMaxHeight(4)

  private val _barFill = Region()
  _barFill.getStyleClass.add("bar-fill")
  _barFill.setPrefHeight(4)
  _barFill.setMaxHeight(4)

  _barBox.widthProperty.addListener((_, _, newWidth) =>
    val range = max - min
    val ratio = if range > 0 then (value.get - min).toDouble / range else 0
    _barFill.setPrefWidth(newWidth.doubleValue * ratio)
  )

  value.addListener((_, _, newVal) =>
    val range = max - min
    val ratio =
      if range > 0 then (newVal.intValue - min).toDouble / range else 0
    _barFill.setPrefWidth(_barBox.getWidth * ratio)
  )

  getChildren.addAll(_inputRow, _barBox)
  _barBox.getChildren.add(_barFill)

object JagBarSlider:
  def apply(
      min: Int,
      max: Int,
      initial: Int,
      label: String = ""
  ): JagBarSlider =
    new JagBarSlider(min, max, initial, label)
