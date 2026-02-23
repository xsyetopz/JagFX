using System.Collections.Immutable;
namespace JagFX.Domain.Models;

public record class Voice(
    Envelope FrequencyEnvelope,
    Envelope AmplitudeEnvelope,
    LowFrequencyOscillator? PitchLfo,
    LowFrequencyOscillator? AmplitudeLfo,
    Envelope? GateSilenceEnvelope,
    Envelope? GateDurationEnvelope,
    ImmutableList<Partial> Partials,
    Echo Echo,
    int DurationSamples,
    int StartSample,
    Filter? Filter = null
);
