using JagFX.Domain;
using JagFX.Domain.Models;
using Xunit;

namespace JagFX.IO.Tests;

public class SynthFileReaderTests
{
    private static readonly int[] expected = [0, 1];

    #region Voice Count Tests

    [Fact]
    public void ReadsCowDeath1VoiceCorrectly()
    {
        var result = SynthFileReader.Read(TestFixtures.CowDeath);
        Assert.NotNull(result);
        Assert.Single(result.ActiveVoices);
        Assert.Equal(0, result.Loop.Begin);
        Assert.Equal(0, result.Loop.End);
    }

    [Fact]
    public void ReadsProtectFromMagic2VoicesCorrectly()
    {
        var result = SynthFileReader.Read(TestFixtures.ProtectFromMagic);
        Assert.NotNull(result);
        Assert.Equal(2, result.ActiveVoices.Count);
        var voiceIndices = result.ActiveVoices.Select(v => v.Index).ToList();
        Assert.Equal(expected, voiceIndices);
        Assert.Equal(0, result.Loop.Begin);
        Assert.Equal(0, result.Loop.End);
    }

    [Fact]
    public void ReadsIceCast2VoicesCorrectly()
    {
        var result = SynthFileReader.Read(TestFixtures.IceCast);
        Assert.NotNull(result);
        Assert.Equal(2, result.ActiveVoices.Count);
        Assert.Equal(0, result.Loop.Begin);
        Assert.Equal(0, result.Loop.End);
    }

    #endregion

    #region Envelope Tests

    [Fact]
    public void ParsesEnvelopeFormsCorrectly()
    {
        var cow = SynthFileReader.Read(TestFixtures.CowDeath);
        Assert.NotNull(cow);
        var (_, cowVoice) = cow.ActiveVoices.First();
        Assert.Equal(Waveform.Sine, cowVoice.FrequencyEnvelope.Waveform);

        var protect = SynthFileReader.Read(TestFixtures.ProtectFromMagic);
        Assert.NotNull(protect);
        var (_, protectVoice1) = protect.ActiveVoices.First();
        Assert.Equal(Waveform.Square, protectVoice1.FrequencyEnvelope.Waveform);
    }

    #endregion

    #region Oscillator Tests

    [Fact]
    public void ParsesPartialsCorrectly()
    {
        var result = SynthFileReader.Read(TestFixtures.CowDeath);
        Assert.NotNull(result);
        var (_, voice) = result.ActiveVoices.First();
        Assert.Equal(2, voice.Oscillators.Count);
        Assert.Equal(100, voice.Oscillators[0].Amplitude.Value);
    }

    #endregion
}
