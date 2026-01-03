package jagfx.ui.controller

import javafx.scene.layout._
import javafx.scene.control.Label
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.ui.components._
import javafx.beans.binding.Bindings
import javafx.beans.property.IntegerProperty
import javafx.beans.value.ChangeListener
import jagfx.ui.viewmodel.RackMode
import jagfx.utils.IconUtils
import javafx.geometry.Pos
import jagfx.Constants
import jagfx.ui.BindingManager

class FooterController(viewModel: SynthViewModel) extends IController[HBox]:
  protected val view = HBox()
  view.getStyleClass.add("footer")
  private val reverbBindings = BindingManager()

  private val tonesPanel = createTonesPanel()
  private val harmonicsPanel = createHarmonicsPanel()
  private val reverbPanel = createReverbPanel()
  private val modePanel = createModePanel()

  view.getChildren.addAll(tonesPanel, harmonicsPanel, reverbPanel, modePanel)

  private def createTonesPanel(): VBox =
    val panel = VBox()
    panel.setId("tones-panel")
    panel.getStyleClass.add("panel")
    panel.setMinWidth(70)
    panel.setPrefWidth(70)
    panel.setMaxWidth(70)
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
    col1.setPercentWidth(50)
    val col2 = new ColumnConstraints()
    col2.setPercentWidth(50)
    grid.getColumnConstraints.addAll(col1, col2)

    import Constants._
    val buttons = new Array[JagButton](MaxTones)
    for i <- 0 until MaxTones do
      val btn = JagButton((i + 1).toString)
      btn.setMaxWidth(Double.MaxValue)
      btn.setOnAction(_ =>
        buttons.foreach(_.setActive(false))
        btn.setActive(true)
        viewModel.setActiveToneIndex(i)
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

  private def createHarmonicsPanel(): VBox =
    val panel = VBox()
    panel.getStyleClass.add("panel")
    HBox.setHgrow(panel, Priority.ALWAYS)

    val head = Label("HARMONICS")
    head.getStyleClass.add("panel-head")
    head.setMaxWidth(Double.MaxValue)
    head.setAlignment(Pos.CENTER)

    val grid = HBox(4)
    grid.setId("harmonics")
    VBox.setVgrow(grid, Priority.ALWAYS)

    for i <- 0 until 5 do
      val strip = VBox()
      strip.getStyleClass.add("h-strip")
      HBox.setHgrow(strip, Priority.ALWAYS)

      val label = Label(s"H${i + 1}")
      label.getStyleClass.add("h-head")

      val sRow = new HarmonicsRow("SEMI", -120, 120, 9.0, "%.1f")
      val vRow = new HarmonicsRow("VOL", 0, 100)
      val dRow = new HarmonicsRow("DEL", 0, 1000)

      strip.getChildren.addAll(label, sRow.view, vRow.view, dRow.view)
      grid.getChildren.add(strip)

      var currentVolProp: Option[IntegerProperty] = None
      val volListener: ChangeListener[Number] = (_, _, newVal) =>
        val dim = newVal.intValue == 0
        strip.setOpacity(if dim then 0.5 else 1.0)
        sRow.view.setDisable(dim)
        dRow.view.setDisable(dim)

      val bindHarmonic = () =>
        val h = viewModel.getActiveTone.harmonics(i)

        currentVolProp.foreach(_.removeListener(volListener))

        sRow.bind(h.semitone)
        vRow.bind(h.volume)
        dRow.bind(h.delay)

        h.volume.addListener(volListener)
        currentVolProp = Some(h.volume)

        volListener.changed(null, null, h.volume.get)

      viewModel.activeToneIndexProperty.addListener((_, _, _) => bindHarmonic())
      bindHarmonic()

    panel.getChildren.addAll(head, grid)
    panel

  private class HarmonicsRow(
      labelTxt: String,
      min: Int,
      max: Int,
      scale: Double = 1.0,
      format: String = "%.0f"
  ):
    val view = VBox()
    view.getStyleClass.add("h-row")

    val topRow = HBox()
    topRow.getStyleClass.add("h-sub-row")

    val label = Label(labelTxt)
    label.getStyleClass.add("h-lbl")

    val input = JagNumericField(min, max, 0, scale, format)
    input.setPrefWidth(34)

    val barBox = VBox()
    barBox.getStyleClass.add("bar-box")
    val barFill = Region()
    barFill.getStyleClass.add("bar-fill")
    barBox.getChildren.add(barFill)

    val spacer = new Region()
    HBox.setHgrow(spacer, Priority.ALWAYS)

    topRow.getChildren.addAll(label, spacer, input)
    view.getChildren.addAll(topRow, barBox)

    private var currentProp: Option[IntegerProperty] = None

    def bind(prop: IntegerProperty): Unit =
      currentProp.foreach(input.valueProperty.unbindBidirectional)
      input.valueProperty.bindBidirectional(prop)
      currentProp = Some(prop)

      barFill.prefWidthProperty.bind(
        barBox.widthProperty.multiply(
          Bindings.createDoubleBinding(
            () => (prop.get - min).toDouble / (max - min),
            prop
          )
        )
      )

  private def createReverbPanel(): VBox =
    val panel = VBox()
    panel.getStyleClass.add("panel")
    panel.setMinWidth(120)
    panel.setPrefWidth(120)
    panel.setMaxWidth(120)
    HBox.setHgrow(panel, Priority.NEVER)
    val head = Label("REVERB")
    head.getStyleClass.add("panel-head")
    head.setMaxWidth(Double.MaxValue)
    head.setAlignment(Pos.CENTER)
    head.setMaxWidth(Double.MaxValue)

    val mix = JagBarSlider(0, 100, 0, "MIX")
    val damp = JagBarSlider(0, 100, 0, "DAMP")

    viewModel.activeToneIndexProperty.addListener((_, _, _) =>
      reverbBindings.unbindAll()
      val tone = viewModel.getActiveTone
      reverbBindings.bindBidirectional(mix.valueProperty, tone.reverbVolume)
      reverbBindings.bindBidirectional(damp.valueProperty, tone.reverbDelay)
    )

    panel.getChildren.addAll(head, mix, damp)
    panel

  private def createModePanel(): VBox =
    val panel = VBox()
    panel.getStyleClass.add("panel")
    panel.setMinWidth(70)
    panel.setPrefWidth(70)
    panel.setMaxWidth(70)
    HBox.setHgrow(panel, Priority.NEVER)

    val head = Label("MODE")
    head.getStyleClass.add("panel-head")
    head.setMaxWidth(Double.MaxValue)
    head.setAlignment(Pos.CENTER)

    val btnMain = JagButton("MAIN")
    val btnFilter = JagButton("FILTER")
    val btnBoth = JagButton("BOTH")

    btnMain.setMaxWidth(Double.MaxValue)
    btnFilter.setMaxWidth(Double.MaxValue)
    btnBoth.setMaxWidth(Double.MaxValue)

    btnMain.setOnAction(_ => viewModel.rackMode.set(RackMode.Main))
    btnFilter.setOnAction(_ => viewModel.rackMode.set(RackMode.Filter))
    btnBoth.setOnAction(_ => viewModel.rackMode.set(RackMode.Both))

    viewModel.rackMode.addListener((_, _, mode) =>
      btnMain.setActive(mode == RackMode.Main)
      btnFilter.setActive(mode == RackMode.Filter)
      btnBoth.setActive(mode == RackMode.Both)
    )
    btnMain.setActive(true)

    val container = VBox(2)
    container.setId("mode-container")
    VBox.setVgrow(container, Priority.ALWAYS)
    container.getChildren.addAll(btnMain, btnFilter, btnBoth)

    panel.getChildren.addAll(head, container)
    panel
