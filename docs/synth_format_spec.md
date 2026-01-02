# Jagex .synth File Format Specification

Reverse-engineered from OSRS cache files.

## Overview

Binary format describing synthesized sound effects. Contains up to `10` instrument voices ("tones") with envelope-controlled pitch, amplitude, modulation, and effects.

**Byte Order**: Big Endian (network order)

## Data Types

| Type | Size | Description |
|------|------|-------------|
| `u8` | `1` | Unsigned 8-bit integer |
| `s32` | `4` | Signed 32-bit big-endian integer |
| `u16` | `2` | Unsigned 16-bit big-endian integer |
| `smart` | `1-2` | Variable-length unsigned integer (see below) |
| `ssmart` | `1-2` | Variable-length signed integer (see below) |

### Smart Integer Encoding

**Unsigned (`smart`)**:

- If first byte < `128`: `value = byte`
- Else: `value = ((byte - 128) << 8) + next_byte`

**Signed (`ssmart`)**:

- If first byte < `128`: `value = byte - 64`
- Else: `value = ((byte << 8) + next_byte) - 49152`

## File Structure

```text
┌─────────────────────────────┐
│  Tone Slots [0..9]          │  10 slots, each either empty or full
├─────────────────────────────┤
│  Loop Begin (u16)           │  Loop start in milliseconds
│  Loop End (u16)             │  Loop end in milliseconds
└─────────────────────────────┘
```

## Tone Slot

Each slot starts with presence byte:

- `0x00`: Empty slot (`1` byte consumed)
- `!= 0x00`: Tone definition follows (byte NOT consumed, rewind before reading)

## Tone Definition

| Field | Type | Description |
|-------|------|-------------|
| Pitch Envelope | `Envelope` | Fundamental frequency trajectory |
| Volume Envelope | `Envelope` | Amplitude trajectory |
| Vibrato | `Optional Pair` | Frequency modulation (rate + depth) |
| Tremolo | `Optional Pair` | Amplitude modulation (rate + depth) |
| Gate | `Optional Pair` | On/off switching (silence + duration) |
| Harmonics | `Harmonic[]` | Additive partials (max `10`) |
| Reverb Delay | `smart` | Echo delay in milliseconds |
| Reverb Volume | `smart` | Echo mix level (`0`-`100`) |
| Duration | `u16` | Total tone length in milliseconds |
| Start Offset | `u16` | Delay before tone begins in milliseconds |

### Optional Pair

Presence check: peek first byte

- `0x00`: Not present (consume `1` byte)
- `!= 0x00`: Two envelopes follow (rate envelope, then depth envelope)

## Envelope Structure

| Field | Type | Description |
|-------|------|-------------|
| Form | `u8` | Waveform: `0`=Off, `1`=Square, `2`=Sine, `3`=Saw, `4`=Noise |
| Start | `s32` | Base value |
| End | `s32` | Target value |
| Segment Count | `u8` | Number of interpolation segments |
| Segments | `Segment[]` | Control points |

### Segment

| Field | Type | Description |
|-------|------|-------------|
| Duration | `u16` | Segment length (normalized `0`-`65535`) |
| Peak | `u16` | Interpolation target (`0`=Start, `65535`=End) |

## Harmonic Structure

Read harmonics until volume = `0` (terminator):

| Field | Type | Description |
|-------|------|-------------|
| Volume | `smart` | Amplitude (`0`=end, `1`-`100`=percentage) |
| Semitone | `ssmart` | Pitch offset in decicents (`10` = `1` semitone) |
| Delay | `smart` | Phase offset in milliseconds |

## Units Reference

| Parameter | Unit | Conversion |
|-----------|------|------------|
| Frequency | Jagex Pitch Units | `Hz ≈ value × 22.05` |
| Semitone | Decicents | `10` decicents = `1` semitone, `120` = `1` octave |
| Duration | Milliseconds | Direct |
| Peak | Normalized `0`-`65535` | `0` = Start value, `65535` = End value |
| Sample Rate | — | `22050` Hz (fixed) |

## Loop Region

If `loop_begin < loop_end`:

- Audio between begin and end repeats for specified count
- Loop expansion appends repeated region to output

If `loop_begin >= loop_end` or either is `0`:

- Loop disabled, single playback

## Implementation Notes

1. **Envelope Interpolation**: Segments act as control points. Amplitude transitions linearly between peak values, scaled to Start/End range.

2. **Harmonic Synthesis**: Each harmonic generates sine/square/saw at fundamental × semitone offset. Limit: `5` harmonics per sample.

3. **Phase Calculation**: 15-bit resolution (`0x7FFF` mask). Square duty cycle: `50%` (threshold `16384`).

4. **Clipping**: Output clamped to `[-32768, 32767]` before 8-bit WAV conversion.

## Example: `cow_death.synth`

Real file from OSRS cache (`129` bytes, `1` active tone):

```text
; === TONE 0 (Pitch Envelope) ===
02                          ; Form: Sine
00 00 00 1e                 ; Start: 30 (pitch units)
00 00 01 2c                 ; End: 300 (pitch units)
05                          ; Segment count: 5
00 00 15 be                 ; Seg 0: dur=0, peak=5566
57 0b 59 c6                 ; Seg 1: dur=22283, peak=22982
b6 46 88 c4                 ; Seg 2: dur=46662, peak=35012
48 b5 ff ff                 ; Seg 3: dur=18613, peak=65535

; === TONE 0 (Volume Envelope) ===
00                          ; Form: Off
00 00 00 00                 ; Start: 0
00 00 00 00                 ; End: 0
00                          ; Segment count: 0

; === TONE 0 (Vibrato) ===
64                          ; Present (form=Noise)
05 00 00 7b e8              ; Vibrato rate envelope...
2e fd b5 40 8c f9           ; ...
84 19 bf 79 27 f0           ; Vibrato depth envelope...
ff ff                       ; ...

; === TONE 0 (Tremolo) ===
00                          ; Not present

; === TONE 0 (Gate) ===
01                          ; Present
00 00 00 00 00 00 00 96     ; Gate silence envelope
02 00 00 7d f4 ff ff 7b e8  ; Gate duration envelope

; === TONE 0 (Harmonics) ===
00                          ; Not present (terminator immediately)
00 00 64                    ; Harmonic 1: vol=100, semi=0
03 00 00 06 25              ; More harmonic data...
33 e6 4d d3 ff ff ff ff     ; ...

; === TONE 0 (Reverb + Timing) ===
00 00                       ; Reverb delay: 0
64 40                       ; Reverb volume: 100
00 3c                       ; Duration: 60ms
c0 85                       ; Start offset: varies

; === TONES 1-9 (Empty) ===
00 00 00 64 03 84           ; Empty slots...
00 00 00 00 00 00 00 00 00  ; ...
00 00 00 00 00 00 00        ; ...

; === LOOP PARAMETERS ===
00 00                       ; Loop begin: 0
00 00                       ; Loop end: 0 (no loop)
```
