package jagfx.ui.controller.rack

import jagfx.ui.viewmodel.EnvelopeViewModel
import jagfx.ui.viewmodel.ToneViewModel

/** Rack cell content type determining inspector behavior. */
enum CellType:
  case Envelope(
      getter: ToneViewModel => EnvelopeViewModel,
      inspectorMode: Boolean = true
  )
  case Filter
  case Output

/** Definition for single rack cell with title, description, and type. */
case class RackCellDef(
    title: String,
    desc: String,
    cellType: CellType,
    enabled: Boolean = true
)

/** Rack cell definitions for all envelope and filter slots. */
object RackDefs:
  val cellDefs: Vector[RackCellDef] = Vector(
    RackCellDef(
      "PITCH",
      "Defines base pitch trajectory. Envelope values are added to fundamental frequency over time.",
      CellType.Envelope(_.pitch)
    ),
    RackCellDef(
      "V.RATE",
      "Modulates speed of vibrato (FM). Higher values create faster pitch wobbling.",
      CellType.Envelope(_.vibratoRate)
    ),
    RackCellDef(
      "V.DEPTH",
      "Controls intensity of vibrato. Higher values create deeper pitch variations.",
      CellType.Envelope(_.vibratoDepth)
    ),
    RackCellDef(
      "P/Z",
      "Visualizes IIR filter poles/zeros. Drag points to shape frequency response.",
      CellType.Filter
    ),
    RackCellDef(
      "VOLUME",
      "Shapes overall loudness over time. Use to create durations, decays, and swells.",
      CellType.Envelope(_.volume)
    ),
    RackCellDef(
      "T.RATE",
      "Modulates speed of tremolo (AM). Higher values create faster volume fluctuations.",
      CellType.Envelope(_.tremoloRate)
    ),
    RackCellDef(
      "T.DEPTH",
      "Controls intensity of tremolo. Higher values create deeper volume cuts.",
      CellType.Envelope(_.tremoloDepth)
    ),
    RackCellDef(
      "FILTER",
      "Interpolates between initial/final filter states. 0 = Start Filter, 1 = End Filter.",
      CellType.Envelope(_.filterEnvelope)
    ),
    RackCellDef(
      "OUTPUT",
      "Real-time visualization of synthesized waveform for active tone.",
      CellType.Output
    ),
    RackCellDef(
      "G.SIL",
      "Controls duration of silence (gate closed) phase.",
      CellType.Envelope(_.gateSilence)
    ),
    RackCellDef(
      "G.DUR",
      "Controls duration of audible (gate open) phase.",
      CellType.Envelope(_.gateDuration)
    ),
    RackCellDef(
      "BODE",
      "Displays frequency magnitude response (Bode plot) of active filter.",
      CellType.Filter
    )
  )
