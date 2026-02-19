namespace JagFX.Domain.Utilities;

public static class MathUtils
{
    public enum UnitType
    {
        Raw16,
        Percent,
        Normalized,
        Decicents
    }

    public const double TwoPi = /* 2.0 * Math.PI */ 6.283185307179586;
    public const double HalfPi = /* Math.PI / 2.0 */ 1.5707963267948966;

    private const double PercentScale = 100.0;
    private const double DecicentsScale = 1200.0;

    private static readonly double Log10 = Math.Log(10.0);

    public static double Distance(double x1, double y1, double x2, double y2)
    {
        var (dx, dy) = (x2 - x1, y2 - y1);
        return Math.Sqrt(dx * dx + dy * dy);
    }

    public static double Clamp(double value, double min, double max)
    {
        return Math.Max(min, Math.Min(max, value));
    }

    public static int Clamp(int value, int min, int max)
    {
        return Math.Max(min, Math.Min(max, value));
    }

    public static void ClipInt16(int[] buffer, int len = -1)
    {
        var end = len < 0 ? buffer.Length : len;
        for (var i = 0; i < end; i++)
        {
            if (buffer[i] < short.MinValue)
                buffer[i] = short.MinValue;
            else if (buffer[i] > short.MaxValue)
                buffer[i] = short.MaxValue;
        }
    }

    public static void ClipToByte(int[] buffer, int len = -1)
    {
        var end = len < 0 ? buffer.Length : len;
        for (var i = 0; i < end; i++)
        {
            var sample = buffer[i];
            if ((sample + byte.MinValue & -byte.MaxValue - 1) != 0)
            {
                buffer[i] = (sample >> 31) ^ 127;
            }
            else
            {
                buffer[i] = sample;
            }
        }
    }

    public static double Lerp(double a, double b, double t)
    {
        return a + (b - a) * t;
    }

    public static double MapRange(double value, double inMin, double inMax, double outMin, double outMax)
    {
        return outMin + (value - inMin) / (inMax - inMin) * (outMax - outMin);
    }

    public static double DbToLinear(double dB)
    {
        return Math.Exp(dB / 20.0 * Log10);
    }

    public static double LinearToDb(double linear)
    {
        return 20.0 * Math.Log10(linear);
    }

    public static double Convert(double value, UnitType from, UnitType to)
    {
        if (from == to)
            return value;

        var normalized = from switch
        {
            UnitType.Raw16 => value / Constants.FixedPoint.Scale,
            UnitType.Percent => value / PercentScale,
            UnitType.Normalized => value,
            UnitType.Decicents => value / DecicentsScale,
            _ => value
        };

        return to switch
        {
            UnitType.Raw16 => normalized * Constants.FixedPoint.Scale,
            UnitType.Percent => normalized * PercentScale,
            UnitType.Normalized => normalized,
            UnitType.Decicents => normalized * DecicentsScale,
            _ => normalized
        };
    }

    public static string Format(double value, UnitType unit, int decimals = 1)
    {
        var fmt = $"%.{decimals}f";
        return unit switch
        {
            UnitType.Raw16 => ((int)value).ToString(),
            UnitType.Percent => $"{string.Format(fmt, value)}%",
            UnitType.Normalized => string.Format(fmt, value),
            UnitType.Decicents => $"{string.Format(fmt, value / 10.0)} st",
            _ => value.ToString()
        };
    }

    public static double DistanceToSegment(double px, double py, double x1, double y1, double x2, double y2)
    {
        var (dx, dy) = (x2 - x1, y2 - y1);
        var lengthSquared = dx * dx + dy * dy;
        if (lengthSquared == 0.0)
            return Distance(px, py, x1, y1);

        var t = Clamp(((px - x1) * dx + (py - y1) * dy) / lengthSquared, 0.0, 1.0);
        var (projx, projy) = (x1 + t * dx, y1 + t * dy);

        (dx, dy) = (px - projx, py - projy);
        return Math.Sqrt(dx * dx + dy * dy);
    }
}
