package jagfx.ui.controller.footer

import javafx.scene.layout._
import javafx.scene.control.Label
import javafx.geometry.Pos
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.ui.components.slider._
import jagfx.ui.BindingManager

private val PanelSize = 120
private val SliderSize = 100

/** Reverb controls panel (`MIX`/`DAMP`). */
object ReverbPanel:
  def create(viewModel: SynthViewModel, bindings: BindingManager): VBox =
    val panel = VBox()
    panel.getStyleClass.add("panel")
    panel.setMinWidth(PanelSize)
    panel.setPrefWidth(PanelSize)
    panel.setMaxWidth(PanelSize)
    HBox.setHgrow(panel, Priority.NEVER)
    val head = Label("REVERB")
    head.getStyleClass.add("panel-head")
    head.setMaxWidth(Double.MaxValue)
    head.setAlignment(Pos.CENTER)
    head.setMaxWidth(Double.MaxValue)

    val volume = JagBarSlider(0, SliderSize, 0, "VOL:")
    val delay = JagBarSlider(0, SliderSize, 0, "DEL:")

    viewModel.activeToneIndexProperty.addListener((_, _, _) =>
      bindings.unbindAll()
      val tone = viewModel.getActiveTone
      bindings.bindBidirectional(volume.valueProperty, tone.reverbVolume)
      bindings.bindBidirectional(delay.valueProperty, tone.reverbDelay)
    )

    panel.getChildren.addAll(head, volume, delay)
    panel
