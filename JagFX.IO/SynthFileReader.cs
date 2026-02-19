using JagFX.Domain;
using JagFX.Domain.Models;
using System.Collections.Immutable;

namespace JagFX.IO;

public static class SynthFileReader
{
    private const int MinWaveformId = 1;
    private const int MaxWaveformId = 4;
    private const int EnvelopeStartThreshold = 10_000_000;
    private const int SegCountOffset = 9;
    private const int MaxReasonableSegCount = 15;

    public static Patch Read(byte[] data)
    {
        var parser = new SynthParser(new BinaryBuffer(data));
        return parser.Parse();
    }

    public static Patch ReadFromPath(string path)
    {
        var data = File.ReadAllBytes(path);
        return Read(data);
    }

    private class SynthParser(BinaryBuffer buf)
    {
        private readonly BinaryBuffer _buf = buf;
        private readonly List<string> _warnings = [];

        public Patch Parse()
        {
            var voices = ReadVoices();

            var loopParams = _buf.Remaining >= 4
                ? new Loop(_buf.ReadUInt16BE(), _buf.ReadUInt16BE())
                : _buf.IsTruncated
                    ? new Loop(0, 0)
                    : new Loop(0, 0);

            if (_buf.IsTruncated)
                _warnings.Add($"File truncated at byte offset {_buf.Position}; loop parameters may be incomplete or invalid");

            return new Patch(voices, loopParams, [.. _warnings]);
        }

        private ImmutableList<Voice?> ReadVoices()
        {
            var voices = new List<Voice?>(Constants.MaxVoices);

            for (var i = 0; i < Constants.MaxVoices; i++)
            {
                if (_buf.Remaining > 0)
                {
                    var marker = _buf.Peek();
                    if (marker != 0)
                    {
                        var voice = ReadVoice();
                        RemoveLeadingZeroPadding();
                        voices.Add(voice);
                    }
                    else
                    {
                        _buf.Skip(1);
                        voices.Add(null);
                    }
                }
                else
                {
                    voices.Add(null);
                }
            }

            return [.. voices];
        }

        private Voice ReadVoice()
        {
            var pitchEnvelope = ReadEnvelope();
            var volumeEnvelope = ReadEnvelope();

            var (vibratoRate, vibratoDepth) = ReadOptionalEnvelopePair();
            var (tremoloRate, tremoloDepth) = ReadOptionalEnvelopePair();
            var (gateSilence, gateDuration) = ReadOptionalEnvelopePair();

            var oscillators = ReadOscillators();
            var feedbackDelay = _buf.ReadUSmart();
            var feedbackMix = _buf.ReadUSmart();
            var duration = _buf.ReadUInt16BE();
            var startTime = _buf.ReadUInt16BE();
            var filter = ReadFilter();

            return new Voice(
                FrequencyEnvelope: CreateEnvelopeWithDuration(pitchEnvelope, duration),
                AmplitudeEnvelope: CreateEnvelopeWithDuration(volumeEnvelope, duration),
                PitchLfo: vibratoRate != null && vibratoDepth != null ? new Lfo(vibratoRate, vibratoDepth) : null,
                AmplitudeLfo: tremoloRate != null && tremoloDepth != null ? new Lfo(tremoloRate, tremoloDepth) : null,
                GateSilence: gateSilence,
                GateDuration: gateDuration,
                Oscillators: oscillators,
                FeedbackDelay: new FeedbackDelay(feedbackDelay, feedbackMix),
                Duration: duration,
                StartTime: startTime,
                Filter: filter);
        }

        private Envelope ReadEnvelope()
        {
            var waveformId = _buf.ReadUInt8();
            var start = _buf.ReadInt32BE();
            var end = _buf.ReadInt32BE();
            var waveform = (waveformId >= 0 && waveformId <= 4) ? (Waveform)waveformId : Waveform.Off;
            var segmentLength = _buf.ReadUInt8();

            var segments = new List<Segment>(segmentLength);
            for (var i = 0; i < segmentLength; i++)
            {
                segments.Add(new Segment(
                    _buf.ReadUInt16BE(),
                    _buf.ReadUInt16BE()));
            }

            return new Envelope(waveform, start, end, [.. segments]);
        }

        private (Envelope? first, Envelope? second) ReadOptionalEnvelopePair()
        {
            var marker = _buf.Peek();
            if (marker != 0)
            {
                var env1 = ReadEnvelope();
                var env2 = ReadEnvelope();
                return (env1, env2);
            }

            _buf.Skip(1);
            return (null, null);
        }

        private ImmutableList<Oscillator> ReadOscillators()
        {
            var oscillators = new List<Oscillator>(Constants.MaxOscillators);

            while (oscillators.Count < Constants.MaxOscillators)
            {
                var volume = _buf.ReadUSmart();
                if (volume == 0) break;

                var pitchOffset = _buf.ReadSmart();
                var startDelay = _buf.ReadUSmart();

                oscillators.Add(new Oscillator(
                    new Percent(volume),
                    pitchOffset,
                    new Millis(startDelay)));
            }

            return [.. oscillators];
        }

        private Filter? ReadFilter()
        {
            if (_buf.Remaining == 0) return null;
            if (!DetectFilterPresent()) return null;

            var (pairCount0, pairCount1) = ReadFilterHeader();
            var unity0 = _buf.ReadUInt16BE();
            var unity1 = _buf.ReadUInt16BE();
            var modulationMask = _buf.ReadUInt8();

            var (frequencies, magnitudes) = ReadFilterCoefficients(
                pairCount0, pairCount1, modulationMask);

            Envelope? envelope = null;
            if (modulationMask != 0 || unity1 != unity0)
            {
                envelope = ReadEnvelopeSegments();
            }

            if (_buf.IsTruncated)
            {
                _warnings.Add("Filter truncated (discarding partial data)");
                return null;
            }

            return BuildFilter(
                pairCount0,
                pairCount1,
                unity0,
                unity1,
                frequencies,
                magnitudes,
                envelope);
        }

        private (int pairCount0, int pairCount1) ReadFilterHeader()
        {
            var packedPairs = _buf.ReadUInt8();
            return (packedPairs >> 4, packedPairs & 0xF);
        }

        private (int[,,] frequencies, int[,,] magnitudes) ReadFilterCoefficients(
            int pairCount0, int pairCount1, int modulationMask)
        {
            var frequencies = new int[2, 2, Constants.MaxFilterPairs];
            var magnitudes = new int[2, 2, Constants.MaxFilterPairs];

            ReadFilterPhase0Coefficients(frequencies, magnitudes, pairCount0, pairCount1);
            ReadFilterPhase1Coefficients(frequencies, magnitudes, pairCount0, pairCount1, modulationMask);

            return (frequencies, magnitudes);
        }

        private void ReadFilterPhase0Coefficients(
            int[,,] frequencies, int[,,] magnitudes, int pairCount0, int pairCount1)
        {
            for (var channel = 0; channel < 2; channel++)
            {
                var pairs = channel == 0 ? pairCount0 : pairCount1;
                for (var p = 0; p < pairs; p++)
                {
                    frequencies[channel, 0, p] = _buf.ReadUInt16BE();
                    magnitudes[channel, 0, p] = _buf.ReadUInt16BE();
                }
            }
        }

        private void ReadFilterPhase1Coefficients(
            int[,,] frequencies, int[,,] magnitudes, int pairCount0, int pairCount1, int modulationMask)
        {
            for (var channel = 0; channel < 2; channel++)
            {
                var pairs = channel == 0 ? pairCount0 : pairCount1;
                for (var p = 0; p < pairs; p++)
                {
                    if ((modulationMask & (1 << (channel * 4 + p))) != 0)
                    {
                        frequencies[channel, 1, p] = _buf.ReadUInt16BE();
                        magnitudes[channel, 1, p] = _buf.ReadUInt16BE();
                    }
                    else
                    {
                        CopyPhase0ToPhase1(frequencies, magnitudes, channel, p);
                    }
                }
            }
        }

        private Envelope ReadEnvelopeSegments()
        {
            var length = _buf.ReadUInt8();
            var segments = new List<Segment>(length);

            for (var i = 0; i < length; i++)
            {
                segments.Add(new Segment(
                    _buf.ReadUInt16BE(),
                    _buf.ReadUInt16BE()));
            }

            return new Envelope(Waveform.Off, 0, 0, [.. segments]);
        }

        private bool DetectFilterPresent()
        {
            var peeked = _buf.Peek();
            if (peeked == 0)
            {
                _buf.Skip(1);
                return false;
            }
            if (IsAmbiguousFilterByte(peeked) && LooksLikeEnvelope()) return false;
            return true;
        }

        private bool IsAmbiguousFilterByte(int b)
        {
            return b >= MinWaveformId && b <= MaxWaveformId &&
                   _buf.Remaining >= Constants.MaxVoices;
        }

        private bool LooksLikeEnvelope()
        {
            var b1 = _buf.PeekAt(1);
            var b2 = _buf.PeekAt(2);
            var b3 = _buf.PeekAt(3);
            var b4 = _buf.PeekAt(4);

            var possibleStart = (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
            if (possibleStart < -EnvelopeStartThreshold ||
                possibleStart > EnvelopeStartThreshold)
                return false;

            var possibleSegCount = _buf.PeekAt(SegCountOffset);
            return possibleSegCount <= MaxReasonableSegCount;
        }

        private void RemoveLeadingZeroPadding()
        {
            if (_buf.Remaining > 0 && _buf.Peek() == 0)
                _buf.Skip(1);
        }

        private static void CopyPhase0ToPhase1(int[,,] frequencies, int[,,] magnitudes, int channel, int p)
        {
            frequencies[channel, 1, p] = frequencies[channel, 0, p];
            magnitudes[channel, 1, p] = magnitudes[channel, 0, p];
        }

        private static Filter BuildFilter(
            int pairCount0,
            int pairCount1,
            int unity0,
            int unity1,
            int[,,] frequencies,
            int[,,] magnitudes,
            Envelope? envelope)
        {
            var pairCounts = ImmutableArray.Create(pairCount0, pairCount1);
            var unity = ImmutableArray.Create(unity0, unity1);

            var (pairPhase, pairMagnitude) = BuildFilterCoeffientArrays(
                frequencies, magnitudes, pairCount0, pairCount1);

            return new Filter(pairCounts, unity, pairPhase, pairMagnitude, envelope);
        }

        private static (ImmutableArray<ImmutableArray<ImmutableArray<int>>> pairPhase, ImmutableArray<ImmutableArray<ImmutableArray<int>>> pairMagnitude) BuildFilterCoeffientArrays(
            int[,,] frequencies, int[,,] magnitudes, int pairCount0, int pairCount1)
        {
            var freqArray = new ImmutableArray<int>[2][];
            var magArray = new ImmutableArray<int>[2][];

            for (var channel = 0; channel < 2; channel++)
            {
                var pairs = channel == 0 ? pairCount0 : pairCount1;
                freqArray[channel] = new ImmutableArray<int>[2];
                magArray[channel] = new ImmutableArray<int>[2];

                for (var phase = 0; phase < 2; phase++)
                {
                    freqArray[channel][phase] = BuildCoeffientArray(frequencies, channel, phase, pairs);
                    magArray[channel][phase] = BuildCoeffientArray(magnitudes, channel, phase, pairs);
                }
            }

            var pairPhase = ImmutableArray.Create(
                [freqArray[0][0], freqArray[0][1]],
                ImmutableArray.Create(freqArray[1][0], freqArray[1][1])
            );
            var pairMagnitude = ImmutableArray.Create(
                [magArray[0][0], magArray[0][1]],
                ImmutableArray.Create(magArray[1][0], magArray[1][1])
            );

            return (pairPhase, pairMagnitude);
        }

        private static ImmutableArray<int> BuildCoeffientArray(
            int[,,] coefficients, int channel, int phase, int pairs)
        {
            var builder = ImmutableArray.CreateBuilder<int>(pairs);
            for (var p = 0; p < pairs; p++)
            {
                builder.Add(coefficients[channel, phase, p]);
            }
            return builder.ToImmutable();
        }

        private static Envelope CreateEnvelopeWithDuration(Envelope env, int duration)
        {
            if (env.Segments.Count == 0 && env.Start != env.End)
            {
                return new Envelope(
                    env.Waveform,
                    env.Start,
                    env.End,
                    [new Segment(duration, env.End)]);
            }
            return env;
        }
    }
}
