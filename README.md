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
| **Partials** | 10 additive partials with volume, decicent offset, and time delay |
| **Filter** | IIR filter with pole/zero editor, frequency response visualisation |
| **Modulation** | FM (vibrato) and AM (tremolo) with envelope-controlled rate/depth |
| **Echo** | Configurable echo delay and mix level per voice |
| **Export** | Save as `.synth` or export to `.wav` (8-bit or 16-bit) |

## Quick Start

```bash
# Build solution
dotnet build

# Run CLI
dotnet run --project JagFX.CLI

# Run tests
dotnet test
```

## CLI Usage

The CLI supports both positional and flag-based arguments (but not mixed):

```bash
# Positional arguments
dotnet run --project JagFX.CLI -- input.synth output.wav
dotnet run --project JagFX.CLI -- input.synth output.wav 4

# Flag arguments  
dotnet run --project JagFX.CLI -- -i input.synth -o output.wav
dotnet run --project JagFX.CLI -- -i input.synth -o output.wav -l 4

# Inspect synth file structure
dotnet run --project JagFX.CLI -- inspect input.synth
```

## Building for Distribution

### Self-Contained Executable

Build a single executable for your platform:

```bash
# Windows (x64)
dotnet publish -c Release -r win-x64 --self-contained -o publish/win-x64

# Linux (x64)
dotnet publish -c Release -r linux-x64 --self-contained -o publish/linux-x64

# macOS (x64)
dotnet publish -c Release -r osx-x64 --self-contained -o publish/osx-x64

# macOS (ARM64)
dotnet publish -c Release -r osx-arm64 --self-contained -o publish/osx-arm64
```

### Framework-Dependent

```bash
dotnet publish -c Release -o publish
```

## Project Structure

```text
JagFX.sln                    # Solution file
├── JagFX.CLI/               # CLI application
│   ├── JagFXCli.cs          # CLI command handling
│   └── Program.cs           # Entry point
├── JagFX.Domain/            # Domain models and types
│   ├── Models/
│   │   ├── Voice.cs         # Voice configuration
│   │   ├── Envelope.cs      # Envelope with segments
│   │   ├── Filter.cs        # IIR filter parameters
│   │   ├── Lfo.cs           # LFO configuration
│   │   ├── Loop.cs          # Loop settings
│   │   ├── Oscillator.cs    # Oscillator with waveform
│   │   ├── Patch.cs         # Complete patch/sound
│   │   └── FeedbackDelay.cs # Echo/delay settings
│   ├── Utilities/
│   │   └── MathUtils.cs     # Math utilities (dB, conversions)
│   ├── Constants.cs         # Global constants
│   ├── Types.cs             # Millis, Percent, Samples
│   └── TestFixtures.cs      # Test data
├── JagFX.IO/                # File I/O operations
│   ├── SynthFileReader.cs   # Binary .synth reader
│   ├── SynthFileWriter.cs   # Binary .synth writer
│   ├── SynthInspector.cs    # Debug inspection tool
│   ├── BinaryBuffer.cs      # Binary buffer utilities
│   └── WaveFileWriter.cs    # WAV export
├── JagFX.Synthesis/         # DSP synthesis engine
│   ├── ToneSynthesizer.cs   # Per-voice synthesis
│   ├── TrackMixer.cs        # Multi-voice mixing
│   ├── FilterProcessor.cs   # IIR filter processing
│   ├── EnvelopeEvaluator.cs # Envelope evaluation
│   ├── SampleBuffer.cs      # Sample buffer type
│   ├── SampleBufferPool.cs  # Buffer pooling
│   └── LookupTables.cs      # Precomputed tables
├── JagFX.Synthesis.Tests/   # Synthesis unit tests
├── JagFX.IO.Tests/          # I/O unit tests
└── SmartInt/                # Smart integer types
    ├── src/
    │   ├── Smart16.cs
    │   └── USmart16.cs
    └── tests/
```

## Requirements

- .NET 8.0 SDK or later

## Examples

### `ice_cast.synth` and `ice_barrage_impact.synth`

<https://github.com/user-attachments/assets/b0564501-5d82-4239-8883-b32ab746e7dc>

## License

This project is licensed under MIT License. See [LICENSE](LICENSE) file for more details.

---

## Special Thanks

[Lost City](https://github.com/LostCityRS) - For having different client versions of `.synth` files

[OpenOSRS](https://github.com/open-osrs/runelite/tree/master/runescape-client) - For decompiled and partially deobfuscated files related to `.synth` format and IIR filter
