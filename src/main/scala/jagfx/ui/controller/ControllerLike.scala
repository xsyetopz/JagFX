package jagfx.ui.controller

import javafx.scene.layout.Region

/** Base trait for UI controllers. */
trait ControllerLike[V <: Region]:
  // Fields

  /** Root view managed by this controller. */
  protected val view: V

  /** Returns root view. */
  def getView: V = view
