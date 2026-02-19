namespace JagFX.Domain.Models;

public record Loop(int Begin, int End)
{
    public bool IsActive => Begin < End;
}
