using System.Collections.Immutable;

namespace JagFX.Domain.Models;

public record class Voice(
    Envelope FrequencyEnvelope,
    Envelope AmplitudeEnvelope,
    LowFrequencyOscillator? PitchLfo,
    LowFrequencyOscillator? AmplitudeLfo,
    Envelope? GateSilence,
    Envelope? GateDuration,
    ImmutableList<Oscillator> Oscillators,
    FeedbackDelay FeedbackDelay,
    int Duration,
    int StartTime,
    Filter? Filter = null
);

