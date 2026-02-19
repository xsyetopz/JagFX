namespace JagFX.CLI;

public static class Program
{
    public static Task<int> Main(string[] args)
        => JagFXCli.RunAsync(args);
}
