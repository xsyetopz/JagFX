using JagFX.Domain;
using JagFX.Domain.Models;
using JagFX.Domain.Utilities;
using System.Collections.Immutable;

namespace JagFX.Synthesis;

public static class TrackMixer
{
    public static SampleBuffer Synthesize(Patch patch, int voiceFilter = -1)
    {
        var voicesToMix = voiceFilter < 0
            ? patch.ActiveVoices
            : [.. patch.ActiveVoices.Where(v => v.Index == voiceFilter)];

        var maxDuration = CalculateMaxDuration(voicesToMix);
        if (maxDuration == 0)
        {
            return SampleBuffer.Empty(0);
        }

        var sampleCount = (int)(maxDuration * Constants.SampleRatePerMillisecond);
        var loopStart = (int)(patch.Loop.Begin * Constants.SampleRatePerMillisecond);
        var loopStop = (int)(patch.Loop.End * Constants.SampleRatePerMillisecond);

        var effectiveLoopCount = ValidateLoopRegion(loopStart, loopStop, sampleCount);
        var totalSampleCount = sampleCount + (loopStop - loopStart) * Math.Max(0, effectiveLoopCount - 1);

        var buffer = MixVoices(voicesToMix, sampleCount, totalSampleCount);
        if (effectiveLoopCount > 1)
        {
            ApplyLoopExpansion(buffer, sampleCount, loopStart, loopStop, effectiveLoopCount);
        }

        MathUtils.ClipInt16(buffer, totalSampleCount);

        var output = new int[totalSampleCount];
        Array.Copy(buffer, 0, output, 0, totalSampleCount);
        SampleBufferPool.Release(buffer);

        return new SampleBuffer(output, Constants.SampleRate);
    }

    private static int CalculateMaxDuration(ImmutableList<(int Index, Voice Voice)> voices)
    {
        var maxDuration = 0;
        foreach (var (_, voice) in voices)
        {
            var endTime = voice.Duration + voice.StartTime;
            if (endTime > maxDuration)
            {
                maxDuration = endTime;
            }
        }

        return maxDuration;
    }

    private static int ValidateLoopRegion(int start, int end, int length)
    {
        if (start < 0 || end > length || start >= end)
        {
            return 0;
        }

        return length;
    }

    private static int[] MixVoices(
        ImmutableList<(int Index, Voice Voice)> voices,
        int sampleCount,
        int totalSampleCount)
    {
        var buffer = SampleBufferPool.Acquire(totalSampleCount);

        foreach (var (_, voice) in voices)
        {
            var voiceBuffer = ToneSynthesizer.Synthesize(voice);
            var startOffset = (int)(voice.StartTime * Constants.SampleRatePerMillisecond);

            for (var i = 0; i < voiceBuffer.Length; i++)
            {
                var pos = i + startOffset;
                if (pos >= 0 && pos < sampleCount)
                {
                    buffer[pos] += voiceBuffer.Samples[i];
                }
            }
        }

        return buffer;
    }

    private static void ApplyLoopExpansion(
        int[] buffer,
        int sampleCount,
        int loopStart,
        int loopStop,
        int loopCount)
    {
        var totalSampleCount = buffer.Length;
        var endOffset = totalSampleCount - sampleCount;

        for (var sample = sampleCount - 1; sample >= loopStop; sample--)
        {
            buffer[sample + endOffset] = buffer[sample];
        }

        for (var loop = 1; loop < loopCount; loop++)
        {
            var offset = (loopStop - loopStart) * loop;
            for (var sample = loopStart; sample < loopStop; sample++)
            {
                buffer[sample + offset] = buffer[sample];
            }
        }
    }
}
