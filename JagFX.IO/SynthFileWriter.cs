using JagFX.Domain;
using JagFX.Domain.Models;
using System.Collections.Immutable;

namespace JagFX.IO;

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

    private static void WriteVoices(BinaryBuffer buf, ImmutableList<Voice?> voices)
    {
        for (var i = 0; i < Constants.MaxVoices; i++)
        {
            var voice = i < voices.Count ? voices[i] : null;
            if (voice == null)
            {
                buf.WriteUInt8(0);
            }
            else
            {
                WriteVoice(buf, voice);
            }
        }
    }

    private static void WriteVoice(BinaryBuffer buf, Voice voice)
    {
        WriteEnvelope(buf, voice.FrequencyEnvelope);
        WriteEnvelope(buf, voice.AmplitudeEnvelope);

        WriteOptionalEnvelopePair(buf, voice.PitchLfo);
        WriteOptionalEnvelopePair(buf, voice.AmplitudeLfo);
        WriteOptionalEnvelopePair(buf, voice.GateSilence, voice.GateDuration);

        WriteOscillators(buf, voice.Oscillators);

        buf.WriteUSmart((ushort)voice.FeedbackDelay.Delay);
        buf.WriteUSmart((ushort)voice.FeedbackDelay.Mix);

        buf.WriteUInt16BE(voice.Duration);
        buf.WriteUInt16BE(voice.StartTime);

        if (voice.Filter != null)
        {
            WriteFilter(buf, voice.Filter);
        }
    }

    private static void WriteEnvelope(BinaryBuffer buf, Envelope envelope)
    {
        buf.WriteUInt8((int)envelope.Waveform);
        buf.WriteInt32BE(envelope.Start);
        buf.WriteInt32BE(envelope.End);
        buf.WriteUInt8(envelope.Segments.Count);

        foreach (var segment in envelope.Segments)
        {
            buf.WriteUInt16BE(segment.Duration);
            buf.WriteUInt16BE(segment.Peak);
        }
    }

    private static void WriteOptionalEnvelopePair(BinaryBuffer buf, Lfo? lfo)
    {
        if (lfo == null)
        {
            buf.WriteUInt8(0);
        }
        else
        {
            WriteEnvelope(buf, lfo.Rate);
            WriteEnvelope(buf, lfo.Depth);
        }
    }

    private static void WriteOptionalEnvelopePair(BinaryBuffer buf, Envelope? env1, Envelope? env2)
    {
        if (env1 == null || env2 == null)
        {
            buf.WriteUInt8(0);
        }
        else
        {
            WriteEnvelope(buf, env1);
            WriteEnvelope(buf, env2);
        }
    }

    private static void WriteOscillators(BinaryBuffer buf, ImmutableList<Oscillator> oscillators)
    {
        foreach (var osc in oscillators)
        {
            buf.WriteUSmart((ushort)osc.Amplitude.Value);
            buf.WriteSmart((short)osc.PitchOffset);
            buf.WriteUSmart((ushort)osc.Delay.Value);
        }

        // Terminator
        buf.WriteUSmart(0);
    }

    private static void WriteFilter(BinaryBuffer buf, Filter filter)
    {
        var pairCounts = filter.PairCounts;
        var unity = filter.Unity;

        var packedPairs = (pairCounts[0] << 4) | pairCounts[1];
        buf.WriteUInt8(packedPairs);

        buf.WriteUInt16BE(unity[0]);
        buf.WriteUInt16BE(unity[1]);

        var modulationMask = CalculateModulationMask(filter);
        buf.WriteUInt8(modulationMask);

        WriteFilterCoefficients(buf, filter, 0);
        WriteFilterCoefficients(buf, filter, 1);

        if (filter.Envelope != null)
        {
            WriteFilterEnvelope(buf, filter.Envelope);
        }
    }

    private static void WriteFilterCoefficients(BinaryBuffer buf, Filter filter, int phase)
    {
        var pairCounts = filter.PairCounts;
        var pairPhase = filter.PairPhase;
        var pairMagnitude = filter.PairMagnitude;

        for (var channel = 0; channel < 2; channel++)
        {
            var pairs = pairCounts[channel];
            for (var p = 0; p < pairs; p++)
            {
                buf.WriteUInt16BE(pairPhase[channel][phase][p]);
                buf.WriteUInt16BE(pairMagnitude[channel][phase][p]);
            }
        }
    }

    private static void WriteFilterEnvelope(BinaryBuffer buf, Envelope envelope)
    {
        buf.WriteUInt8(envelope.Segments.Count);

        foreach (var segment in envelope.Segments)
        {
            buf.WriteUInt16BE(segment.Duration);
            buf.WriteUInt16BE(segment.Peak);
        }
    }

    private static void WriteLoop(BinaryBuffer buf, Loop loop)
    {
        buf.WriteUInt16BE(loop.Begin);
        buf.WriteUInt16BE(loop.End);
    }

    private static int CalculateModulationMask(Filter filter)
    {
        var mask = 0;
        var pairCounts = filter.PairCounts;
        var pairPhase = filter.PairPhase;

        for (var channel = 0; channel < 2; channel++)
        {
            var pairs = pairCounts[channel];
            for (var p = 0; p < pairs; p++)
            {
                var phase0 = pairPhase[channel][0][p];
                var phase1 = pairPhase[channel][1][p];
                if (phase0 != phase1)
                {
                    mask |= 1 << (channel * 4 + p);
                }
            }
        }

        return mask;
    }
}
