package jagfx.utils

import org.kordamp.ikonli.javafx.FontIcon

object IconUtils:
  /** Creates `FontIcon` from literal string description using MaterialDesign2.
    * This uses raw string code (e.g. `"mdi2-play"`).
    */
  def icon(code: String, size: Int = 16): FontIcon =
    val i = new FontIcon(code)
    i.setIconSize(size)
    i
