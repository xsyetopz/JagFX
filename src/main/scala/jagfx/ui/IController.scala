package jagfx.ui

import javafx.scene.layout.Region

/** Base trait for UI controllers. */
trait IController[V <: Region]:
  /** Root view managed by this controller. */
  protected val view: V

  /** Returns root view. */
  def getView: V = view
