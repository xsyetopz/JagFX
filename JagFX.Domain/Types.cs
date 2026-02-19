namespace JagFX.Domain;

public readonly record struct Millis
{
    public int Value { get; }

    public Millis(int value) => Value = value;

    public Millis ToMillis() => new(Value * Constants.MillisecondsPerSample);

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
