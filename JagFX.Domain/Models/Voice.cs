using System.Collections.Immutable;

namespace JagFX.Domain.Models;

public record class Voice(
    Envelope FrequencyEnvelope,
    Envelope AmplitudeEnvelope,
    Lfo? PitchLfo,
    Lfo? AmplitudeLfo,
    Envelope? GateSilence,
    Envelope? GateDuration,
    ImmutableList<Oscillator> Oscillators,
    FeedbackDelay FeedbackDelay,
    int Duration,
    int StartTime,
    Filter? Filter = null
);

