package jagfx.ui.controller.footer

import javafx.scene.layout._
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.ui.controller.IController
import jagfx.ui.BindingManager
import javafx.scene.control.Label

/** Footer controller containing tones, harmonics, reverb, and mode panels. */
class FooterController(viewModel: SynthViewModel) extends IController[VBox]:
  private val content = HBox()
  content.getStyleClass.add("footer")

  private val reverbBindings = BindingManager()

  private val tonesPanel = TonesPanel.create(viewModel)
  private val harmonicsPanel = HarmonicsPanel.create(viewModel)
  private val reverbPanel = ReverbPanel.create(viewModel, reverbBindings)

  content.getChildren.addAll(tonesPanel, harmonicsPanel, reverbPanel)
  HBox.setHgrow(harmonicsPanel, Priority.ALWAYS)

  protected val view = VBox(content)
