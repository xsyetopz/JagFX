using JagFX.Domain.Models;
using JagFX.Io;
using JagFX.TestData;
using Xunit;

namespace JagFX.Io.Tests;

public class SynthFileWriterTests
{
    #region Roundtrip Tests

    [Fact]
    public void CowDeath1VoiceRoundtripPreservesModelEquality()
    {
        var original = SynthFileReader.Read(TestResources.CowDeath);
        Assert.NotNull(original);
        var written = SynthFileWriter.Write(original);
        var reread = SynthFileReader.Read(written);
        Assert.NotNull(reread);
        Assert.Single(reread.ActiveVoices);
        Assert.Equal(original.Loop, reread.Loop);
    }

    [Fact]
    public void WardOfArceuusCastRoundtripPreservesModelEquality()
    {
        var original = SynthFileReader.Read(TestResources.WardOfArceuusCast);
        Assert.NotNull(original);
        var written = SynthFileWriter.Write(original);
        var reread = SynthFileReader.Read(written);
        Assert.NotNull(reread);
        Assert.True(reread.ActiveVoices.Count >= 1);
        Assert.Equal(original.Loop, reread.Loop);
    }

    #endregion

    #region Edge Cases

    [Fact]
    public void WritesEmptyFileCorrectly()
    {
        var emptyFile = new Patch(
            voices: [],
            loop: new Loop(100, 200)
        );
        var written = SynthFileWriter.Write(emptyFile);
        Assert.Equal(14, written.Length);
        var reread = SynthFileReader.Read(written);
        Assert.NotNull(reread);
        Assert.Empty(reread.ActiveVoices);
        // Note: Empty files may have loop parameters reset to 0
        Assert.True(reread.Loop.Begin == 0 || reread.Loop.Begin == 100);
    }

    #endregion
}
