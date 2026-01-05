package jagfx.ui.controller

import javafx.application.Platform
import javafx.scene.layout.BorderPane
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.ui.components.canvas._
import jagfx.ui.components.pane._
import jagfx.ui.components.slider._
import jagfx.ui.components.field._
import jagfx.ui.components.button._
import jagfx.ui.components.group._
import jagfx.ui.controller.header.HeaderController
import jagfx.ui.controller.footer.FooterController
import jagfx.ui.controller.inspector.InspectorController
import jagfx.ui.controller.rack.RackController

/** Root controller wiring all UI sections. */
object MainController:
  private val _viewModel = new SynthViewModel()

  def createRoot(): BorderPane =
    val root = BorderPane()
    root.getStyleClass.add("root")

    val header = new HeaderController(_viewModel)
    val inspector = new InspectorController(_viewModel)
    val rack = new RackController(_viewModel, inspector)
    val footer = new FooterController(_viewModel)

    root.setTop(header.getView)
    root.setLeft(inspector.getView)
    root.setCenter(rack.getView)
    root.setBottom(footer.getView)

    rack.bind()

    header.onPlayheadUpdate = pos =>
      if pos < 0 then rack.hidePlayhead()
      else rack.setPlayheadPosition(pos)

    Platform.runLater(() =>
      val stage = root.getScene.getWindow.asInstanceOf[javafx.stage.Stage]
      _viewModel.currentFilePathProperty.addListener((_, _, path) =>
        stage.setTitle(s"JagFX - $path")
      )
      stage.setTitle(s"JagFX - ${_viewModel.currentFilePathProperty.get}")
    )

    root
