using JagFX.IO;
using JagFX.Synthesis;
using System.CommandLine;

namespace JagFX.CLI;

public static class JagFXCli
{
    public static Task<int> RunAsync(string[] args)
    {
        var rootCommand = CommandHandler.BuildRootCommand();
        var parseResult = rootCommand.Parse(args);
        return parseResult.InvokeAsync(new InvocationConfiguration());
    }
}

internal static class CommandHandler
{
    private static readonly string[] SupportedInputFormats = [".synth"];
    private static readonly string[] SupportedOutputFormats = [".wav"];

    private static readonly string[] InputFlags = ["-i", "--input"];
    private static readonly string[] OutputFlags = ["-o", "--output"];
    private static readonly string[] LoopsFlags = ["-l", "--loops"];

    public static RootCommand BuildRootCommand()
    {
        var inputArgument = new Argument<string?>("input")
        {
            Description = "Input .synth file path (positional mode).",
            Arity = ArgumentArity.ZeroOrOne
        };
        var outputArgument = new Argument<string?>("output")
        {
            Description = "Output .wav file path (positional mode).",
            Arity = ArgumentArity.ZeroOrOne
        };
        var loopsArgument = new Argument<int?>("loopCount")
        {
            Description = "Loop count (positional mode).",
            Arity = ArgumentArity.ZeroOrOne
        };

        var inputOption = new Option<string?>("input", InputFlags)
        {
            Description = "Input .synth file path (flag mode)."
        };
        var outputOption = new Option<string?>("output", OutputFlags)
        {
            Description = "Output .wav file path (flag mode)."
        };
        var loopsOption = new Option<int?>("loops", LoopsFlags)
        {
            Description = "Loop count (flag mode)."
        };

        var rootCommand = new RootCommand("JagFX CLI");
        rootCommand.Aliases.Clear();
        rootCommand.Add(inputArgument);
        rootCommand.Add(outputArgument);
        rootCommand.Add(loopsArgument);
        rootCommand.Add(inputOption);
        rootCommand.Add(outputOption);
        rootCommand.Add(loopsOption);

        rootCommand.SetAction(parseResult =>
            HandleConvertCommand(parseResult, inputArgument, outputArgument, loopsArgument, inputOption, outputOption, loopsOption));

        rootCommand.Add(BuildInspectCommand());

        return rootCommand;
    }

    private static int HandleConvertCommand(ParseResult parseResult, Argument<string?> inputArg, Argument<string?> outputArg, Argument<int?> loopsArg,
        Option<string?> inputOpt, Option<string?> outputOpt, Option<int?> loopsOpt)
    {
        var usesFlags = parseResult.GetResult(inputOpt) != null || parseResult.GetResult(outputOpt) != null || parseResult.GetResult(loopsOpt) != null;
        var (inputPath, outputPath, loopCount, hasPositional) = ResolveArguments(parseResult, inputArg, outputArg, loopsArg, inputOpt, outputOpt, loopsOpt, usesFlags);

        if (usesFlags && hasPositional)
        {
            Console.Error.WriteLine("Error: Positional arguments cannot be mixed with flags.");
            Console.Error.WriteLine("Use either positional form or flags, but not both.");
            return 1;
        }

        if (string.IsNullOrWhiteSpace(inputPath) || string.IsNullOrWhiteSpace(outputPath))
        {
            Console.Error.WriteLine("Error: Missing required input or output argument.");
            return 1;
        }

        if (!ValidateInputFile(inputPath) || !ValidateFormats(GetExtension(inputPath), GetExtension(outputPath)))
            return 1;

        return ProcessConversion(inputPath, outputPath, GetExtension(inputPath), GetExtension(outputPath), loopCount);
    }

    private static (string?, string?, int, bool) ResolveArguments(ParseResult parseResult, Argument<string?> inputArg, Argument<string?> outputArg, Argument<int?> loopsArg,
        Option<string?> inputOpt, Option<string?> outputOpt, Option<int?> loopsOpt, bool usesFlags)
    {
        var positionalInput = parseResult.GetValue(inputArg);
        var positionalOutput = parseResult.GetValue(outputArg);
        var positionalLoops = parseResult.GetValue(loopsArg);

        var hasPositional = positionalInput != null || positionalOutput != null || positionalLoops != null;
        var inputPath = usesFlags ? parseResult.GetValue(inputOpt) : positionalInput;
        var outputPath = usesFlags ? parseResult.GetValue(outputOpt) : positionalOutput;
        var loopCount = usesFlags ? (parseResult.GetValue(loopsOpt) ?? 1) : (positionalLoops ?? 1);

        return (inputPath, outputPath, loopCount, hasPositional);
    }

    private static Command BuildInspectCommand()
    {
        var inspectFileArgument = new Argument<string>("file") { Description = "Path to .synth file." };
        var inspectCommand = new Command("inspect", "Inspect synth file structure") { inspectFileArgument };
        inspectCommand.SetAction(parseResult => HandleInspectCommand(parseResult, inspectFileArgument));
        return inspectCommand;
    }

    private static int HandleInspectCommand(ParseResult parseResult, Argument<string> fileArg)
    {
        var file = parseResult.GetValue(fileArg);
        if (string.IsNullOrWhiteSpace(file) || !ValidateInputFile(file))
            return 1;

        SynthInspector.Inspect(file);
        return 0;
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

    private static int ProcessConversion(string inputPath, string outputPath, string inputExt, string outputExt, int loopCount)
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
                    return 0;
                default:
                    Console.Error.WriteLine("Error: Unexpected format combination");
                    return 1;
            }
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine($"Error: Failed to process file: {ex.Message}");
            return 1;
        }
    }

    private static string GetExtension(string path) => Path.GetExtension(path).ToLowerInvariant();

    private static void Exit(int code) => Environment.Exit(code);
}
