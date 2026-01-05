package jagfx.utils

import java.util.prefs.Preferences
import javafx.beans.property._

private val _KeyExport16Bit = "export_16_bit"

/** Handles persistent user preferences using `java.util.prefs`. */
object UserPrefs:
  private val _prefs = Preferences.userNodeForPackage(getClass)

  val export16Bit: BooleanProperty =
    new SimpleBooleanProperty(_prefs.getBoolean(_KeyExport16Bit, false))
  export16Bit.addListener((_, _, newVal) =>
    _prefs.putBoolean(_KeyExport16Bit, newVal)
  )
