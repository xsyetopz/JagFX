namespace JagFX.Domain.Models;

public record class LowFrequencyOscillator(
    Envelope FrequencyRate,
    Envelope ModulationDepth
);
