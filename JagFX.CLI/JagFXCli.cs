using JagFX.CLI.Commands;
using System.CommandLine;

namespace JagFX.CLI;

/// <summary>
/// Entry point for the JagFX CLI application.
/// </summary>
public static class JagFXCli
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
        ConvertCommandHandler.Configure(rootCommand);

        rootCommand.Add(new InspectCommand());
        return rootCommand;
    }
}
