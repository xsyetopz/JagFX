using JagFX.Core.Constants;
using JagFX.Domain;
using JagFX.Domain.Models;
using JagFX.Domain.Utilities;
using JagFX.Synthesis.Data;

namespace JagFX.Synthesis.Processing;

public static class AudioFilter
{
    private const int MaxCoefs = 8;
    private const int ChunkSize = 128;

    public static void Apply(int[] buffer, Filter filter, EnvelopeGenerator? envelopeEval, int sampleCount)
    {
        if (filter.PairCounts[0] == 0 && filter.PairCounts[1] == 0)
        {
            return;
        }

        envelopeEval?.Reset();

        var tempInput = AudioBufferPool.Acquire(sampleCount);
        var inputSpan = tempInput.AsSpan();
        var bufferSpan = buffer.AsSpan();
        bufferSpan.CopyTo(inputSpan);

        var state = new FilterState(filter);
        var envelopeValue = envelopeEval?.Evaluate(sampleCount) ?? AudioConstants.FixedPoint.Scale;
        var envelopeFactor = envelopeValue / (float)AudioConstants.FixedPoint.Scale;

        var count0 = state.ComputeCoefs(0, envelopeFactor);
        var inverseA0 = state.InverseA0;
        var count1 = state.ComputeCoefs(1, envelopeFactor);
        state.InverseA0 = inverseA0;

        if (sampleCount < count0 + count1)
        {
            AudioBufferPool.Release(tempInput);
            return;
        }

        ProcessInitialChunk(inputSpan, bufferSpan, state, envelopeEval, count0, count1, sampleCount);
        ProcessMainChunks(inputSpan, bufferSpan, state, envelopeEval, sampleCount, ref count0, ref count1);
        ProcessFinalSamples(inputSpan, bufferSpan, state, envelopeEval, sampleCount, count0, count1);

        AudioBufferPool.Release(tempInput);
    }

    private static void ProcessInitialChunk(Span<int> inputSpan, Span<int> bufferSpan, FilterState state, EnvelopeGenerator? envelopeEval, int count0, int count1, int sampleCount)
    {
        ProcessSampleRange(inputSpan, bufferSpan, state, envelopeEval, 0, count1, count0, count1, sampleCount);
    }

    private static void ProcessMainChunks(Span<int> inputSpan, Span<int> bufferSpan, FilterState state, EnvelopeGenerator? envelopeEval, int sampleCount, ref int count0, ref int count1)
    {
        int sampleIndex = count1;

        while (sampleIndex < sampleCount - count0)
        {
            int chunkEnd = Math.Min(sampleIndex + ChunkSize, sampleCount - count0);
            ProcessSampleRange(inputSpan, bufferSpan, state, envelopeEval, sampleIndex, chunkEnd, count0, count1, sampleCount);
            sampleIndex = chunkEnd;
        }
    }

    private static void ProcessFinalSamples(Span<int> inputSpan, Span<int> bufferSpan, FilterState state, EnvelopeGenerator? envelopeEval, int sampleCount, int count0, int count1)
    {
        ProcessSampleRange(inputSpan, bufferSpan, state, envelopeEval, sampleCount - count0, sampleCount, count0, count1, sampleCount, isFinal: true);
    }

    private static int ProcessSampleRange(Span<int> inputSpan, Span<int> bufferSpan, FilterState state,
        EnvelopeGenerator? envelopeEval, int start, int end, int count0, int count1, int sampleCount, bool isFinal = false)
    {
        var lastEnvelopeValue = AudioConstants.FixedPoint.Scale;
        for (var i = start; i < end; i++)
        {
            if (isFinal)
            {
                ApplyFilterToFinalSample(inputSpan, bufferSpan, state, i, count0, count1, sampleCount);
            }
            else
            {
                ApplyFilterToSample(inputSpan, bufferSpan, state, i, count0, count1);
            }
            lastEnvelopeValue = envelopeEval?.Evaluate(sampleCount) ?? AudioConstants.FixedPoint.Scale;

            if (i + 1 < sampleCount)
            {
                var envelopeFactor = lastEnvelopeValue / (float)AudioConstants.FixedPoint.Scale;
                count0 = state.ComputeCoefs(0, envelopeFactor);
                count1 = state.ComputeCoefs(1, envelopeFactor);
            }
        }
        return lastEnvelopeValue;
    }

    private static void ApplyFilterToSample(Span<int> inputSpan, Span<int> bufferSpan, FilterState state, int sampleIndex, int count0, int feedbackCount)
    {
        var output = 0L;
        AddFeedforwardTerms(inputSpan, state, sampleIndex, count0, ref output);
        AddFeedbackTerms(bufferSpan, state, sampleIndex, feedbackCount, ref output, subtract: true);
        bufferSpan[sampleIndex] = (int)output;
    }



    private static void ApplyFilterToFinalSample(Span<int> inputSpan, Span<int> bufferSpan, FilterState state, int sampleIndex, int count0, int feedbackCount, int sampleCount)
    {
        var output = 0L;
        AddFeedforwardTermsForFinal(inputSpan, state, sampleIndex, count0, sampleCount, ref output);
        AddFeedbackTerms(bufferSpan, state, sampleIndex, feedbackCount, ref output, subtract: true);
        bufferSpan[sampleIndex] = (int)output;
    }

    private static void AddFeedforwardTerms(Span<int> inputSpan, FilterState state, int sampleIndex, int count0, ref long output)
    {
        output += ((long)inputSpan[sampleIndex + count0] * state.InverseA0) >> 16;
        for (var j = 0; j < count0; j++)
        {
            output += ((long)inputSpan[sampleIndex + count0 - 1 - j] * state.Feedforward[j]) >> 16;
        }
    }

    private static void AddFeedforwardTermsForFinal(Span<int> inputSpan, FilterState state, int sampleIndex, int count0, int sampleCount, ref long output)
    {
        var startJ = sampleIndex + count0 - sampleCount;
        for (var j = startJ; j < count0; j++)
        {
            var inputIndex = sampleIndex + count0 - 1 - j;
            output += ((long)inputSpan[inputIndex] * state.Feedforward[j]) >> 16;
        }
    }

    private static void AddFeedbackTerms(Span<int> bufferSpan, FilterState state, int sampleIndex, int feedbackCount, ref long output, bool subtract = false)
    {
        var actualFeedbackCount = Math.Min(sampleIndex, feedbackCount);
        for (var j = 0; j < actualFeedbackCount; j++)
        {
            var bufferIndex = sampleIndex - 1 - j;
            var term = ((long)bufferSpan[bufferIndex] * state.Feedback[j]) >> 16;
            output -= term;
        }
    }

    private static float Interpolate(float value0, float value1, float factor)
    {
        return value0 + factor * (value1 - value0);
    }

    private static float GetAmplitude(Filter filter, int direction, int pair, float factor)
    {
        var mag0 = filter.PairMagnitude[direction][0][pair];
        var mag1 = filter.PairMagnitude[direction][1][pair];
        var interpolatedMag = Interpolate(mag0, mag1, factor);
        var dbValue = interpolatedMag * 0.0015258789f;
        return 1.0f - (float)AudioMath.DbToLinear(-dbValue);
    }

    private static float CalculatePhase(Filter filter, int dir, int pair, float factor)
    {
        var phase0 = filter.PairPhase[dir][0][pair];
        var phase1 = filter.PairPhase[dir][1][pair];
        var interpolatedPhase = Interpolate(phase0, phase1, factor);
        var scaledPhase = interpolatedPhase * 1.2207031e-4f;
        return GetOctavePhase(scaledPhase);
    }

    private static float GetOctavePhase(float pow2Value)
    {
        var frequencyHz = Math.Pow(2.0, pow2Value) * 32.703197;
        return (float)(frequencyHz * AudioMath.TwoPi / AudioConstants.SampleRate);
    }

    public class FilterState(Filter filter)
    {
        private readonly Filter _filter = filter;
        private readonly float[,] _floatCoefs = new float[2, MaxCoefs];

        public int[] Feedforward { get; } = new int[MaxCoefs];
        public int[] Feedback { get; } = new int[MaxCoefs];
        public int InverseA0 { get; set; }

        public int ComputeCoefs(int dir, float envelopeFactor)
        {
            Array.Clear(_floatCoefs, 0, _floatCoefs.Length);

            var inverseA0 = ComputeInverseA0(envelopeFactor);
            InverseA0 = inverseA0;
            var floatInvA0 = inverseA0 / (float)AudioConstants.FixedPoint.Scale;
            var pairCount = _filter.PairCounts[dir];

            if (pairCount == 0)
            {
                return 0;
            }

            InitFirstPair(dir, envelopeFactor);
            CascadeRemainingPairs(dir, pairCount, envelopeFactor);
            return ApplyGainAndConvert(dir, pairCount, floatInvA0);
        }

        private int ComputeInverseA0(float envelopeFactor)
        {
            var unityGain0 = _filter.Unity[0];
            var unityGain1 = _filter.Unity[1];
            var interpolatedGain = Interpolate(unityGain0, unityGain1, envelopeFactor);
            var gainDb = interpolatedGain * 0.0030517578f;
            var floatInvA0 = (float)Math.Pow(0.1, gainDb / 20.0);
            return (int)(floatInvA0 * AudioConstants.FixedPoint.Scale);
        }

        private void InitFirstPair(int dir, float envelopeFactor)
        {
            var amp = GetAmplitude(_filter, dir, 0, envelopeFactor);
            var phase = CalculatePhase(_filter, dir, 0, envelopeFactor);
            var cosPhase = (float)Math.Cos(phase);
            _floatCoefs[dir, 0] = -2.0f * amp * cosPhase;
            _floatCoefs[dir, 1] = amp * amp;
        }

        private void CascadeRemainingPairs(int dir, int pairCount, float envelopeFactor)
        {
            for (var pairIndex = 1; pairIndex < pairCount; pairIndex++)
            {
                var ampP = GetAmplitude(_filter, dir, pairIndex, envelopeFactor);
                var phaseP = CalculatePhase(_filter, dir, pairIndex, envelopeFactor);
                var cosPhaseP = (float)Math.Cos(phaseP);
                var term1 = -2.0f * ampP * cosPhaseP;
                var term2 = ampP * ampP;

                _floatCoefs[dir, pairIndex * 2 + 1] = _floatCoefs[dir, pairIndex * 2 - 1] * term2;
                _floatCoefs[dir, pairIndex * 2] = _floatCoefs[dir, pairIndex * 2 - 1] * term1 + _floatCoefs[dir, pairIndex * 2 - 2] * term2;

                for (var coeffIndex = pairIndex * 2 - 1; coeffIndex >= 2; coeffIndex--)
                {
                    _floatCoefs[dir, coeffIndex] += _floatCoefs[dir, coeffIndex - 1] * term1 + _floatCoefs[dir, coeffIndex - 2] * term2;
                }

                _floatCoefs[dir, 1] += _floatCoefs[dir, 0] * term1 + term2;
                _floatCoefs[dir, 0] += term1;
            }
        }

        private int ApplyGainAndConvert(int dir, int pairCount, float floatInvA0)
        {
            var iCoef = dir == 0 ? Feedforward : Feedback;
            var coefCount = pairCount * 2;

            if (dir == 0)
            {
                for (var i = 0; i < coefCount; i++)
                {
                    _floatCoefs[0, i] *= floatInvA0;
                }
            }

            for (var i = 0; i < coefCount; i++)
            {
                iCoef[i] = (int)(_floatCoefs[dir, i] * AudioConstants.FixedPoint.Scale);
            }

            return coefCount;
        }
    }
}