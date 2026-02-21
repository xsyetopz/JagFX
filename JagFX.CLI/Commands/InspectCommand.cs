using JagFX.Domain;
using JagFX.IO;
using System.CommandLine;

namespace JagFX.CLI.Commands;

/// <summary>
/// CLI command for inspecting .synth file structure.
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

    private static void PrintHeader(string filePath, int size)
    {
        Console.WriteLine($"File: {filePath}");
        Console.WriteLine($"Size: {size} bytes ({size / 1024.0:F2} KB)");
        Console.WriteLine();
    }

    private static void InspectFile(InspectorContext ctx)
    {
        InspectVoices(ctx);
        InspectLoop(ctx);
        PrintSummary(ctx);
    }

    private static void InspectVoices(InspectorContext ctx)
    {
        Console.WriteLine("Voices:");
        for (int i = 0; i < Constants.MaxVoices; i++)
        {
            if (ctx.Buffer.Remaining == 0) break;

            var marker = ctx.Buffer.Peek();
            if (marker == 0)
            {
                var pos = ctx.Buffer.Position;
                ctx.Buffer.ReadUInt8();
                Console.WriteLine($"  Voice {i}: Empty (Offset 0x{pos:X4})");
                continue;
            }

            InspectVoice(ctx, i);
        }
        Console.WriteLine();
    }

    private static void InspectVoice(InspectorContext ctx, int voiceIndex)
    {
        Console.WriteLine($"  Voice {voiceIndex} (Offset 0x{ctx.Buffer.Position:X4}):");

        InspectEnvelope(ctx, "Pitch");
        InspectEnvelope(ctx, "Volume");
        InspectOptionalLFO(ctx, "Vibrato");
        InspectOptionalLFO(ctx, "Tremolo");
        InspectOptionalLFO(ctx, "Gate");
        InspectOscillators(ctx);

        ctx.ReadSmart("Feedback Amount", "Echo feedback amount");
        ctx.ReadSmart("Feedback Mix", "Echo dry/wet mix");
        ctx.ReadUInt16("Duration", "ms");
        ctx.ReadUInt16("Start Time", "ms");

        InspectFilter(ctx);

        Console.WriteLine();
    }

    private static void InspectEnvelope(InspectorContext ctx, string name)
    {
        Console.WriteLine($"    {name} Envelope:");
        ctx.ReadByte("      Waveform", GetWaveformName);
        ctx.ReadInt32("      Start Level", "fixed");
        ctx.ReadInt32("      End Level", "fixed");
        var segCount = ctx.ReadByte("      Segment Count", b => b.ToString());
        for (int i = 0; i < segCount; i++)
        {
            ctx.ReadUInt16($"      Segment {i} Duration", "ms");
            ctx.ReadUInt16($"      Segment {i} Peak", "fixed");
        }
    }

    private static void InspectOptionalLFO(InspectorContext ctx, string name)
    {
        var marker = ctx.Buffer.Peek();
        if (marker == 0)
        {
            ctx.ReadByte($"    {name} LFO Present", _ => "No");
            return;
        }

        ctx.ReadByte($"    {name} LFO Present", _ => "Yes");
        InspectEnvelope(ctx, $"{name} Rate");
        InspectEnvelope(ctx, $"{name} Depth");
    }

    private static void InspectOscillators(InspectorContext ctx)
    {
        Console.WriteLine("    Oscillators:");
        int oscIndex = 0;
        while (oscIndex < Constants.MaxOscillators)
        {
            if (ctx.Buffer.Remaining == 0) break;

            var marker = ctx.Buffer.Peek();
            if (marker == 0)
            {
                ctx.ReadByte("      Terminator", _ => "End");
                break;
            }

            Console.WriteLine($"      Oscillator {oscIndex}:");
            ctx.ReadSmart("        Amplitude", "%");
            ctx.ReadSmart("        Pitch Offset", "decicents");
            ctx.ReadSmart("        Delay", "ms");
            oscIndex++;
        }
    }

    private static void InspectFilter(InspectorContext ctx)
    {
        Console.WriteLine("    Filter:");
        if (!HasFilterData(ctx))
        {
            Console.WriteLine("      Not present (insufficient data)");
            return;
        }

        _ = ReadFilterConfig(ctx, out int ch0Pairs, out int ch1Pairs);
        if (!HasPolePairs(ch0Pairs, ch1Pairs))
        {
            Console.WriteLine("      Not present (no pole pairs)");
            return;
        }

        Console.WriteLine($"      Pole Pairs: Ch0={ch0Pairs}, Ch1={ch1Pairs}");
        ReadFilterGains(ctx);
        var modMask = ReadFilterModMask(ctx);

        InspectFilterPoles(ctx, ch0Pairs, ch1Pairs);

        if (modMask != 0)
        {
            InspectEnvelope(ctx, "Filter Mod");
        }
    }

    private static bool HasFilterData(InspectorContext ctx)
    {
        return ctx.Buffer.Remaining >= 5;
    }

    private static byte ReadFilterConfig(InspectorContext ctx, out int ch0Pairs, out int ch1Pairs)
    {
        var packed = ctx.ReadByte("      Channel Config", b => $"0x{b:X2}");
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
        ctx.ReadUInt16("      Unity Gain Ch0", "fixed");
        ctx.ReadUInt16("      Unity Gain Ch1", "fixed");
    }

    private static byte ReadFilterModMask(InspectorContext ctx)
    {
        return ctx.ReadByte("      Modulation Mask", b => $"0x{b:X2}");
    }

    private static void InspectFilterPoles(InspectorContext ctx, int ch0Pairs, int ch1Pairs)
    {
        for (int ch = 0; ch < 2; ch++)
        {
            int pairs = ch == 0 ? ch0Pairs : ch1Pairs;
            if (pairs == 0) continue;

            Console.WriteLine($"      Channel {ch} Poles:");
            for (int i = 0; i < pairs; i++)
            {
                ctx.ReadUInt16($"        Pole {i} Frequency", "Hz");
                ctx.ReadUInt16($"        Pole {i} Magnitude", "fixed");
            }
        }
    }

    private static void InspectLoop(InspectorContext ctx)
    {
        Console.WriteLine("Loop Parameters:");
        if (ctx.Buffer.Remaining >= 4)
        {
            ctx.ReadUInt16("  Loop Start", "samples");
            ctx.ReadUInt16("  Loop End", "samples");
        }
        else
        {
            Console.WriteLine("  Loop parameters missing or file truncated");
        }
        Console.WriteLine();
    }

    private static void PrintSummary(InspectorContext ctx)
    {
        Console.WriteLine("Summary:");
        Console.WriteLine($"  Total Bytes Read:    {ctx.Buffer.Position}");
        Console.WriteLine($"  Total Bytes in File: {ctx.Buffer.Data.Length}");
        Console.WriteLine($"  Bytes Remaining:     {ctx.Buffer.Remaining} ({ctx.Buffer.Remaining * 100.0 / ctx.Buffer.Data.Length:F1}%)");

        if (ctx.Buffer.Remaining > 0)
        {
            Console.WriteLine("  WARNING: Unparsed bytes remain at end of file");
        }
        else
        {
            Console.WriteLine("  Status: File parsed completely");
        }
        Console.WriteLine();
    }

    private static void PrintError(InspectorContext ctx, Exception ex)
    {
        Console.WriteLine("Parsing Error:");
        Console.WriteLine($"  Offset: 0x{ctx.Buffer.Position:X4}");
        Console.WriteLine($"  Error:  {ex.Message}");
    }

    private static string GetWaveformName(byte id)
    {
        return id switch
        {
            0 => "Off",
            1 => "Square",
            2 => "Sine",
            3 => "Saw",
            4 => "Noise",
            _ => $"Unknown ({id})"
        };
    }

    private class InspectorContext(byte[] data)
    {
        public BinaryBuffer Buffer { get; } = new(data);

        public byte ReadByte(string name, Func<byte, string> formatter)
        {
            return ReadAndPrint(() => (byte)Buffer.ReadUInt8(), name, formatter);
        }

        public int ReadSmart(string name, string unit)
        {
            return ReadAndPrint(Buffer.ReadSmart, name, v => $"{v} {unit}");
        }

        public int ReadInt32(string name, string unit)
        {
            return ReadAndPrint(Buffer.ReadInt32BE, name, v => $"{v} {unit}");
        }

        public int ReadUInt16(string name, string unit)
        {
            return ReadAndPrint(Buffer.ReadUInt16BE, name, v => $"{v} {unit}");
        }

        private static T ReadAndPrint<T>(Func<T> read, string name, Func<T, string> formatter)
        {
            var value = read();
            Console.WriteLine($"      {name}: {formatter(value)}");
            return value;
        }
    }
}
