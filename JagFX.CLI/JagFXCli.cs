using JagFX.IO;
using JagFX.Synthesis;

namespace JagFX.CLI;

public static class JagFXCli
{
    private static readonly string[] SupportedInputFormats = [".synth"];
    private static readonly string[] SupportedOutputFormats = [".wav"];

    public static void Run(string[] args)
    {
        if (args.Length == 0 || IsHelpRequested(args[0]))
        {
            PrintHelp();
            return;
        }

        if (args[0] == "inspect")
        {
            HandleInspect(args);
            return;
        }
        HandleConversion(args);
    }

    private static void HandleInspect(string[] args)
    {
        if (args.Length < 2)
        {
            Console.Error.WriteLine("Error: Missing file path for inspect command");
            PrintInspectHelp();
            Exit(1);
            return;
        }

        SynthInspector.Inspect(args[1]);
        Exit(0);
    }

    private static void HandleConversion(string[] args)
    {
        if (args.Length < 2)
        {
            Console.Error.WriteLine("Error: Insufficient arguments");
            Console.Error.WriteLine("Usage: jagfx-cli <input> <output> [loopCount]");
            Exit(1);
            return;
        }

        var inputPath = args[0];
        var outputPath = args[1];
        var loopCount = args.Length > 2 ? int.Parse(args[2]) : 1;

        if (!ValidateInputFile(inputPath))
            return;

        var inputExt = GetExtension(inputPath);
        var outputExt = GetExtension(outputPath);

        if (!ValidateFormats(inputExt, outputExt))
            return;

        ProcessConversion(inputPath, outputPath, inputExt, outputExt, loopCount);
    }

    private static bool ValidateInputFile(string path)
    {
        if (!File.Exists(path))
        {
            Console.Error.WriteLine($"Error: Input file not found: {path}");
            Exit(1);
            return false;
        }
        return true;
    }

    private static bool ValidateFormats(string inputExt, string outputExt)
    {
        if (inputExt == ".tone")
        {
            Console.WriteLine("TBD: Mod Surma video showcased .tone files in a directory. Not sure what their structure is...");
            Exit(0);
            return false;
        }

        if (!SupportedInputFormats.Contains(inputExt))
        {
            Console.Error.WriteLine($"Unsupported input format: {inputExt}");
            Console.Error.WriteLine($"Supported formats: {string.Join(", ", SupportedInputFormats)}");
            Exit(1);
            return false;
        }
        if (!SupportedOutputFormats.Contains(outputExt))
        {
            Console.Error.WriteLine($"Unsupported output format: {outputExt}");
            Console.Error.WriteLine($"Supported formats: {string.Join(", ", SupportedOutputFormats)}");
            Exit(1);
            return false;
        }

        return true;
    }

    private static void ProcessConversion(string inputPath, string outputPath, string inputExt, string outputExt, int loopCount)
    {
        try
        {
            switch (inputExt, outputExt)
            {
                case (".synth", ".wav"):
                    var patch = SynthFileReader.ReadFromPath(inputPath);
                    var audio = TrackMixer.Synthesize(patch, loopCount);
                    WaveFileWriter.WriteToPath(audio.ToUBytes(), outputPath);
                    Console.WriteLine($"Successfully wrote {outputPath}");
                    Exit(0);
                    break;
                default:
                    Console.Error.WriteLine("Error: Unexpected format combination");
                    Exit(1);
                    break;
            }
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine($"Error: Failed to process file: {ex.Message}");
            Exit(1);
        }
    }

    private static string GetExtension(string path) => Path.GetExtension(path).ToLowerInvariant();

    private static bool IsHelpRequested(string arg) => arg == "--help" || arg == "-h";

    private static void PrintHelp()
    {
        Console.WriteLine("JagFX CLI\n");
        Console.WriteLine("Usage:");
        Console.WriteLine("  jagfx-cli <input> <output> [loopCount]          Convert between formats");
        Console.WriteLine("  jagfx-cli inspect <file.synth>                  Inspect synth file structure");
        Console.WriteLine("  jagfx-cli --help                                Show this help message\n");
        Console.WriteLine("Supported formats:");
        Console.WriteLine("  .synth → .wav    Convert synth to WAV audio");
        Console.WriteLine("  .tone            Show info about .tone files\n");
        Console.WriteLine("Examples:");
        Console.WriteLine("  jagfx-cli song.synth song.wav                  Convert with default 1 loop");
        Console.WriteLine("  jagfx-cli song.synth song.wav 3                Convert with 3 loops");
        Console.WriteLine("  jagfx-cli inspect song.synth                   Inspect binary structure");
    }

    private static void PrintInspectHelp()
    {
        Console.WriteLine("Usage: jagfx-cli inspect <file.synth>\n");
        Console.WriteLine("Inspects the binary structure of a .synth file for debugging.");
        Console.WriteLine("Use this to compare parsing between Scala and C# implementations.");
    }

    private static void Exit(int code) => Environment.Exit(code);
}
