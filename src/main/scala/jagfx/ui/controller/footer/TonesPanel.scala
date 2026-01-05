package jagfx.ui.controller.footer

import jagfx.Constants
import jagfx.ui.components.button.*
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.utils.IconUtils
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.*

private val TonesPanelSize = 70
private val ColumnSize = 50

/** Tones selection panel (`1-10` buttons with copy/paste). */
object TonesPanel:
  /** Creates tones panel with tone selection and copy/paste buttons. */
  def create(viewModel: SynthViewModel): VBox =
    import Constants._

    val panel = VBox()
    panel.setId("tones-panel")
    panel.getStyleClass.add("panel")
    panel.setMinWidth(TonesPanelSize)
    panel.setPrefWidth(TonesPanelSize)
    panel.setMaxWidth(TonesPanelSize)
    HBox.setHgrow(panel, Priority.NEVER)

    val head = Label("TONES")
    head.getStyleClass.add("panel-head")
    head.setMaxWidth(Double.MaxValue)
    head.setAlignment(Pos.CENTER)

    val container = VBox()
    container.setId("tones-container")
    VBox.setVgrow(container, Priority.ALWAYS)

    val grid = GridPane()
    grid.setId("tones")
    grid.setHgap(2)
    grid.setVgap(2)
    VBox.setVgrow(grid, Priority.ALWAYS)

    val col1 = new ColumnConstraints()
    col1.setPercentWidth(ColumnSize)
    val col2 = new ColumnConstraints()
    col2.setPercentWidth(ColumnSize)
    grid.getColumnConstraints.addAll(col1, col2)

    val buttons = new Array[JagButton](MaxTones)
    for i <- 0 until MaxTones do
      val btn = JagButton((i + 1).toString)
      btn.setMaxWidth(Double.MaxValue)
      btn.setOnAction(_ =>
        buttons.foreach(_.setActive(false))
        btn.setActive(true)
        viewModel.setActiveToneIndex(i)
      )
      btn.setOnMouseClicked(e =>
        if e.getClickCount == 2 then
          val tone = viewModel.getTones.get(i)
          tone.enabled.set(!tone.enabled.get)
      )
      if i == 0 then btn.setActive(true)

      val tone = viewModel.getTones.get(i)
      val updateDim = (enabled: Boolean) =>
        btn.setOpacity(if enabled then 1.0 else 0.5)

      tone.enabled.addListener((_, _, enabled) => updateDim(enabled))
      updateDim(tone.enabled.get)

      buttons(i) = btn
      grid.add(btn, i % 2, i / 2)

    viewModel.activeToneIndexProperty.addListener((_, _, newIdx) =>
      for i <- 0 until MaxTones do buttons(i).setActive(i == newIdx.intValue)
    )

    val ops = HBox()
    ops.setId("tone-ops")
    val copyBtn = JagButton()
    copyBtn.setGraphic(IconUtils.icon("mdi2c-content-copy"))
    val pasteBtn = JagButton()
    pasteBtn.setGraphic(IconUtils.icon("mdi2c-content-paste"))
    HBox.setHgrow(copyBtn, Priority.ALWAYS)
    HBox.setHgrow(pasteBtn, Priority.ALWAYS)
    copyBtn.setMaxWidth(Double.MaxValue)
    pasteBtn.setMaxWidth(Double.MaxValue)

    copyBtn.setOnAction(_ => viewModel.copyActiveTone())
    pasteBtn.setOnAction(_ => viewModel.pasteToActiveTone())

    ops.getChildren.addAll(copyBtn, pasteBtn)

    container.getChildren.addAll(grid, ops)
    panel.getChildren.addAll(head, container)
    panel
