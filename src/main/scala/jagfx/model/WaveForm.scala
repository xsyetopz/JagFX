package jagfx.model

/** `Waveform` type for oscillators and LFOs. */
enum WaveForm(val id: Int):
  case Off extends WaveForm(0)
  case Square extends WaveForm(1)
  case Sine extends WaveForm(2)
  case Saw extends WaveForm(3)
  case Noise extends WaveForm(4)

/** `Waveform` companion with factory method. */
object WaveForm:
  /** Creates `WaveForm` from numeric ID (0-4). Unknown IDs return `Off`. */
  def fromId(id: Int): WaveForm = id match
    case 1 => Square
    case 2 => Sine
    case 3 => Saw
    case 4 => Noise
    case _ => Off
