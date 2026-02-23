using JagFX.Core.Constants;
using JagFX.Domain;
using JagFX.Domain.Models;
using System.Collections.Immutable;

namespace JagFX.Io;

public static class SynthFileWriter
{
    public static byte[] Write(Patch patch)
    {
        var buffer = new BinaryBuffer(4096);

        WriteVoices(buffer, patch.Voices);
        WriteLoop(buffer, patch.Loop);

        return buffer.Data[..buffer.Position];
    }

    public static void WriteToPath(Patch patch, string path)
    {
        var data = Write(patch);
        File.WriteAllBytes(path, data);
    }

    private static void WriteVoices(BinaryBuffer buffer, ImmutableList<Voice?> voices)
    {
        for (var i = 0; i < AudioConstants.MaxVoices; i++)
        {
            var voice = i < voices.Count ? voices[i] : null;
            if (voice == null)
            {
                buffer.WriteUInt8(0);
            }
            else
            {
                WriteVoice(buffer, voice);
            }
        }
    }

    private static void WriteVoice(BinaryBuffer buffer, Voice voice)
    {
        WriteEnvelope(buffer, voice.FrequencyEnvelope);
        WriteEnvelope(buffer, voice.AmplitudeEnvelope);

        WriteOptionalEnvelopePair(buffer, voice.PitchLfo);
        WriteOptionalEnvelopePair(buffer, voice.AmplitudeLfo);
        WriteOptionalEnvelopePair(buffer, voice.GateSilenceEnvelope, voice.GateDurationEnvelope);

        WritePartials(buffer, voice.Partials);

        buffer.WriteUSmart((ushort)voice.Echo.DelayMilliseconds);
        buffer.WriteUSmart((ushort)voice.Echo.MixPercent);

        buffer.WriteUInt16BE(voice.DurationSamples);
        buffer.WriteUInt16BE(voice.StartSample);

        if (voice.Filter != null)
        {
            WriteFilter(buffer, voice.Filter);
        }
    }

    private static void WriteEnvelope(BinaryBuffer buffer, Envelope envelope)
    {
        buffer.WriteUInt8((int)envelope.Waveform);
        buffer.WriteInt32BE(envelope.StartSample);
        buffer.WriteInt32BE(envelope.EndSample);
        buffer.WriteUInt8(envelope.Segments.Count);

        foreach (var segment in envelope.Segments)
        {
            buffer.WriteUInt16BE(segment.DurationSamples);
            buffer.WriteUInt16BE(segment.PeakLevel);
        }
    }

    private static void WriteOptionalEnvelopePair(BinaryBuffer buffer, LowFrequencyOscillator? lfo)
    {
        if (lfo == null)
        {
            buffer.WriteUInt8(0);
        }
        else
        {
            WriteEnvelope(buffer, lfo.FrequencyRate);
            WriteEnvelope(buffer, lfo.ModulationDepth);
        }
    }

    private static void WriteOptionalEnvelopePair(BinaryBuffer buffer, Envelope? env1, Envelope? env2)
    {
        if (env1 == null || env2 == null)
        {
            buffer.WriteUInt8(0);
        }
        else
        {
            WriteEnvelope(buffer, env1);
            WriteEnvelope(buffer, env2);
        }
    }

    private static void WritePartials(BinaryBuffer buffer, ImmutableList<Partial> partials)
    {
        foreach (var partial in partials)
        {
            buffer.WriteUSmart((ushort)partial.Amplitude.Value);
            buffer.WriteSmart((short)partial.PitchOffsetSemitones);
            buffer.WriteUSmart((ushort)partial.Delay.Value);
        }
        buffer.WriteUSmart(0);
    }

    private static void WriteFilter(BinaryBuffer buffer, Filter filter)
    {
        var poleCounts = filter.PoleCounts;
        var unityGain = filter.UnityGain;

        var packedPoles = (poleCounts[0] << 4) | poleCounts[1];
        buffer.WriteUInt8(packedPoles);

        buffer.WriteUInt16BE(unityGain[0]);
        buffer.WriteUInt16BE(unityGain[1]);

        var modulationMask = CalculateModulationMask(filter);
        buffer.WriteUInt8(modulationMask);

        WriteFilterCoefficients(buffer, filter, 0);
        WriteFilterCoefficients(buffer, filter, 1);

        if (filter.CutoffEnvelope != null)
        {
            WriteFilterEnvelope(buffer, filter.CutoffEnvelope);
        }
    }

    private static void WriteFilterCoefficients(BinaryBuffer buffer, Filter filter, int phase)
    {
        var poleCounts = filter.PoleCounts;
        var polePhase = filter.PolePhase;
        var poleMagnitude = filter.PoleMagnitude;

        for (var channel = 0; channel < 2; channel++)
        {
            var poles = poleCounts[channel];
            for (var p = 0; p < poles; p++)
            {
                buffer.WriteUInt16BE(polePhase[channel][phase][p]);
                buffer.WriteUInt16BE(poleMagnitude[channel][phase][p]);
            }
        }
    }

    private static void WriteFilterEnvelope(BinaryBuffer buffer, Envelope envelope)
    {
        buffer.WriteUInt8(envelope.Segments.Count);

        foreach (var segment in envelope.Segments)
        {
            buffer.WriteUInt16BE(segment.DurationSamples);
            buffer.WriteUInt16BE(segment.PeakLevel);
        }
    }

    private static void WriteLoop(BinaryBuffer buffer, Loop loop)
    {
        buffer.WriteUInt16BE(loop.BeginSample);
        buffer.WriteUInt16BE(loop.EndSample);
    }

    private static int CalculateModulationMask(Filter filter)
    {
        var mask = 0;
        var poleCounts = filter.PoleCounts;
        var polePhase = filter.PolePhase;

        for (var channel = 0; channel < 2; channel++)
        {
            var poles = poleCounts[channel];
            for (var pole = 0; pole < poles; pole++)
            {
                var phase0 = polePhase[channel][0][pole];
                var phase1 = polePhase[channel][1][pole];
                if (phase0 != phase1)
                {
                    mask |= 1 << (channel * 4 + pole);
                }
            }
        }

        return mask;
    }
}
