package jagfx.ui.components.group

import javafx.beans.property._
import javafx.scene.layout.HBox
import jagfx.utils.IconUtils
import jagfx.ui.components.button.JagButton

/** Mutual-exclusion button group. */
class JagToggleGroup(items: (String, String)*)
    extends JagBaseGroup(items.headOption.map(_._1).getOrElse("")):

  getStyleClass.add("jag-toggle-group")
  setSpacing(2)

  items.foreach { case (value, iconCode) =>
    val btn = JagButton()
    if iconCode.nonEmpty then btn.setGraphic(IconUtils.icon(iconCode))
    else btn.setText(value)

    btn.activeProperty.bind(selected.isEqualTo(value))
    btn.setOnAction(_ => selected.set(value))
    getChildren.add(btn)
  }

object JagToggleGroup:
  def apply(items: (String, String)*): JagToggleGroup = new JagToggleGroup(
    items*
  )
