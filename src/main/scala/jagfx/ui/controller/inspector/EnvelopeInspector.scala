package jagfx.ui.controller.inspector

import javafx.scene.layout._
import javafx.scene.control._
import javafx.geometry.Pos
import jagfx.ui.viewmodel.EnvelopeViewModel
import jagfx.ui.components.field._
import jagfx.ui.components.group._
import jagfx.ui.components.slider._
import jagfx.ui.components.pane._
import jagfx.model.WaveForm

private val RangeFieldSize = 48

/** Inspector panel for envelope parameters. */
class EnvelopeInspector extends VBox:
  private var currentEnvelope: Option[EnvelopeViewModel] = None

  setSpacing(8)

  // Wave section
  private val waveLabel = Label("WAVE")
  waveLabel.getStyleClass.addAll("label", "h-head")

  private val waveGrid = JagToggleGroup(
    ("Off", "mdi2v-volume-off"),
    ("Sqr", "mdi2s-square-wave"),
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

  private val maxRangeValue = 999999

  private val startField = JagNumericField(-maxRangeValue, maxRangeValue, 0)
  startField.setPrefWidth(RangeFieldSize)
  startField.setTooltip(new Tooltip("Envelope start value"))
  startField.valueProperty.addListener((_, _, nv) =>
    currentEnvelope.foreach(_.start.set(nv.intValue))
  )

  private val endField = JagNumericField(-maxRangeValue, maxRangeValue, 0)
  endField.setPrefWidth(RangeFieldSize)
  endField.setTooltip(new Tooltip("Envelope end value"))
  endField.valueProperty.addListener((_, _, nv) =>
    currentEnvelope.foreach(_.end.set(nv.intValue))
  )

  private val rangeRow = HBox(4)
  rangeRow.setAlignment(Pos.CENTER_LEFT)
  rangeRow.getChildren.addAll(
    Label("S:"),
    startField,
    new Region() { HBox.setHgrow(this, Priority.ALWAYS) },
    Label("E:"),
    endField
  )

  // Segments section
  private val segLabel = Label("SEGMENTS")
  segLabel.getStyleClass.addAll("label", "h-head")

  private val segmentEditor = EnvelopeSegmentEditor()

  getChildren.addAll(
    waveLabel,
    waveGrid,
    rangeLabel,
    rangeRow,
    segLabel,
    segmentEditor
  )

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
    segmentEditor.bind(envelope)
