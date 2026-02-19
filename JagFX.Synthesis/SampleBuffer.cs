using JagFX.Domain;
using JagFX.Domain.Utilities;

namespace JagFX.Synthesis;

public class SampleBuffer(int[] samples, int sampleRate = Constants.SampleRate)
{
    public int[] Samples { get; } = samples;

    public int SampleRate { get; } = sampleRate;

    public int Length => Samples.Length;

    public static SampleBuffer Empty(int sampleCount, int sampleRate = Constants.SampleRate)
    {
        return new SampleBuffer(new int[sampleCount], sampleRate);
    }

    public SampleBuffer Mix(SampleBuffer other, int offset)
    {
        var maxLen = Math.Max(Samples.Length, other.Samples.Length + offset);
        var result = new int[maxLen];
        Array.Copy(Samples, 0, result, 0, Samples.Length);

        for (var i = 0; i < other.Samples.Length; i++)
        {
            var pos = i + offset;
            if (pos >= 0 && pos < maxLen)
            {
                result[pos] += other.Samples[i];
            }
        }

        return new SampleBuffer(result, SampleRate);
    }

    public SampleBuffer Clip()
    {
        var newSamples = (int[])Samples.Clone();
        MathUtils.ClipInt16(newSamples);
        return new SampleBuffer(newSamples, SampleRate);
    }

    public byte[] ToUBytes()
    {
        var result = new byte[Samples.Length];
        for (var i = 0; i < Samples.Length; i++)
        {
            result[i] = (byte)((Samples[i] >> 8) + 128);
        }
        return result;
    }

    public byte[] ToBytes()
    {
        var result = new byte[Samples.Length];
        for (var i = 0; i < Samples.Length; i++)
        {
            result[i] = (byte)(Samples[i] >> 8);
        }
        return result;
    }

    public byte[] ToBytes16BE()
    {
        var result = new byte[Samples.Length * 2];
        for (var i = 0; i < Samples.Length; i++)
        {
            result[i * 2] = (byte)(Samples[i] >> 8);
            result[i * 2 + 1] = (byte)Samples[i];
        }
        return result;
    }

    public byte[] ToBytes16LE()
    {
        var result = new byte[Samples.Length * 2];
        for (var i = 0; i < Samples.Length; i++)
        {
            result[i * 2] = (byte)Samples[i];
            result[i * 2 + 1] = (byte)(Samples[i] >> 8);
        }
        return result;
    }
}
