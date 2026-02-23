namespace JagFX.Domain.Models;

public record LoopSegment(int BeginSample, int EndSample)
{
    public bool IsActive => BeginSample < EndSample;
}
