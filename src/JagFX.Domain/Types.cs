namespace JagFX.Domain;

public readonly record struct Milliseconds
{
    public int Value { get; }

    public Milliseconds(int value) => Value = value;

    public Milliseconds ToMilliseconds() => new(Value * Constants.MillisecondsPerSample);

    public Samples ToSamples() => new((int)(Value * Constants.SampleRatePerMillisecond));
}

public readonly record struct Percent
{
    public int Value { get; }

    public Percent(int value) => Value = value;
}

public readonly record struct Samples
{
    public int Value { get; }

    public Samples(int value) => Value = value;

    public Samples ToSamples() => new((int)(Value * Constants.SampleRatePerMillisecond));
}
