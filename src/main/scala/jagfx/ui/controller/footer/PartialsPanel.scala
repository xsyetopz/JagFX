package jagfx.ui.controller.footer

import jagfx.Constants
import jagfx.ui.components.button.*
import jagfx.ui.components.field.*
import jagfx.ui.viewmodel.SynthViewModel
import javafx.beans.binding.Bindings
import javafx.beans.property.IntegerProperty
import javafx.beans.value.ChangeListener
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*

// Constants
private final val HalfMaxPartials = Constants.MaxPartials - 5

/** Partials panel with P1-5/P6-10 bank switcher. */
object PartialsPanel:
  // Types
  private case class HStrip(
      strip: VBox,
      label: Label,
      sRow: PartialsRow,
      vRow: PartialsRow,
      dRow: PartialsRow
  )

  /** Creates partials panel with bank switching and sliders. */
  def create(viewModel: SynthViewModel): VBox =
    val panel = VBox()
    panel.getStyleClass.add("panel")
    HBox.setHgrow(panel, Priority.ALWAYS)

    var bankOffset = 0

    val headRow = StackPane()
    headRow.setAlignment(Pos.CENTER)

    val head = Label("PARTIALS")
    head.getStyleClass.add("panel-head")
    head.setMaxWidth(Double.MaxValue)
    head.setAlignment(Pos.CENTER)

    val bankBtn = JagButton("1-5")
    bankBtn.setPrefWidth(40)
    StackPane.setAlignment(bankBtn, Pos.CENTER_RIGHT)
    StackPane.setMargin(bankBtn, new javafx.geometry.Insets(0, 4, 0, 0))

    headRow.getChildren.addAll(head, bankBtn)

    val grid = HBox(2)
    grid.setId("partials")
    VBox.setVgrow(grid, Priority.ALWAYS)

    val strips = new Array[HStrip](HalfMaxPartials)
    for i <- 0 until HalfMaxPartials do
      val hs = createStrip(i)
      strips(i) = hs
      grid.getChildren.add(hs.strip)

    val volListeners =
      Array.fill[Option[(IntegerProperty, ChangeListener[Number])]](
        HalfMaxPartials
      )(None)

    /** Binds partials to this view model. */
    def bindPartials(): Unit =
      for i <- 0 until HalfMaxPartials do
        val hIdx = bankOffset + i
        val height = viewModel.getActiveTone.partials(hIdx)

        volListeners(i).foreach { case (prop, listener) =>
          prop.removeListener(listener)
        }

        val hs = strips(i)
        hs.label.setText(s"PARTIAL ${hIdx + 1}")
        hs.sRow.bind(height.pitchOffset)
        hs.vRow.bind(height.volume)
        hs.dRow.bind(height.startDelay)

        val volListener = createVolListener(hs)
        height.volume.addListener(volListener)
        volListeners(i) = Some((height.volume, volListener))

        volListener.changed(null, null, height.volume.get)

    bankBtn.setOnAction(_ =>
      bankOffset = if bankOffset == 0 then HalfMaxPartials else 0
      bankBtn.setText(if bankOffset == 0 then "1-5" else "6-10")
      bindPartials()
    )

    viewModel.activeToneIndexProperty.addListener((_, _, _) => bindPartials())
    bindPartials()

    panel.getChildren.addAll(headRow, grid)
    panel

  private def createStrip(index: Int): HStrip =
    val strip = VBox()
    strip.getStyleClass.add("height-strip")
    HBox.setHgrow(strip, Priority.ALWAYS)

    val label = Label(s"PARTIAL ${index + 1}")
    label.getStyleClass.add("height-head")

    val sRow = PartialsRow("SEMI:", -480, 480, 10.0, "%.1f")
    val vRow = PartialsRow("VOL:", 0, 100)
    val dRow = PartialsRow("DEL:", 0, 1000)

    strip.getChildren.addAll(label, sRow.view, vRow.view, dRow.view)
    HStrip(strip, label, sRow, vRow, dRow)

  private def createVolListener(hs: HStrip): ChangeListener[Number] =
    (_, _, newVal) =>
      val dim = newVal.intValue == 0
      hs.strip.setOpacity(if dim then 0.5 else 1.0)
      hs.sRow.view.setDisable(dim)
      hs.dRow.view.setDisable(dim)

/** Single row in partials strip. */
class PartialsRow(
    labelTxt: String,
    min: Int,
    max: Int,
    scale: Double = 1.0,
    format: String = "%.0f"
):
  // Fields
  val view: VBox = VBox()
  val topRow: HBox = HBox()
  val label: Label = Label(labelTxt)
  val input: JagNumericField = JagNumericField(min, max, 0, scale, format)
  val barBox: VBox = VBox()
  val barFill: Region = Region()
  val spacer: Region = Region()
  val tipText: String = labelTxt match
    case "PIT:" => "Pitch offset (decicents, 10 = 1 semitone)"
    case "VOL:" => "Partial volume (0-100%)"
    case "DEL:" => "Time delay (ms)"
    case _      => labelTxt

  private var currentProp: Option[IntegerProperty] = None

  // Init: styling
  view.getStyleClass.add("height-row")
  topRow.getStyleClass.add("height-sub-row")
  label.getStyleClass.add("height-lbl")
  input.setPrefWidth(32)
  input.setTooltip(
    new Tooltip(tipText + "\nScroll: ±1\nShift: ±10\nCmd: ±0.01")
  )
  barBox.getStyleClass.add("bar-box")
  barFill.getStyleClass.add("bar-fill")
  HBox.setHgrow(spacer, Priority.ALWAYS)

  // Init: build hierarchy
  barBox.getChildren.add(barFill)
  topRow.getChildren.addAll(label, spacer, input)
  view.getChildren.addAll(topRow, barBox)

  /** Binds property to input field and bar. */
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
