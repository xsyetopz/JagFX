package jagfx.ui.controller

import jagfx.ui.controller.footer.FooterController
import jagfx.ui.controller.header.HeaderController
import jagfx.ui.controller.inspector.InspectorController
import jagfx.ui.controller.rack.RackController
import jagfx.ui.viewmodel.SynthViewModel
import javafx.application.Platform
import javafx.scene.layout.BorderPane

/** Root controller wiring all UI sections. */
object MainController:
  // Fields
  private val viewModel = new SynthViewModel()

  /** Creates root UI component. */
  def createRoot(): BorderPane =
    val root = BorderPane()
    root.getStyleClass.add("root")

    val header = new HeaderController(viewModel)
    val inspector = new InspectorController(viewModel)
    val rack = new RackController(viewModel, inspector)
    val footer = new FooterController(viewModel)

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
      viewModel.currentFilePathProperty.addListener((_, _, path) =>
        stage.setTitle(s"JagFX - $path")
      )
      stage.setTitle(s"JagFX - ${viewModel.currentFilePathProperty.get}")
    )

    root
