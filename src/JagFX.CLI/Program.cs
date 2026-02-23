namespace JagFX.Cli;

public static class Program
{
    public static Task<int> Main(string[] args)
    {
        return JagFxCli.RunAsync(args);
    }
}
