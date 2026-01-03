package jagfx.utils

import java.util.prefs.Preferences
import javafx.beans.property.{BooleanProperty, SimpleBooleanProperty}

private val KeyExport16Bit = "export_16_bit"

/** Handles persistent user preferences using `java.util.prefs`. */
object UserPreferences:
  private val prefs = Preferences.userNodeForPackage(getClass)

  val export16Bit: BooleanProperty =
    new SimpleBooleanProperty(prefs.getBoolean(KeyExport16Bit, false))
  export16Bit.addListener((_, _, newVal) =>
    prefs.putBoolean(KeyExport16Bit, newVal)
  )
