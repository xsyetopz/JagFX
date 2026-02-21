# JagFX AI Coding Agent Instructions

## Project Overview

JagFX is synthesizer written in C# for `.synth` binary files used in RuneScape. It enables loading, editing, synthesizing, and exporting audio from legacy format.

**Core Architecture:**

- `JagFX.CLI`: CLI application entry point (System.CommandLine)
- `JagFX.Domain`: Domain models and types (immutable record classes)
- `JagFX.IO`: Binary file I/O (SynthFileReader, SynthFileWriter, WaveFileWriter)
- `JagFX.Synthesis`: DSP synthesis engine (synthesizers, filters, envelopes)
- `JagFX.Synthesis.Tests`: Synthesis unit tests
- `JagFX.IO.Tests`: I/O unit tests
- `SmartInt`: Smart integer utility library

## Build System

### dotnet CLI Commands

```bash
# Build entire solution
dotnet build

# Run CLI application
dotnet run --project JagFX.CLI

# Convert .synth to .wav (positional)
dotnet run --project JagFX.CLI -- input.synth output.wav
dotnet run --project JagFX.CLI -- input.synth output.wav 4

# Convert .synth to .wav (flags)
dotnet run --project JagFX.CLI -- -i input.synth -o output.wav
dotnet run --project JagFX.CLI -- -i input.synth -o output.wav -l 4

# Inspect .synth file
dotnet run --project JagFX.CLI -- inspect input.synth

# Run tests
dotnet test

# Format code
dotnet format
```

## Code Conventions

### File Organization (Idiomatic C#)

Every file follows this section ordering:

1. **Using directives** - Namespace imports
2. **Namespace declaration** - File-scoped namespace
3. **Types** - Classes, records, structs, enums
4. **Fields** - Private fields with `// Fields` comment
5. **Constructors** - Primary and secondary constructors
6. **Properties** - Public properties
7. **Public methods** - API surface
8. **Private helpers** - Implementation details

### Documentation Style

- Use `/// XML documentation` for public API only
- Use `// Section` comments to separate logical blocks
- NO inline comments explaining obvious code
- NO `TODO`, `FIXME`, or placeholder markers

### Naming Conventions

| Category | Pattern | Example |
|----------|---------|---------|
| Domain Models | `record class` | `Voice`, `Envelope`, `Filter` |
| Value Types | `readonly record struct` | `Millis`, `Percent`, `Samples` |
| Synthesizers | `*Synthesizer` suffix | `ToneSynthesizer`, `TrackMixer` |
| Evaluators | `*Evaluator` suffix | `EnvelopeEvaluator` |
| Processors | `*Processor` suffix | `FilterProcessor` |
| Utilities | Static classes | `MathUtils` |

## Architecture Patterns

### Domain Model Pattern

Models are immutable `record class` types with init-only properties:

```csharp
public record class Voice(
    Envelope FrequencyEnvelope,
    Envelope AmplitudeEnvelope,
    ImmutableList<Oscillator> Oscillators,
    ...
);
```

Value types use `readonly record struct`:

```csharp
public readonly record struct Millis(int Value);
public readonly record struct Samples(int Value);
public readonly record struct Percent(int Value);
```

### Parser Pattern

`SynthFileReader` uses an internal parser class:

```csharp
public static class SynthFileReader
{
    public static Patch Read(byte[] data) { ... }
    public static Patch ReadFromPath(string path) { ... }

    private class SynthParser(BinaryBuffer buf) { ... }
}
```

### Synthesis Pipeline

```text
Patch (from SynthFileReader)
    ↓
TrackMixer.Synthesize(patch, loopCount)
    ↓
For each voice: ToneSynthesizer.Synthesize(voice)
    ↓
SampleBuffer with synthesized audio
    ↓
WaveFileWriter.WriteToPath(bytes, path)
```

## Binary Format Notes

### `.synth` File Structure

```text
[Voice 0..9] - Up to 10 voices, empty slots marked with 0x00
  ├── Frequency Envelope (pitch)
  ├── Amplitude Envelope (volume)
  ├── Pitch LFO (vibrato - optional pair)
  ├── Amplitude LFO (tremolo - optional pair)
  ├── Gate Envelopes (silence/duration - optional pair)
  ├── Oscillators (variable length, 0x00 terminated)
  ├── Feedback Delay (echo parameters)
  ├── Duration/Start Time
  └── Filter (optional IIR filter)
[Loop Parameters] - 4 bytes (start, end)
```

### Revision Compatibility

Parser supports multiple `.synth` file format revisions:

- **Rev. 245 (2004-07-13):** Compact layout without padding; filter presence is detected heuristically.
- **Rev. 377 (2004-07-13):** Introduces `0x00` padding between voices for alignment.

Format detection and any necessary compatibility patches are handled automatically by parser.

For more details on RuneScape client revisions, see [roadmap](https://2004.lostcity.rs/roadmap).

## Module Dependencies

```text
SmartInt, Constants
       ↓
   JagFX.Domain (models, types, utilities)
       ↓
   JagFX.IO, JagFX.Synthesis
       ↓
   JagFX.CLI, Tests
```

## Key Files

- `JagFX.CLI/Commands/InspectCommand.cs`: Debug inspection tool
- `JagFX.Domain/Constants.cs`: SampleRate (22050), MaxVoices (10), FixedPoint scale
- `JagFX.Domain/Types.cs`: Millis, Percent, Samples value types
- `JagFX.Domain/Utilities/MathUtils.cs`: dB conversion, unit conversions
- `JagFX.IO/SynthFileReader.cs`: Binary parser for .synth files
- `JagFX.Synthesis/ToneSynthesizer.cs`: Per-voice synthesis logic
- `JagFX.Synthesis/TrackMixer.cs`: Multi-voice mixing and output

## Domain Model Reference

### Voice

```csharp
public record class Voice(
    Envelope FrequencyEnvelope,
    Envelope AmplitudeEnvelope,
    Lfo? PitchLfo,           // Vibrato
    Lfo? AmplitudeLfo,       // Tremolo
    Envelope? GateSilence,   // Gate silence envelope
    Envelope? GateDuration,  // Gate duration envelope
    ImmutableList<Oscillator> Oscillators,
    FeedbackDelay FeedbackDelay,
    int Duration,            // In milliseconds
    int StartTime,
    Filter? Filter = null
);
```

### Envelope

```csharp
public record class Envelope(
    Waveform Waveform,
    int Start,
    int End,
    ImmutableList<Segment> Segments
);

public record struct Segment(int Duration, int Peak);
```

### Oscillator

```csharp
public enum Waveform { Off = 0, Square = 1, Sine = 2, Saw = 3, Noise = 4 }

public record class Oscillator(
    Percent Amplitude,
    int PitchOffset,    // In decicents
    Millis Delay
);
```

## Common Pitfalls

1. **Immutable collections**: Use `ImmutableArray<T>` or `ImmutableList<T>` in models, not mutable arrays
2. **SampleBuffer lifecycle**: Always release buffers back to `SampleBufferPool`
3. **Fixed-point math**: Use `Constants.FixedPoint.Scale` (65536) for conversions
4. **Envelope evaluation**: Create new `EnvelopeEvaluator` per voice synthesis, call `Reset()` before use
5. **Millisecond conversions**: Use `Millis` and `Samples` types with their conversion methods
6. **Waveform IDs**: IDs 1-4 are valid (Square, Sine, Saw, Noise), 0 means Off
