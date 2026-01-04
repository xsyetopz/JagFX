# JagFX - Jagex Synth Editor

<p align="left">
  <img
    src="assets/370_cow_death.png"
    alt="Cow Death"
    onerror="this.onerror=null;this.src='https://raw.githubusercontent.com/xsyetopz/jagfx-scala/main/assets/370_cow_death.png';"
  >
</p>

Cross-platform editor for Jagex Audio Synthesis (`.synth`) files. Create, edit, visualize, and export older/newer OldSchool RuneScape sound effects.

## Features

| Category | Description |
|----------|----------|
| **Envelopes** | Pitch, Volume, Vibrato (Rate/Depth), Tremolo (Rate/Depth), Gate (Silence/Duration) |
| **Harmonics** | 10 additive partials with volume, semitone offset, and phase delay |
| **Filter** | IIR filter with pole/zero editor, frequency response visualisation |
| **Modulation** | FM (vibrato) and AM (tremolo) with envelope-controlled rate/depth |
| **Reverb** | Configurable delay and mix level per tone |
| **Export** | Save as `.synth` or export to `.wav` (8-bit or 16-bit) |

## Quick Start

```bash
# Run GUI
sbt dev

# Run tests
sbt test
```

## CLI Usage

Batch convert files without opening GUI:

```bash
# Convert single file
sbt "cli input.synth output.wav"

# Convert with loop count
sbt "cli input.synth output_looped.wav 4"
```

## Building for Distribution

```bash
sbt dist
```

Creates `target/universal/jagfx-1.0.0.zip` with platform launchers for macOS, Linux, and Windows.

## Project Structure

```text
src/main/scala/jagfx/
├── io/              # Binary .synth reader/writer
├── model/           # Data models (Tone, Envelope, Filter)
├── synth/           # DSP engine (oscillators, filters, synthesis)
├── ui/              # JavaFX controllers and components
├── utils/           # Utilities (icons, colors, preferences)
├── JagFX.scala      # GUI entry point
└── JagFXCli.scala   # CLI entry point
```

## Requirements

- JDK 21+
- sbt 1.11+
- Node.js (for SCSS compilation)

## Examples

### `ice_cast.synth` and `ice_barrage_impact.synth`

<https://github.com/user-attachments/assets/6a0216bb-44e4-4d96-a53e-27e7c986313d>

## License

This project is licensed under MIT License. See [LICENSE](LICENSE) file for more details.
