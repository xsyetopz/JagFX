package jagfx.ui.controller.footer

import jagfx.Constants
import jagfx.ui.components.button.*
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.utils.IconUtils
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.*

// Constants
private final val VoicesPanelSize = 70
private final val VoicesColumnSize = 50

/** Voices selection panel (`1-10` buttons with copy/paste). */
object VoicesPanel:
  /** Creates voices panel with voice selection and copy/paste buttons. */
  def create(viewModel: SynthViewModel): VBox =
    import Constants._

    val panel = VBox()
    panel.setId("voices-panel")
    panel.getStyleClass.add("panel")
    panel.setMinWidth(VoicesPanelSize)
    panel.setPrefWidth(VoicesPanelSize)
    panel.setMaxWidth(VoicesPanelSize)
    HBox.setHgrow(panel, Priority.NEVER)

    val head = Label("TONES")
    head.getStyleClass.add("panel-head")
    head.setMaxWidth(Double.MaxValue)
    head.setAlignment(Pos.CENTER)

    val container = VBox()
    container.setId("voices-container")
    VBox.setVgrow(container, Priority.ALWAYS)

    val grid = GridPane()
    grid.setId("voices")
    grid.setHgap(2)
    grid.setVgap(2)
    VBox.setVgrow(grid, Priority.ALWAYS)

    val col1 = new ColumnConstraints()
    col1.setPercentWidth(VoicesColumnSize)
    val col2 = new ColumnConstraints()
    col2.setPercentWidth(VoicesColumnSize)
    grid.getColumnConstraints.addAll(col1, col2)

    val buttons = new Array[JagButton](MaxVoices)
    for i <- 0 until MaxVoices do
      val btn = JagButton((i + 1).toString)
      btn.setMaxWidth(Double.MaxValue)
      btn.setOnAction(_ =>
        buttons.foreach(_.setActive(false))
        btn.setActive(true)
        viewModel.setActiveVoiceIndex(i)
      )
      btn.setOnMouseClicked(e =>
        if e.getClickCount == 2 then
          val voice = viewModel.getVoices.get(i)
          voice.enabled.set(!voice.enabled.get)
      )
      if i == 0 then btn.setActive(true)

      val voice = viewModel.getVoices.get(i)
      val updateDim = (enabled: Boolean) =>
        btn.setOpacity(if enabled then 1.0 else 0.5)

      voice.enabled.addListener((_, _, enabled) => updateDim(enabled))
      updateDim(voice.enabled.get)

      buttons(i) = btn
      grid.add(btn, i % 2, i / 2)

    viewModel.activeVoiceIndexProperty.addListener((_, _, newIdx) =>
      for i <- 0 until MaxVoices do buttons(i).setActive(i == newIdx.intValue)
    )

    val ops = HBox()
    ops.setId("voice-ops")
    val copyBtn = JagButton()
    copyBtn.setGraphic(IconUtils.icon("mdi2c-content-copy"))
    val pasteBtn = JagButton()
    pasteBtn.setGraphic(IconUtils.icon("mdi2c-content-paste"))
    HBox.setHgrow(copyBtn, Priority.ALWAYS)
    HBox.setHgrow(pasteBtn, Priority.ALWAYS)
    copyBtn.setMaxWidth(Double.MaxValue)
    pasteBtn.setMaxWidth(Double.MaxValue)

    copyBtn.setOnAction(_ => viewModel.copyActiveVoice())
    pasteBtn.setOnAction(_ => viewModel.pasteToActiveVoice())

    ops.getChildren.addAll(copyBtn, pasteBtn)

    container.getChildren.addAll(grid, ops)
    panel.getChildren.addAll(head, container)
    panel
