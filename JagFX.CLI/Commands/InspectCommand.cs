using JagFX.Domain;
using JagFX.IO;
using System.CommandLine;

namespace JagFX.CLI.Commands;

/// <summary>
/// CLI command for inspecting .synth file structure with educational output.
/// </summary>
public class InspectCommand : Command
{
    public InspectCommand() : base("inspect", "Inspect synth file structure")
    {
        var fileArgument = new Argument<string>("file")
        {
            Description = "Path to .synth file"
        };

        Arguments.Add(fileArgument);

        SetAction((parseResult) =>
        {
            var filePath = parseResult.GetValue(fileArgument);
            return Execute(filePath);
        });
    }

    private static int Execute(string? filePath)
    {
        if (!ValidateFilePath(filePath, out var validPath))
            return 1;

        var bytes = ReadFileBytes(validPath);
        var ctx = new InspectorContext(bytes);

        PrintHeader(validPath, bytes.Length);

        try
        {
            InspectFile(ctx);
            return 0;
        }
        catch (Exception ex)
        {
            PrintError(ctx, ex);
            return 1;
        }
    }

    private static bool ValidateFilePath(string? filePath, out string validPath)
    {
        validPath = filePath ?? string.Empty;
        if (string.IsNullOrWhiteSpace(validPath))
        {
            Console.Error.WriteLine("Error: File path is required");
            return false;
        }

        if (!File.Exists(validPath))
        {
            Console.Error.WriteLine($"Error: File not found: {validPath}");
            return false;
        }

        return true;
    }

    private static byte[] ReadFileBytes(string filePath)
    {
        return File.ReadAllBytes(filePath);
    }

    private static void InspectFile(InspectorContext ctx)
    {
        InspectVoices(ctx);
        InspectLoop(ctx);
        PrintSummary(ctx);
    }

    private static void PrintHeader(string filePath, int size)
    {
        BoxHeader("JAGEX SYNTH FILE ANALYZER");
        BoxField($"File: {filePath}", $"Size: {size} bytes ({size / 1024.0:F2} KB)");
        BoxFooter();
        Console.WriteLine();
    }

    private static void InspectVoices(InspectorContext ctx)
    {
        SectionHeader("SYNTHESIS OVERVIEW");
        SectionText(
            "A .synth file contains up to 10 voices (layers). Each voice defines a",
            "complete synthesizer patch with envelopes, LFOs, oscillators, and filter.",
            "",
            "Signal Flow: Pitch Env -> LFOs -> Oscillators ─┐",
            "                                                ├─→ Mixer ─→ Filter ─→ Output",
            "              Amp Env   -> LFOs ────────────────┘"
        );
        SectionFooter();

        for (int i = 0; i < Constants.MaxVoices; i++)
        {
            if (ctx.Buffer.Remaining == 0) break;

            var marker = ctx.Buffer.Peek();
            if (marker == 0)
            {
                var pos = ctx.Buffer.Position;
                var _ = ctx.Buffer.ReadUInt8();
                BoxHeader($"VOICE {i} (Offset: 0x{pos:X4}) - EMPTY");
                BoxField("Marker: 0x00 (unused slot)");
                BoxFooter();
                Console.WriteLine();
                continue;
            }

            InspectVoice(ctx, i);
        }
    }

    private static void InspectVoice(InspectorContext ctx, int voiceIndex)
    {
        BoxHeader($"VOICE {voiceIndex} (Offset: 0x{ctx.Buffer.Position:X4})");

        EnvelopeSection("PITCH ENVELOPE", "Controls frequency/pitch over time",
            "This envelope modulates the base pitch of the voice over time.",
            "Format: [Waveform:1] [Start:4] [End:4] [SegCount:1] [(Duration,Peak)...]");
        InspectEnvelope(ctx, "Pitch");

        EnvelopeSection("VOLUME ENVELOPE", "Controls amplitude/loudness over time");
        InspectEnvelope(ctx, "Volume");

        EnvelopeSection("VIBRATO LFO", "Pitch modulation for vibrato effect",
            "Vibrato adds periodic pitch variation. Two envelopes define:",
            "  - Rate: How fast the vibrato oscillates",
            "  - Depth: How much pitch varies");
        InspectOptionalLFO(ctx, "Vibrato");

        EnvelopeSection("TREMOLO LFO", "Amplitude modulation for tremolo effect");
        InspectOptionalLFO(ctx, "Tremolo");

        EnvelopeSection("GATE ENVELOPES", "Optional silence/duration control");
        InspectOptionalLFO(ctx, "Gate");

        EnvelopeSection("OSCILLATORS", "Sound generators - the actual tone sources",
            "Up to 10 additive oscillators. Each contributes a waveform to the mix.",
            "Format per oscillator: [Amplitude:var] [PitchOffset:var] [Delay:var]",
            "Terminated by 0x00.");
        InspectOscillators(ctx);

        EnvelopeSection("FEEDBACK DELAY", "Echo effect parameters");
        ctx.ReadSmart("Feedback Amount", "Echo feedback amount", "Higher = more echoes");
        ctx.ReadSmart("Feedback Mix", "Echo dry/wet mix", "Balance between dry and echoed signal");

        EnvelopeSection("TIMING PARAMETERS", "Timing parameters");
        ctx.ReadUInt16("Duration", "ms", "Total voice duration in milliseconds");
        ctx.ReadUInt16("Start Time", "ms", "Delay before voice starts playing");

        EnvelopeSection("FILTER", "IIR filter for frequency shaping",
            "Optional IIR filter with configurable poles/zeros for EQ-like effects.");
        InspectFilter(ctx);

        Console.WriteLine();
    }

    private static void InspectEnvelope(InspectorContext ctx, string name)
    {
        SectionHeader($"{name.ToUpper()} ENVELOPE");
        ctx.ReadByte("Waveform", GetWaveformName, "Oscillator shape for envelope modulation");
        ctx.ReadInt32("Start Level", "fixed", "Initial envelope value (fixed-point)");
        ctx.ReadInt32("End Level", "fixed", "Final envelope value (fixed-point)");
        var segCount = ctx.ReadByte("Segment Count", b => b.ToString(), "Number of envelope segments");

        for (int i = 0; i < segCount; i++)
        {
            ctx.ReadUInt16($"Segment {i} Duration", "ms", "Time to reach peak");
            ctx.ReadUInt16($"Segment {i} Peak", "fixed", "Peak value at segment end");
        }
        SectionFooter();
    }

    private static void InspectOptionalLFO(InspectorContext ctx, string name)
    {
        SectionHeader($"{name.ToUpper()} LFO");
        var marker = ctx.Buffer.Peek();
        if (marker == 0)
        {
            ctx.ReadByte($"{name} LFO Present", _ => "No", "0x00 = LFO not present");
            SectionFooter();
            return;
        }

        ctx.ReadByte($"{name} LFO Present", _ => "Yes", "Non-zero = LFO follows");
        SectionText($"{name} Rate Envelope (controls {name.ToLower()} speed over time):");
        InspectEnvelope(ctx, $"{name} Rate");
        SectionText($"{name} Depth Envelope (controls {name.ToLower()} amount over time):");
        InspectEnvelope(ctx, $"{name} Depth");
        SectionFooter();
    }

    private static void InspectOscillators(InspectorContext ctx)
    {
        SectionHeader("OSCILLATORS");
        int oscIndex = 0;
        while (oscIndex < Constants.MaxOscillators)
        {
            if (ctx.Buffer.Remaining == 0) break;

            var marker = ctx.Buffer.Peek();
            if (marker == 0)
            {
                ctx.ReadByte("Terminator", _ => "End of Oscillators", "0x00 marks end of oscillator list");
                break;
            }

            SectionText($"OSCILLATOR {oscIndex}:");
            ctx.ReadSmart("  Amplitude", "%", "Relative volume of this oscillator");
            ctx.ReadSmart("  Pitch Offset", "decicents", "Fine pitch adjustment (1/10th of cent)");
            ctx.ReadSmart("  Delay", "ms", "Time before oscillator starts");
            oscIndex++;
        }
        SectionFooter();
    }

    private static void InspectFilter(InspectorContext ctx)
    {
        SectionHeader("FILTER");
        if (!HasFilterData(ctx))
        {
            SectionText("Filter: Not present (insufficient data remaining)");
            SectionFooter();
            return;
        }

        var packed = ReadFilterConfig(ctx, out int ch0Pairs, out int ch1Pairs);
        if (!HasPolePairs(ch0Pairs, ch1Pairs))
        {
            SectionText("Filter: Not present (no pole pairs defined)");
            SectionFooter();
            return;
        }

        SectionText($"Pole Pairs: Channel 0 = {ch0Pairs}, Channel 1 = {ch1Pairs}");
        ReadFilterGains(ctx);
        var modMask = ReadFilterModMask(ctx);

        InspectFilterPoles(ctx, ch0Pairs, ch1Pairs);

        if (modMask != 0)
        {
            SectionText("Filter Modulation Envelope:");
            InspectEnvelope(ctx, "Filter Mod");
        }
        SectionFooter();
    }

    private static bool HasFilterData(InspectorContext ctx)
    {
        return ctx.Buffer.Remaining >= 5;
    }

    private static byte ReadFilterConfig(InspectorContext ctx, out int ch0Pairs, out int ch1Pairs)
    {
        var packed = ctx.ReadByte("Channel Config", b => $"0x{b:X2}", "Packed byte: [Ch0Pairs:4 | Ch1Pairs:4]");
        ch0Pairs = packed >> 4;
        ch1Pairs = packed & 0x0F;
        return packed;
    }

    private static bool HasPolePairs(int ch0Pairs, int ch1Pairs)
    {
        return ch0Pairs != 0 || ch1Pairs != 0;
    }

    private static void ReadFilterGains(InspectorContext ctx)
    {
        ctx.ReadUInt16("Unity Gain Ch0", "fixed", "Filter gain for channel 0");
        ctx.ReadUInt16("Unity Gain Ch1", "fixed", "Filter gain for channel 1");
    }

    private static byte ReadFilterModMask(InspectorContext ctx)
    {
        return ctx.ReadByte("Modulation Mask", b => $"0x{b:X2}", "Bit flags for modulated poles");
    }

    private static void InspectFilterPoles(InspectorContext ctx, int ch0Pairs, int ch1Pairs)
    {
        for (int ch = 0; ch < 2; ch++)
        {
            int pairs = ch == 0 ? ch0Pairs : ch1Pairs;
            if (pairs == 0) continue;

            SectionText($"Channel {ch} Poles:");
            for (int i = 0; i < pairs; i++)
            {
                ctx.ReadUInt16($"  Pole {i} Frequency", "Hz", "Pole center frequency");
                ctx.ReadUInt16($"  Pole {i} Magnitude", "fixed", "Pole magnitude (resonance)");
            }
        }
    }

    private static void InspectLoop(InspectorContext ctx)
    {
        BoxHeader("LOOP PARAMETERS");
        SectionText(
            "Defines start and end points for looping playback."
        );
        if (ctx.Buffer.Remaining >= 4)
        {
            ctx.ReadUInt16("Loop Start", "samples", "Sample index where loop begins");
            ctx.ReadUInt16("Loop End", "samples", "Sample index where loop ends");
        }
        else
        {
            SectionText("?? - Loop parameters missing or file truncated");
        }
        BoxFooter();
        Console.WriteLine();
    }

    private static void PrintSummary(InspectorContext ctx)
    {
        BoxHeader("ANALYSIS SUMMARY");
        BoxField(
            $"Total Bytes Read:    {ctx.Buffer.Position}",
            $"Total Bytes in File: {ctx.Buffer.Data.Length}",
            $"Bytes Remaining:     {ctx.Buffer.Remaining} ({ctx.Buffer.Remaining * 100.0 / ctx.Buffer.Data.Length:F1}%)"
        );

        if (ctx.Buffer.Remaining > 0)
        {
            SectionText(
                "WARNING: Unparsed bytes remain at end of file",
                "These may be:",
                "  - Padding bytes (format revision dependent)",
                "  - Truncated file",
                "  - Unknown data format"
            );
        }
        else
        {
            SectionText("Status: File parsed completely");
        }
        BoxFooter();
    }

    private static void PrintError(InspectorContext ctx, Exception ex)
    {
        BoxHeader("PARSING ERROR");
        BoxField($"Offset: 0x{ctx.Buffer.Position:X4}", $"Error:  {ex.Message}");
        BoxFooter();
    }

    private static string GetWaveformName(byte id)
    {
        return id switch
        {
            0 => "Off (silent)",
            1 => "Square",
            2 => "Sine",
            3 => "Saw",
            4 => "Noise",
            _ => $"?? Unknown ({id})"
        };
    }

    private static void BoxHeader(string title)
    {
        Console.WriteLine("╔══════════════════════════════════════════════════════════════════════════════╗");
        Console.WriteLine($"║ {title,-74} ║");
        Console.WriteLine("╠══════════════════════════════════════════════════════════════════════════════╣");
    }

    private static void BoxField(params string[] fields)
    {
        foreach (var field in fields)
            Console.WriteLine($"║ {field,-74} ║");
    }

    private static void BoxFooter()
    {
        Console.WriteLine("╚══════════════════════════════════════════════════════════════════════════════╝");
    }

    private static void SectionHeader(string title)
    {
        Console.WriteLine("┌──────────────────────────────────────────────────────────────────────────────┐");
        Console.WriteLine($"│ {title,-74} │");
        Console.WriteLine("├──────────────────────────────────────────────────────────────────────────────┤");
    }

    private static void SectionText(params string[] lines)
    {
        foreach (var line in lines)
            Console.WriteLine($"│ {line,-74} │");
    }

    private static void SectionFooter()
    {
        Console.WriteLine("└──────────────────────────────────────────────────────────────────────────────┘");
    }

    private static void EnvelopeSection(string title, string subtitle, params string[] extra)
    {
        BoxHeader(title);
        BoxField(subtitle);
        foreach (var line in extra)
            BoxField(line);
        BoxFooter();
    }

    private class InspectorContext(byte[] data)
    {
        public BinaryBuffer Buffer { get; } = new(data);

        public byte ReadByte(string name, Func<byte, string> formatter, string description)
        {
            return ReadAndPrint(() => (byte)Buffer.ReadUInt8(), name, formatter, description);
        }

        public int ReadSmart(string name, string unit, string description)
        {
            return ReadAndPrint(Buffer.ReadSmart, name, v => FormatUnit(v, unit), description);
        }

        public int ReadInt32(string name, string unit, string description)
        {
            return ReadAndPrint(Buffer.ReadInt32BE, name, v => FormatUnit(v, unit), description);
        }

        public int ReadUInt16(string name, string unit, string description)
        {
            return ReadAndPrint(Buffer.ReadUInt16BE, name, v => FormatUnit(v, unit), description);
        }

        private T ReadAndPrint<T>(Func<T> read, string name, Func<T, string> formatter, string description)
        {
            var pos = Buffer.Position;
            var value = read();
            var formatted = formatter(value);
            PrintField(pos, name, value is null ? "null" : value, formatted, description);
            return value;
        }

        private static string FormatUnit(int value, string unit)
        {
            return unit == "fixed" ? $"{value} (raw fixed-point)" : $"{value} {unit}";
        }

        private static void PrintField(int pos, string name, object raw, string formatted, string description)
        {
            Console.WriteLine($"│ 0x{pos:X4} │ {name,-25} │ {raw,-15} │ {formatted,-20} │");
            Console.WriteLine($"│        │   -> {description}");
        }
    }
}
