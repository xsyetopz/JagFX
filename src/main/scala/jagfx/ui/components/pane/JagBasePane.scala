package jagfx.ui.components.pane

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.*

/** Abstract base for pane components with header. */
abstract class JagBasePane(title: String) extends VBox:
  // Fields
  protected val header = new Label(title)

  // Init: styling
  header.getStyleClass.add("panel-head")
  header.setMaxWidth(Double.MaxValue)
  header.setAlignment(Pos.CENTER)

  getChildren.add(header)

  /** Disposes of resources used by this pane. */
  def dispose(): Unit = {}
