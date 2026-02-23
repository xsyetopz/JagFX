using System.Collections.Immutable;
namespace JagFX.Domain.Models;

public record Filter(
    ImmutableArray<int> PoleCounts,
    ImmutableArray<int> UnityGain,
    ImmutableArray<ImmutableArray<ImmutableArray<int>>> PolePhase,
    ImmutableArray<ImmutableArray<ImmutableArray<int>>> PoleMagnitude,
    Envelope? CutoffEnvelope
);
