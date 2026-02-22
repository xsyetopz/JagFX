package jagfx.ui.controller.inspector

import jagfx.model.Waveform
import jagfx.ui.components.field.*
import jagfx.ui.components.group.*
import jagfx.ui.viewmodel.EnvelopeViewModel
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*

// Constants
private final val RangeFieldSize = 48

/** Inspector panel for envelope parameters. */
class EnvelopeInspector extends VBox:
  // Fields
  private var currentEnvelope: Option[EnvelopeViewModel] = None
  private val waveLabel = Label("WAVE")
  private val waveGrid = JagToggleGroup(
    ("Off", "mdi2v-volume-off"),
    ("Sqr", "mdi2s-square-wave"),
    ("Sin", "mdi2s-sine-wave"),
    ("Saw", "mdi2s-sawtooth-wave"),
    ("Nse", "mdi2w-waveform")
  )
  private val rangeLabel = Label("RANGE")
  private val maxRangeValue = 999999
  private val startField = JagNumericField(-maxRangeValue, maxRangeValue, 0)
  private val endField = JagNumericField(-maxRangeValue, maxRangeValue, 0)
  private val rangeRow = HBox(4)
  private val segLabel = Label("SEGMENTS")
  private val segmentEditor = EnvelopeSegmentEditor()

  // Init: styling
  setSpacing(8)

  waveLabel.getStyleClass.addAll("label", "height-head")
  waveGrid.setAlignment(Pos.CENTER)

  rangeLabel.getStyleClass.addAll("label", "height-head")
  startField.setPrefWidth(RangeFieldSize)
  startField.setTooltip(new Tooltip("Envelope start value"))
  endField.setPrefWidth(RangeFieldSize)
  endField.setTooltip(new Tooltip("Envelope end value"))
  rangeRow.setAlignment(Pos.CENTER_LEFT)

  segLabel.getStyleClass.addAll("label", "height-head")

  // Init: listeners
  waveGrid.selectedProperty.addListener((_, _, newVal) =>
    currentEnvelope.foreach { env =>
      val waveform = newVal match
        case "Off" => Waveform.Off
        case "Sqr" => Waveform.Square
        case "Sin" => Waveform.Sine
        case "Saw" => Waveform.Saw
        case "Nse" => Waveform.Noise
        case _     => Waveform.Off
      env.waveform.set(waveform)
    }
  )

  startField.valueProperty.addListener((_, _, nv) =>
    currentEnvelope.foreach(_.start.set(nv.intValue))
  )

  endField.valueProperty.addListener((_, _, nv) =>
    currentEnvelope.foreach(_.end.set(nv.intValue))
  )

  // Init: build hierarchy
  rangeRow.getChildren.addAll(
    Label("S:"),
    startField,
    new Region() { HBox.setHgrow(this, Priority.ALWAYS) },
    Label("E:"),
    endField
  )

  getChildren.addAll(
    waveLabel,
    waveGrid,
    rangeLabel,
    rangeRow,
    segLabel,
    segmentEditor
  )

  /** Binds envelope view model to inspector. */
  def bind(envelope: EnvelopeViewModel): Unit =
    currentEnvelope = Some(envelope)

    val formStr = envelope.waveform.get match
      case Waveform.Square => "Sqr"
      case Waveform.Sine   => "Sin"
      case Waveform.Saw    => "Saw"
      case Waveform.Noise  => "Nse"
      case _               => "Off"

    waveGrid.setSelected(formStr)
    startField.setValue(envelope.start.get)
    endField.setValue(envelope.end.get)
    segmentEditor.bind(envelope)
