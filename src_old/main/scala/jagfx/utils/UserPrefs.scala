package jagfx.utils

import java.util.prefs.Preferences

import javafx.beans.property.*

// Constants
private final val KeyExport16Bit = "export_16_bit"

/** Handles persistent user preferences using `java.util.prefs`. */
object UserPrefs:
  // Fields
  private val prefs = Preferences.userNodeForPackage(getClass)

  /** Export bit depth preference (`false`=8-bit, `true`=16-bit). */
  val export16Bit: BooleanProperty =
    new SimpleBooleanProperty(prefs.getBoolean(KeyExport16Bit, false))

  // Init: listeners
  export16Bit.addListener((_, _, newVal) =>
    prefs.putBoolean(KeyExport16Bit, newVal)
  )
