package jagfx.ui.components

import javafx.beans.property._
import javafx.scene.layout.HBox
import jagfx.utils.IconUtils

/** Mutual-exclusion button group. */
class JagToggleGroup(items: (String, String)*) extends HBox:
  private val selected = SimpleStringProperty(
    items.headOption.map(_._1).getOrElse("")
  )

  def selectedProperty: StringProperty = selected
  def getSelected: String = selected.get
  def setSelected(value: String): Unit = selected.set(value)

  getStyleClass.add("jag-toggle-group")
  setSpacing(2)

  items.foreach { case (value, iconCode) =>
    val btn = JagButton()
    btn.setGraphic(IconUtils.icon(iconCode))
    btn.activeProperty.bind(selected.isEqualTo(value))
    btn.setOnAction(_ => selected.set(value))
    getChildren.add(btn)
  }

object JagToggleGroup:
  def apply(items: (String, String)*): JagToggleGroup = new JagToggleGroup(
    items*
  )
