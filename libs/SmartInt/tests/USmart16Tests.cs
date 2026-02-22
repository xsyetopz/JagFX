using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace SmartInt.Tests;

[TestClass]
public class USmart16Tests
{
    [TestMethod]
    public void EncodeDecode_SmallValue_Works()
    {
        var smart = new USmart16(42);
        var buffer = new byte[3];
        var bytes = smart.Encode(buffer);
        var decoded = USmart16.FromEncoded(buffer, out var bytesRead);

        Assert.AreEqual(bytes, bytesRead);
        Assert.AreEqual((ushort)42, decoded.Value);
    }

    [TestMethod]
    public void EncodeDecode_LargeValue_Works()
    {
        var smart = new USmart16(USmart16.MaxValue);
        var buffer = new byte[3];
        var bytes = smart.Encode(buffer);
        var decoded = USmart16.FromEncoded(buffer, out var bytesRead);

        Assert.AreEqual(bytes, bytesRead);
        Assert.AreEqual(USmart16.MaxValue, decoded.Value);
    }
}
