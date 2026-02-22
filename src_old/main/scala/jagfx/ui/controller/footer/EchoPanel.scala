package jagfx.ui.controller.footer

import jagfx.ui.BindingManager
import jagfx.ui.components.slider.*
import jagfx.ui.viewmodel.SynthViewModel
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.*

// Constants
private final val EchoPanelSize = 120
private final val EchoSliderSize = 100

/** Echo controls panel. */
object EchoPanel:
  /** Creates echo panel with mix and delay sliders. */
  def create(viewModel: SynthViewModel, bindings: BindingManager): VBox =
    val panel = VBox()
    panel.getStyleClass.add("panel")
    panel.setMinWidth(EchoPanelSize)
    panel.setPrefWidth(EchoPanelSize)
    panel.setMaxWidth(EchoPanelSize)
    HBox.setHgrow(panel, Priority.NEVER)

    val head = Label("ECHO")
    head.getStyleClass.add("panel-head")
    head.setMaxWidth(Double.MaxValue)
    head.setAlignment(Pos.CENTER)

    val mixSlider = JagBarSlider(0, EchoSliderSize, 0, "MIX:")
    val delaySlider = JagBarSlider(0, EchoSliderSize, 0, "DEL:")

    viewModel.activeVoiceIndexProperty.addListener((_, _, _) =>
      bindings.unbindAll()
      val activeVoice = viewModel.getActiveVoice
      bindings.bindBidirectional(mixSlider.valueProperty, activeVoice.echoMix)
      bindings.bindBidirectional(
        delaySlider.valueProperty,
        activeVoice.echoDelay
      )
    )

    panel.getChildren.addAll(head, mixSlider, delaySlider)
    panel
