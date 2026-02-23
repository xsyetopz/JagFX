namespace JagFX.Core.Contracts;

/// <summary>
/// Contract for reading audio data from files.
/// </summary>
/// <typeparam name="T">The type of data to read.</typeparam>
public interface IFileReader<T>
{
    /// <summary>
    /// Reads data from the specified file path.
    /// </summary>
    /// <param name="path">The file path to read from.</param>
    /// <returns>The parsed data.</returns>
    T Read(string path);
}
