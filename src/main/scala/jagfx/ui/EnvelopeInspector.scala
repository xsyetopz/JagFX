package jagfx.ui

import javafx.scene.layout._
import javafx.scene.control.Label
import javafx.geometry.Pos
import jagfx.ui.components._
import jagfx.ui.viewmodel.EnvelopeViewModel
import jagfx.model.WaveForm

/** Inspector panel for envelope parameters. */
class EnvelopeInspector extends VBox:
  private var currentEnvelope: Option[EnvelopeViewModel] = None

  setSpacing(8)

  // Wave section
  private val waveLabel = Label("WAVE")
  waveLabel.getStyleClass.addAll("label", "h-head")

  private val waveGrid = JagToggleGroup(
    ("Off", "mdi2v-volume-off"),
    ("Sq", "mdi2s-square-wave"),
    ("Sin", "mdi2s-sine-wave"),
    ("Saw", "mdi2s-sawtooth-wave"),
    ("Nse", "mdi2w-waveform")
  )
  waveGrid.setAlignment(Pos.CENTER)

  waveGrid.selectedProperty.addListener((_, _, newVal) =>
    currentEnvelope.foreach { env =>
      val form = newVal match
        case "Off" => WaveForm.Off
        case "Sqr" => WaveForm.Square
        case "Sin" => WaveForm.Sine
        case "Saw" => WaveForm.Saw
        case "Nse" => WaveForm.Noise
        case _     => WaveForm.Off
      env.form.set(form)
    }
  )

  // Range section
  private val rangeLabel = Label("RANGE")
  rangeLabel.getStyleClass.addAll("label", "h-head")

  private val startField = JagNumericField(-999999, 999999, 0)
  startField.setPrefWidth(55)
  startField.valueProperty.addListener((_, _, nv) =>
    currentEnvelope.foreach(_.start.set(nv.intValue))
  )

  private val endField = JagNumericField(-999999, 999999, 0)
  endField.setPrefWidth(55)
  endField.valueProperty.addListener((_, _, nv) =>
    currentEnvelope.foreach(_.end.set(nv.intValue))
  )

  private val rangeRow = HBox(4)
  rangeRow.setAlignment(Pos.CENTER_LEFT)
  rangeRow.getChildren.addAll(
    Label("S"),
    startField,
    new Region() { HBox.setHgrow(this, Priority.ALWAYS) },
    Label("E"),
    endField
  )

  // Segments section
  private val segLabel = Label("SEGMENTS")
  segLabel.getStyleClass.addAll("label", "h-head")

  private val segCountLabel = Label("0 pts")
  segCountLabel.getStyleClass.add("label")
  segCountLabel.setStyle("-fx-text-fill: #888;")

  private val segRow = HBox(4)
  segRow.setAlignment(Pos.CENTER_LEFT)
  segRow.getChildren.addAll(
    segLabel,
    new Region() { HBox.setHgrow(this, Priority.ALWAYS) },
    segCountLabel
  )

  getChildren.addAll(waveLabel, waveGrid, rangeLabel, rangeRow, segRow)

  /** Bind to envelope view model. */
  def bind(envelope: EnvelopeViewModel): Unit =
    currentEnvelope = Some(envelope)

    val formStr = envelope.form.get match
      case WaveForm.Square => "Sqr"
      case WaveForm.Sine   => "Sin"
      case WaveForm.Saw    => "Saw"
      case WaveForm.Noise  => "Nse"
      case _               => "Off"

    waveGrid.setSelected(formStr)
    startField.setValue(envelope.start.get)
    endField.setValue(envelope.end.get)
    segCountLabel.setText(s"${envelope.getSegments.length} pts")

  /** Get help text for this inspector mode. */
  def getHelpText: String =
    "Wave: Oscillator shape\nS/E: Start and end values\nSegments shown on canvas"

object EnvelopeInspector:
  def apply(): EnvelopeInspector = new EnvelopeInspector()
