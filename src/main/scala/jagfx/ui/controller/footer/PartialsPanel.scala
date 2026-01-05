package jagfx.ui.controller.footer

import javafx.scene.layout._
import javafx.scene.control.Label
import javafx.geometry.Pos
import javafx.beans.property.IntegerProperty
import javafx.beans.value.ChangeListener
import javafx.beans.binding.Bindings
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.ui.components.slider._
import jagfx.ui.components.field._
import jagfx.ui.components.button._
import jagfx.constants

/** Partials panel with P1-5/P6-10 bank switcher. */
object PartialsPanel:
  // store strip components for rebinding
  private case class HStrip(
      strip: VBox,
      label: Label,
      sRow: PartialsRow,
      vRow: PartialsRow,
      dRow: PartialsRow
  )

  private val _HalfMaxPartials = constants.MaxPartials - 5

  def create(viewModel: SynthViewModel): VBox =
    val panel = VBox()
    panel.getStyleClass.add("panel")
    HBox.setHgrow(panel, Priority.ALWAYS)

    // 0 = P1-5, 5 = P6-10
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

    val strips = new Array[HStrip](_HalfMaxPartials)
    for i <- 0 until _HalfMaxPartials do
      val hs = _createStrip(i)
      strips(i) = hs
      grid.getChildren.add(hs.strip)

    var volListeners =
      Array.fill[Option[(IntegerProperty, ChangeListener[Number])]](
        _HalfMaxPartials
      )(None)

    def bindPartials(): Unit =
      for i <- 0 until _HalfMaxPartials do
        val hIdx = bankOffset + i
        val h = viewModel.getActiveTone.partials(hIdx)

        // remove old listener
        volListeners(i).foreach { case (prop, listener) =>
          prop.removeListener(listener)
        }

        val hs = strips(i)
        hs.label.setText(s"PARTIAL ${hIdx + 1}")
        hs.sRow.bind(h.pitchOffset)
        hs.vRow.bind(h.volume)
        hs.dRow.bind(h.startDelay)

        val volListener = _createVolListener(hs)
        h.volume.addListener(volListener)
        volListeners(i) = Some((h.volume, volListener))

        volListener.changed(null, null, h.volume.get)

    bankBtn.setOnAction(_ =>
      bankOffset = if bankOffset == 0 then _HalfMaxPartials else 0
      bankBtn.setText(if bankOffset == 0 then "1-5" else "6-10")
      bindPartials()
    )

    viewModel.activeToneIndexProperty.addListener((_, _, _) => bindPartials())
    bindPartials()

    panel.getChildren.addAll(headRow, grid)
    panel

  private def _createStrip(index: Int): HStrip =
    val strip = VBox()
    strip.getStyleClass.add("h-strip")
    HBox.setHgrow(strip, Priority.ALWAYS)

    val label = Label(s"PARTIAL ${index + 1}")
    label.getStyleClass.add("h-head")

    val sRow = PartialsRow("SEMI:", -480, 480, 10.0, "%.1f")
    val vRow = PartialsRow("VOL:", 0, 100)
    val dRow = PartialsRow("DEL:", 0, 1000)

    strip.getChildren.addAll(label, sRow.view, vRow.view, dRow.view)
    HStrip(strip, label, sRow, vRow, dRow)

  private def _createVolListener(hs: HStrip): ChangeListener[Number] =
    (_, _, newVal) =>
      val dim = newVal.intValue == 0
      hs.strip.setOpacity(if dim then 0.5 else 1.0)
      hs.sRow.view.setDisable(dim)
      hs.dRow.view.setDisable(dim)

import javafx.scene.control._

/** Single row in partials strip. */
class PartialsRow(
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
  input.setPrefWidth(32)
  val scrollHelp = "\nScroll: ±1\nShift: ±10\nCmd: ±0.01"
  val tipText = labelTxt match
    case "PIT:" => "Pitch offset (decicents, 10 = 1 semitone)"
    case "VOL:" => "Partial volume (0-100%)"
    case "DEL:" => "Time delay (ms)"
    case _      => labelTxt

  input.setTooltip(new Tooltip(tipText + scrollHelp))

  val barBox = VBox()
  barBox.getStyleClass.add("bar-box")
  val barFill = Region()
  barFill.getStyleClass.add("bar-fill")
  barBox.getChildren.add(barFill)

  val spacer = new Region()
  HBox.setHgrow(spacer, Priority.ALWAYS)

  topRow.getChildren.addAll(label, spacer, input)
  view.getChildren.addAll(topRow, barBox)

  private var _currentProp: Option[IntegerProperty] = None

  def bind(prop: IntegerProperty): Unit =
    _currentProp.foreach(input.valueProperty.unbindBidirectional)
    input.valueProperty.bindBidirectional(prop)
    _currentProp = Some(prop)

    barFill.prefWidthProperty.bind(
      barBox.widthProperty.multiply(
        Bindings.createDoubleBinding(
          () => (prop.get - min).toDouble / (max - min),
          prop
        )
      )
    )
