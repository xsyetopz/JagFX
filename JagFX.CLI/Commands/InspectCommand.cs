using JagFX.Domain;
using JagFX.IO;
using System.CommandLine;

namespace JagFX.CLI.Commands;

/// <summary>
/// CLI command for inspecting .synth file structure in assembly-like format.
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
        if (string.IsNullOrWhiteSpace(filePath))
        {
            Console.Error.WriteLine("Error: File path is required");
            return 1;
        }

        if (!File.Exists(filePath))
        {
            Console.Error.WriteLine($"Error: File not found: {filePath}");
            return 1;
        }

        var bytes = File.ReadAllBytes(filePath);
        var ctx = new InspectorContext(bytes);

        Console.WriteLine($"; File: {filePath}");
        Console.WriteLine($"; Size: {bytes.Length} bytes");
        Console.WriteLine();

        try
        {
            InspectVoices(ctx);
            InspectLoop(ctx);
            PrintSummary(ctx);
            return 0;
        }
        catch (Exception ex)
        {
            PrintError(ctx, ex);
            return 1;
        }
    }

    private static void InspectVoices(InspectorContext ctx)
    {
        for (var i = 0; i < Constants.MaxVoices; i++)
        {
            if (ctx.Buffer.Remaining == 0) break;

            var marker = ctx.Buffer.Peek();
            if (marker == 0)
            {
                ctx.ReadByte("empty", $"voice {i}");
                continue;
            }

            InspectVoice(ctx, i);
        }
    }

    private static void InspectVoice(InspectorContext ctx, int voiceIndex)
    {
        var marker = ctx.Buffer.Peek();
        ctx.PrintLine($"voice {voiceIndex}", $"active, wf={GetWaveformName((byte)marker)}");

        InspectEnvelope(ctx, "penv");
        InspectEnvelope(ctx, "aenv");

        InspectOptionalLFO(ctx, "vib");
        InspectOptionalLFO(ctx, "trem");
        InspectOptionalLFO(ctx, "gate");

        InspectOscillators(ctx);

        ctx.ReadSmart("echo", "feedback");
        ctx.ReadSmart("", "mix");

        ctx.ReadUInt16("time", "dur");
        ctx.ReadUInt16("", "start");

        InspectFilter(ctx);
    }

    private static void InspectEnvelope(InspectorContext ctx, string _)
    {
        ctx.ReadByte("", "wf");
        ctx.ReadInt32("", "start");
        ctx.ReadInt32("", "end");
        ctx.ReadByte("", $"segs={ctx.LastByte}");

        for (var i = 0; i < ctx.LastByte; i++)
        {
            ctx.ReadUInt16("", $"seg{i}.dur");
            ctx.ReadUInt16("", $"seg{i}.peak");
        }
    }

    private static void InspectOptionalLFO(InspectorContext ctx, string label)
    {
        var marker = ctx.Buffer.Peek();
        if (marker == 0)
        {
            ctx.ReadByte("", $"{label}=none");
            return;
        }

        ctx.PrintLine(label, "present");
        InspectEnvelope(ctx, $"  {label}.rate");
        InspectEnvelope(ctx, $"  {label}.depth");
    }

    private static void InspectOscillators(InspectorContext ctx)
    {
        var idx = 0;
        while (idx < Constants.MaxOscillators && ctx.Buffer.Remaining > 0)
        {
            var marker = ctx.Buffer.Peek();
            if (marker == 0)
            {
                ctx.ReadByte("", "osc=end");
                break;
            }

            ctx.ReadSmart("", $"osc{idx}");
            ctx.ReadSmart("", $"pitch");
            ctx.ReadSmart("", $"delay");
            idx++;
        }
    }

    private static void InspectFilter(InspectorContext ctx)
    {
        if (ctx.Buffer.Remaining == 0)
        {
            ctx.PrintLine("; filter", "none (EOF)");
            return;
        }

        var packed = ctx.Buffer.Peek();
        var pair0 = packed >> 4;
        var pair1 = packed & 0x0F;

        if (packed == 0)
        {
            ctx.ReadByte("", "filt=none");
            return;
        }

        ctx.ReadByte("", $"filt: ch0={pair0}, ch1={pair1}");
        ctx.ReadUInt16("", "unity0");
        ctx.ReadUInt16("", "unity1");
        ctx.ReadByte("", $"modmask=0x{ctx.LastByte:X2}");

        // Poles
        for (var ch = 0; ch < 2; ch++)
        {
            var pairs = ch == 0 ? pair0 : pair1;
            if (pairs == 0) continue;

            for (var p = 0; p < pairs; p++)
            {
                ctx.ReadUInt16("", $"ch{ch}.pole{p}.freq");
                ctx.ReadUInt16("", $"mag");
            }
        }

        // Modulation
        if (ctx.LastByte != 0)
        {
            for (var ch = 0; ch < 2; ch++)
            {
                var pairs = ch == 0 ? pair0 : pair1;
                for (var p = 0; p < pairs; p++)
                {
                    if ((ctx.LastByte & (1 << (ch * 4 + p))) != 0)
                    {
                        ctx.ReadUInt16("", $"ch{ch}.pole{p}.freq_mod");
                        ctx.ReadUInt16("", $"mag_mod");
                    }
                }
            }
            InspectEnvelopeSegments(ctx);
        }
    }

    private static void InspectEnvelopeSegments(InspectorContext ctx)
    {
        ctx.ReadByte("", $"env_segs={ctx.LastByte}");
        for (var i = 0; i < ctx.LastByte; i++)
        {
            ctx.ReadUInt16("", $"seg{i}.dur");
            ctx.ReadUInt16("", $"seg{i}.peak");
        }
    }

    private static void InspectLoop(InspectorContext ctx)
    {
        if (ctx.Buffer.Remaining >= 4)
        {
            ctx.ReadUInt16("loop", "start");
            ctx.ReadUInt16("", "end");
        }
    }

    private static void PrintSummary(InspectorContext ctx)
    {
        Console.WriteLine($"; Parsed {ctx.Buffer.Position}/{ctx.Buffer.Data.Length} bytes ({ctx.Buffer.Position * 100.0 / ctx.Buffer.Data.Length:F1}%)");
        if (ctx.Buffer.Remaining > 0)
        {
            Console.WriteLine($"; Remaining: {ctx.Buffer.Remaining} bytes unparsed");
        }
    }

    private static void PrintError(InspectorContext ctx, Exception ex)
    {
        Console.WriteLine($"; ERROR at 0x{ctx.Buffer.Position:X4}: {ex.Message}");
    }

    private static string GetWaveformName(byte id) => id switch
    {
        0 => "off",
        1 => "square",
        2 => "sine",
        3 => "saw",
        4 => "noise",
        _ => $"?({id})"
    };

    private class InspectorContext(byte[] data)
    {
        public BinaryBuffer Buffer { get; } = new(data);
        public byte LastByte { get; private set; }

        public byte ReadByte(string mnemonic, string comment)
            => (byte)ReadAndPrint(1, mnemonic, comment, Buffer.ReadUInt8);

        public int ReadSmart(string mnemonic, string comment)
        {
            var startPos = Buffer.Position;
            var value = Buffer.ReadSmart();
            var len = Buffer.Position - startPos;
            var bytes = Buffer.Data.Skip(startPos).Take(len).ToArray();
            PrintLine(startPos, bytes, mnemonic, comment.Length > 0 ? $"{comment}={value}" : value.ToString());
            return value;
        }

        public int ReadInt32(string mnemonic, string comment)
            => ReadAndPrint(4, mnemonic, comment, Buffer.ReadInt32BE);

        public int ReadUInt16(string mnemonic, string comment)
            => ReadAndPrint(2, mnemonic, comment, Buffer.ReadUInt16BE);

        private int ReadAndPrint(int byteCount, string mnemonic, string comment, Func<int> readFunc)
        {
            var startPos = Buffer.Position;
            var value = readFunc();
            var bytes = Buffer.Data.Skip(startPos).Take(byteCount).ToArray();
            if (byteCount == 1) LastByte = bytes[0];
            PrintLine(startPos, bytes, mnemonic, comment.Length > 0 ? $"{comment}={value}" : value.ToString());
            return value;
        }

        public void PrintLine(string mnemonic, string comment)
        {
            PrintLine(Buffer.Position, [], mnemonic, comment);
        }

        private static void PrintLine(int pos, byte[] bytes, string mnemonic, string comment)
        {
            var hex = bytes.Length > 0 ? string.Join(" ", bytes.Select(b => b.ToString("X2"))) : "";
            if (hex.Length > 18) hex = hex[..15] + "...";
            var mnem = mnemonic.PadRight(10);
            var comma = comment.Length > 0 && mnemonic.Length > 0 ? ", " : "";
            Console.WriteLine($"{pos:X4}: {hex,-18} {mnem}{comma}{comment}");
        }
    }
}
