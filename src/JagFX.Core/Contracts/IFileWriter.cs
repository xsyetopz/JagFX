namespace JagFX.Core.Contracts;

/// <summary>
/// Contract for writing audio data to files.
/// </summary>
/// <typeparam name="T">The type of data to write.</typeparam>
public interface IFileWriter<T>
{
    /// <summary>
    /// Writes data to the specified file path.
    /// </summary>
    /// <param name="data">The data to write.</param>
    /// <param name="path">The file path to write to.</param>
    void Write(T data, string path);
}
