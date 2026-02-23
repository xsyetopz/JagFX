using JagFX.Core.Contracts;
using JagFX.Domain.Models;
using JagFX.Synthesis.Data;

namespace JagFX.Application.Services;

/// <summary>
/// High-level service for audio synthesis operations.
/// Orchestrates domain, synthesis, and I/O concerns.
/// </summary>
public interface ISynthesisService
{
    /// <summary>
    /// Converts a patch to audio samples with specified loop count.
    /// </summary>
    /// <param name="patch">The patch to synthesize.</param>
    /// <param name="loopCount">Number of times to loop the audio.</param>
    /// <returns>A buffer containing the synthesized audio.</returns>
    AudioBuffer ConvertPatchToAudio(Patch patch, int loopCount = 1);
    
    /// <summary>
    /// Loads a patch from the specified file path.
    /// </summary>
    /// <param name="path">The file path to load from.</param>
    /// <returns>The loaded patch.</returns>
    Patch LoadPatch(string path);
    
    /// <summary>
    /// Saves a patch to the specified file path.
    /// </summary>
    /// <param name="patch">The patch to save.</param>
    /// <param name="path">The file path to save to.</param>
    void SavePatch(Patch patch, string path);
    
    /// <summary>
    /// Exports audio samples to a WAV file.
    /// </summary>
    /// <param name="buffer">The audio buffer to export.</param>
    /// <param name="path">The file path to export to.</param>
    /// <param name="bitsPerSample">Bits per sample (8 or 16).</param>
    void ExportToWav(AudioBuffer buffer, string path, int bitsPerSample = 8);
}
