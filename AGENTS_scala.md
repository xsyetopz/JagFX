# JagFX AI Coding Agent Instructions

## Project Overview

JagFX is Scala 3 synthesizer editor for `.synth` binary files used in RuneScape. It enables loading, editing, synthesizing, and exporting audio from legacy format.

**Core Architecture:**

- `jagfx`: Main package containing entry points (`JagFX`, `JagFXCli`)
- `jagfx.model`: Immutable domain models (`Tone`, `Envelope`, `Filter`, `Partial`)
- `jagfx.io`: Binary serialization (`SynthReader`, `SynthWriter`, `BinaryBuffer`)
- `jagfx.synth`: DSP synthesis engine (`ToneSynthesizer`, `TrackSynthesizer`, `FilterSynthesizer`)
- `jagfx.tools`: Debugging and testing tools (`SynthInspector`)
- `jagfx.ui`: JavaFX UI layer with MVVM architecture
- `jagfx.utils`: Shared utilities (`ColorUtils`, `MathUtils`, `DrawingUtils`)

## Build System

### sbt Commands

```bash
# Run GUI application
sbt run

# Run CLI (synth-to-wav converter)
sbt "runMain jagfx.JagFXCli input.synth output.wav"

# Run debug inspector
sbt "runMain jagfx.tools.SynthInspector input.synth"

# Compile
sbt compile

# Format with scalafmt
sbt scalafmtAll
```

## Code Conventions

### File Organization (Idiomatic Scala 3)

Every file follows this section ordering:

1. **Types** - Nested enums, case classes, type aliases
2. **Fields** - `val`/`var` declarations with `// Fields` comment
3. **Init** - Constructor body, listeners, event handlers
4. **Public methods** - API surface
5. **Protected methods** - Template method hooks
6. **Private helpers** - Implementation details
7. **Companion object** - Factory methods, constants

### Documentation Style

- Use `/** ScalaDoc */` for public API only
- Use `// Section` comments to separate logical blocks
- NO inline comments explaining obvious code
- NO `TODO`, `FIXME`, or placeholder markers

### Naming Conventions

| Category | Pattern | Example |
|----------|---------|---------|
| UI Components | `Jag*` prefix | `JagButton`, `JagBaseCanvas` |
| View Models | `*ViewModel` suffix | `ToneViewModel`, `FilterViewModel` |
| Controllers | `*Controller` suffix | `MainController`, `HeaderController` |
| Inspectors | `*Inspector` suffix | `EnvelopeInspector`, `FilterInspector` |
| Synthesizers | `*Synthesizer` suffix | `ToneSynthesizer`, `TrackSynthesizer` |

## Architecture Patterns

### MVVM Pattern

```text
Model (jagfx.model)   ←→   ViewModel (jagfx.ui.viewmodel)   ←→   View (jagfx.ui.controller)
    Tone, Envelope            ToneViewModel                        RackController
```

- **Model**: Immutable case classes, no JavaFX dependencies
- **ViewModel**: Mutable JavaFX properties, bidirectional sync
- **View**: JavaFX nodes, binds to ViewModel properties

### Controller Pattern

Controllers extend `ControllerLike[V]`:

- `view: V` - Protected field for root JavaFX node
- `getView: V` - Public accessor
- `dispose()` - Cleanup method

### Canvas Rendering

Custom `JagBaseCanvas` uses:

- Software pixel buffer rendering (no GPU)
- `AnimationTimer` for 60 FPS updates
- `drawContent(buffer, width, height)` override for subclasses

## Binary Format Notes

### `.synth` File Structure

```text
[Tone 0..9] - Up to 10 tones, empty slots marked with 0x00
  ├── Pitch Envelope
  ├── Volume Envelope
  ├── Vibrato (optional pair)
  ├── Tremolo (optional pair)
  ├── Gate (optional pair)
  ├── Partials (variable length, 0x00 terminated)
  ├── Echo parameters
  ├── Duration/Start
  └── Filter (optional)
[Loop Parameters] - 4 bytes (start, end)
```

### Revision Compatibility

- **Rev377**: Uses 0x00 padding between tones and explicit filter markers
- **Rev245**: Packed format, no padding, heuristic filter detection

Patches in `SynthReader`:

- `applyRev377TonePadding()` - Skips trailing padding
- `applyRev245EnvelopePatch()` - Adds missing envelope segments

## Module Dependencies

```text
Constants, types    ←  model  ←  io, synth
                              ←  ui.viewmodel  ←  ui.components  ←  ui.controller
utils  ←  (everywhere)
```

## Key Files

- `src/main/scala/jagfx/Constants.scala`: Global constants
- `src/main/scala/jagfx/types.scala`: Opaque types (`Smart`, `Millis`, `Samples`)
- `src/main/scala/jagfx/io/SynthReader.scala`: Binary parser
- `src/main/scala/jagfx/synth/ToneSynthesizer.scala`: Main DSP logic
- `src/main/scala/jagfx/ui/controller/MainController.scala`: Root UI controller

## Common Pitfalls

1. **IArray for model fields**: Use `IArray` (immutable) not `Array` in case classes
2. **Platform.runLater**: Always wrap ViewModel updates from background threads
3. **BufferPool**: Always call `release()` after synthesis to return buffers
4. **16-bit range**: Use `Constants.Int16.Range` (65536) not hardcoded values
