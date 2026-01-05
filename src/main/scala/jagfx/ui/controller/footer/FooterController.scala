package jagfx.ui.controller.footer

import jagfx.ui.BindingManager
import jagfx.ui.controller.ControllerLike
import jagfx.ui.viewmodel.SynthViewModel
import javafx.scene.layout.*

/** Footer controller containing tones, partials, echo, and mode panels. */
class FooterController(viewModel: SynthViewModel) extends ControllerLike[VBox]:
  // Fields
  protected val view: VBox = VBox()
  private val content = HBox()
  private val echoBindings = BindingManager()
  private val tonesPanel = TonesPanel.create(viewModel)
  private val partialsPanel = PartialsPanel.create(viewModel)
  private val echoPanel = EchoPanel.create(viewModel, echoBindings)

  // Init: styling
  content.getStyleClass.add("footer")

  // Init: build hierarchy
  content.getChildren.addAll(tonesPanel, partialsPanel, echoPanel)
  HBox.setHgrow(partialsPanel, Priority.ALWAYS)
  view.getChildren.add(content)
