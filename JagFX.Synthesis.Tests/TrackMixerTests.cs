using JagFX.Domain;
using JagFX.Domain.Models;
using JagFX.IO;
using Xunit;

namespace JagFX.Synthesis.Tests;

public class TrackMixerTests
{
    #region Single Voice Tests

    [Fact]
    public void SynthesizesCowDeath1Voice()
    {
        var file = SynthFileReader.Read(TestFixtures.CowDeath);
        Assert.NotNull(file);
        var audio = TrackMixer.Synthesize(file, 1);
        Assert.True(audio.Length > 0);
        Assert.Equal(Constants.SampleRate, audio.SampleRate);
        Assert.Equal(19889 - 44, audio.Length);
    }

    #endregion

    #region Multi-Voice Tests

    [Fact]
    public void SynthesizesProtectFromMagic2Voices()
    {
        var file = SynthFileReader.Read(TestFixtures.ProtectFromMagic);
        Assert.NotNull(file);
        var audio = TrackMixer.Synthesize(file, 1);
        Assert.True(audio.Length > 0);
        Assert.Equal(Constants.SampleRate, audio.SampleRate);
        Assert.Equal(33119 - 44, audio.Length);
    }

    [Fact]
    public void SynthesizesIceCast2Voices()
    {
        var file = SynthFileReader.Read(TestFixtures.IceCast);
        Assert.NotNull(file);
        var audio = TrackMixer.Synthesize(file, 1);
        Assert.True(audio.Length > 0);
        Assert.Equal(Constants.SampleRate, audio.SampleRate);
    }

    #endregion

    #region Edge Cases

    [Fact]
    public void EmptyFileProducesEmptyBuffer()
    {
        var emptyFile = new Patch(
            voices: [],
            loop: new Loop(0, 0)
        );
        var audio = TrackMixer.Synthesize(emptyFile, 1);
        Assert.Equal(0, audio.Length);
    }

    #endregion
}
