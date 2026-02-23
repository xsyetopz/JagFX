using System.Collections.Immutable;
namespace JagFX.Domain.Models;

public record class Envelope(Waveform Waveform, int StartSample, int EndSample, ImmutableList<Segment> Segments);
public record struct Segment(int DurationSamples, int PeakLevel);
