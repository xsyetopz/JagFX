using JagFX.Cli.Commands;
using System.CommandLine;

namespace JagFX.Cli;

/// <summary>
/// Entry point for the JagFX CLI application.
/// </summary>
public static class JagFxCli
{
    /// <summary>
    /// Runs the CLI with the provided arguments.
    /// </summary>
    public static Task<int> RunAsync(string[] args)
    {
        var rootCommand = BuildRootCommand();
        var parseResult = rootCommand.Parse(args);
        return parseResult.InvokeAsync(new InvocationConfiguration());
    }

    private static RootCommand BuildRootCommand()
    {
        var rootCommand = new RootCommand("JagFX CLI");
        ConvertCommand.Configure(rootCommand);

        rootCommand.Add(new InspectCommand());
        return rootCommand;
    }
}
