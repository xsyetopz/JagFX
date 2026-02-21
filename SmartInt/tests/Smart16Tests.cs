using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace SmartInt.Tests;

[TestClass]
public class Smart16Tests
{
    [TestMethod]
    public void EncodeDecode_SmallValue_Works()
    {
        var smart = new Smart16(42);
        var buffer = new byte[3];
        var bytes = smart.Encode(buffer);
        var decoded = Smart16.FromEncoded(buffer, out var bytesRead);

        Assert.AreEqual(bytes, bytesRead);
        Assert.AreEqual((short)42, decoded.Value);
    }

    [TestMethod]
    public void EncodeDecode_NegativeValue_Works()
    {
        var smart = new Smart16(-100);
        var buffer = new byte[3];
        var bytes = smart.Encode(buffer);
        var decoded = Smart16.FromEncoded(buffer, out var bytesRead);

        Assert.AreEqual(bytes, bytesRead);
        Assert.AreEqual((short)-100, decoded.Value);
    }

    [TestMethod]
    public void EncodeDecode_LargeValue_Works()
    {
        var smart = new Smart16(Smart16.MaxValue);
        var buffer = new byte[3];
        var bytes = smart.Encode(buffer);
        var decoded = Smart16.FromEncoded(buffer, out var bytesRead);

        Assert.AreEqual(bytes, bytesRead);
        Assert.AreEqual(Smart16.MaxValue, decoded.Value);
    }
}
