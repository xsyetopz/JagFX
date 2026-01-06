# Jagex .synth File Format Specification

Reverse-engineered from RuneScape cache files (Lost City OS1 reference).

## Overview

Binary format for synthesized sound effects. Contains up to **`10` instrument voices ("tones")** with envelope-controlled pitch, amplitude, modulation, and IIR filtering.

**Byte Order**: Big Endian
**Sample Rate**: `22,050` Hz (fixed)

---

## Data Types

| Type | Size | Description |
|------|------|-------------|
| `u8` | `1` | Unsigned 8-bit |
| `u16` | `2` | Unsigned 16-bit BE |
| `s32` | `4` | Signed 32-bit BE |
| `smart` | `1-2` | Variable signed: `<128` encodes as `val - 64`; else `((b<<8)+next) - 49152` |
| `usmart`| `1-2` | Variable unsigned: `<128` encodes as `val`; else `((b-128)<<8) + next` |

---

## File Structure

```text
┌──────────────────────────┐
│  Voice Slot [0..9]       │
├──────────────────────────┤
│  Loop Begin (u16)        │  (Optional: if bytes remain)
│  Loop End (u16)          │
└──────────────────────────┘
```

### Voicee Slot Detection

Maximum `10` slots. Each detected via **Header Marker**:

- `0x00`: **Empty Slot**. (Consumes `1` byte. Parser advances to next slot).
- `!= 0x00`: **Voice Present**. (Byte NOT consumed during detection).
  - Marker equals **FormID** of Pitch Envelope.

### Truncation Handling

- **Voice**: If EOF reached, remaining slots assumed empty.
- **Filter**: If EOF reached during Filter read, Filter discarded (fallback to raw synth).
- **Loop**: If EOF reached before Loop Params, loop disabled (`0,0`).

---

## Voice Definition

| Field | Type | Notes |
|-------|------|-------|
| Pitch Envelope | `Envelope` | Frequency trajectory |
| Volume Envelope | `Envelope` | Amplitude trajectory |
| Vibrato | `OptPair` | Rate + Depth |
| Tremolo | `OptPair` | Rate + Depth |
| Gate | `OptPair` | Silence + Duration |
| Partials | `Partial[]` | Max `10`, null-terminated |
| Echo Delay | `usmart` | |
| Echo Mix | `usmart` | |
| Duration | `u16` | Total duration (ms) |
| Start Offset | `u16` | Start delay (ms) |
| Filter | `Filter` | Optional IIR |

**OptPair**: Peek byte. `0x00`=absent (consume `1`), else read two Envelopes.

---

## Envelope Structures

### 1. Full Envelope (Voice Params)

| Field | Type | Description |
|-------|------|-------------|
| Form | `u8` | `0`=Off, `1`=Square, `2`=Sine, `3`=Saw, `4`=Noise |
| Start | `s32` | Start Value |
| End | `s32` | End Value |
| Count | `u8` | Segment count |
| Segments | `Segment[]` | Linear interpolation segments |

### 2. Segment-Only Envelope (Filter)

| Field | Type | Description |
|-------|------|-------------|
| Count | `u8` | Segment count |
| Segments | `Segment[]` | Linear interpolation segments |

### Segment Definition

**Order**: Duration BEFORE Peak.

| Field | Type | Range |
|-------|------|-------|
| Duration | `u16` | Time delta |
| Peak | `u16` | Value at end of segment |

---

## Filter Definition

IIR filter. Max `4` pole pairs per direction.

| Field | Type | Notes |
|-------|------|-------|
| Packed Pairs | `u8` | `Count0 = byte >> 4`, `Count1 = byte & 0xF` |
| **IF Packed == 0** | | **Filter Empty (Return)** |
| Unity | `u16[2]` | Gain (Forward `0`, Forward `1`) |
| Mask | `u8` | Modulation Flags |
| Coeffs `0` | `(u16,u16)[]` | `Count0` pairs of (Freq, Mag) for Channel `0` |
| Coeffs `1` | `(u16,u16)[]` | `Count0` pairs of (Freq, Mag) for Channel `1`.<br>If`(Mask & (1<<(Ch*4)<<p))` set, read new values.<br>Else copy `Ch0` values. |
| Envelope | `SegEnv` | **Read ONLY if** `Mask != 0 \|\| Unity1 != Unity0`. Else None. |

---

## Partials

Read until `Volume (usmart) == 0` or Max `10` reached.

| Field | Type | Notes |
|-------|------|-------|
| Volume | `usmart` | If `0`, stop reading. |
| Semitone | `smart` | Signed relative pitch |
| Delay | `usmart` | Phase offset |

---

## Synthesis Flow

```mermaid
graph LR
    subgraph Data [Data Model]
      EnvP[Pitch Env]
      EnvV[Vol Env]
      FiltDef[Filter Def]
    end

    subgraph AudioLoop [Per Sample Processing]

      direction LR

      EnvP -->|Evaluate| Pitch[Pitch]
      EnvV -->|Evaluate| Amp[Amplitude]

      Pitch --> Osc[Oscillator]
      Amp --> Osc

      Osc --> Gating
      Gating --> Filter[IIR Filter]

      Filter --> Mix[Mix Partials]
      Mix --> Echo

      Echo --> Out[Sample Output]

      subgraph RateControl [Modulation]
         Vib[Vibrato] -.-> Pitch
         Trem[Tremolo] -.-> Amp
      end
    end

    FiltDef -.->|"Update Coeffs (128 samples)"| Filter
```

---

## Units & Evaluation

- **Smart Integers**:
  - `smart`: Signed.
  - `usmart`: Unsigned.

- **Envelope Runtime**:
  - Interpolates between `Start` and `End`.
  - `Segment.Peak` (`0-65535`) maps to `0.0-1.0` progress between Start and End values.
