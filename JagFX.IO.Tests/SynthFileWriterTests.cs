using JagFX.Domain;
using JagFX.Domain.Models;
using JagFX.IO;
using Xunit;

namespace JagFX.IO.Tests;

public class SynthFileWriterTests
{
    #region Roundtrip Tests

    [Fact]
    public void CowDeath1VoiceRoundtripPreservesModelEquality()
    {
        var original = SynthFileReader.Read(TestFixtures.CowDeath);
        Assert.NotNull(original);
        var written = SynthFileWriter.Write(original);
        var reread = SynthFileReader.Read(written);
        Assert.NotNull(reread);
        Assert.Single(reread.ActiveVoices);
        Assert.Equal(original.Loop, reread.Loop);
    }

    [Fact]
    public void ProtectFromMagic2VoicesRoundtripPreservesModelEquality()
    {
        var original = SynthFileReader.Read(TestFixtures.ProtectFromMagic);
        Assert.NotNull(original);

        var written = SynthFileWriter.Write(original);
        var reread = SynthFileReader.Read(written);
        Assert.NotNull(reread);
        Assert.Equal(2, reread.ActiveVoices.Count);
        Assert.Equal(original.Loop, reread.Loop);
    }

    [Fact]
    public void IceCast2VoicesRoundtripPreservesModelEquality()
    {
        var original = SynthFileReader.Read(TestFixtures.IceCast);
        Assert.NotNull(original);
        var written = SynthFileWriter.Write(original);
        var reread = SynthFileReader.Read(written);
        Assert.NotNull(reread);
        Assert.Equal(2, reread.ActiveVoices.Count);
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
        Assert.Equal(100, reread.Loop.Begin);
    }

    #endregion
}
