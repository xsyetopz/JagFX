package jagfx.ui.controller.footer

import jagfx.ui.BindingManager
import jagfx.ui.components.slider.*
import jagfx.ui.viewmodel.SynthViewModel
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.*

// Constants
private final val PanelSize = 120
private final val SliderSize = 100

/** Echo controls panel. */
object EchoPanel:
  /** Creates echo panel with mix and delay sliders. */
  def create(viewModel: SynthViewModel, bindings: BindingManager): VBox =
    val panel = VBox()
    panel.getStyleClass.add("panel")
    panel.setMinWidth(PanelSize)
    panel.setPrefWidth(PanelSize)
    panel.setMaxWidth(PanelSize)
    HBox.setHgrow(panel, Priority.NEVER)

    val head = Label("ECHO")
    head.getStyleClass.add("panel-head")
    head.setMaxWidth(Double.MaxValue)
    head.setAlignment(Pos.CENTER)

    val mixSlider = JagBarSlider(0, SliderSize, 0, "MIX:")
    val delaySlider = JagBarSlider(0, SliderSize, 0, "DEL:")

    viewModel.activeToneIndexProperty.addListener((_, _, _) =>
      bindings.unbindAll()
      val activeTone = viewModel.getActiveTone
      bindings.bindBidirectional(mixSlider.valueProperty, activeTone.echoMix)
      bindings.bindBidirectional(
        delaySlider.valueProperty,
        activeTone.echoDelay
      )
    )

    panel.getChildren.addAll(head, mixSlider, delaySlider)
    panel
