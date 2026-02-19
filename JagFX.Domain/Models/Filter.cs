using System.Collections.Immutable;

namespace JagFX.Domain.Models;

public record Filter(
    ImmutableArray<int> PairCounts,
    ImmutableArray<int> Unity,
    ImmutableArray<ImmutableArray<ImmutableArray<int>>> PairPhase,
    ImmutableArray<ImmutableArray<ImmutableArray<int>>> PairMagnitude,
    Envelope? Envelope
);
