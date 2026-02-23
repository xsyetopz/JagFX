namespace JagFX.Domain.Models;

public record Loop(int BeginSample, int EndSample)
{
    public bool IsActive => BeginSample < EndSample;
}
