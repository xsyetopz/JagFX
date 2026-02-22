package jagfx.ui.components.group

import jagfx.ui.components.button.JagButton
import jagfx.utils.IconUtils

/** Mutual-exclusion button group with icon or text buttons. */
class JagToggleGroup(items: (String, String)*)
    extends JagBaseGroup(items.headOption.map(_._1).getOrElse("")):

  // Init: styling
  getStyleClass.add("jag-toggle-group")
  setSpacing(2)

  // Init: build buttons
  items.foreach { case (value, iconCode) =>
    val btn = JagButton()
    if iconCode.nonEmpty then btn.setGraphic(IconUtils.icon(iconCode))
    else btn.setText(value)

    btn.activeProperty.bind(selected.isEqualTo(value))
    btn.setOnAction(_ => selected.set(value))
    getChildren.add(btn)
  }

object JagToggleGroup:
  /** Creates toggle group from value-icon pairs. */
  def apply(items: (String, String)*): JagToggleGroup = new JagToggleGroup(
    items*
  )
