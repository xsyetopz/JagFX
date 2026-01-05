package jagfx.ui.controller.footer

import javafx.scene.layout._
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.ui.controller.IController
import jagfx.ui.BindingManager
import javafx.scene.control.Label

/** Footer controller containing tones, harmonics, reverb, and mode panels. */
class FooterController(viewModel: SynthViewModel) extends IController[VBox]:
  private val _content = HBox()
  _content.getStyleClass.add("footer")

  private val _reverbBindings = BindingManager()

  private val _tonesPanel = TonesPanel.create(viewModel)
  private val _harmonicsPanel = HarmonicsPanel.create(viewModel)
  private val _reverbPanel = ReverbPanel.create(viewModel, _reverbBindings)

  _content.getChildren.addAll(_tonesPanel, _harmonicsPanel, _reverbPanel)
  HBox.setHgrow(_harmonicsPanel, Priority.ALWAYS)

  protected val view = VBox(_content)
