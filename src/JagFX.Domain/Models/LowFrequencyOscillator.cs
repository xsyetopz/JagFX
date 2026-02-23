namespace JagFX.Domain.Models;

public record class LowFrequencyOscillator(
    Envelope Rate,
    Envelope Depth
);
