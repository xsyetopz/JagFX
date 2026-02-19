namespace JagFX.IO
{
    public static class WaveFileWriter
    {
        private const int RiffMagic = 0x52494646; // "RIFF"
        private const int WaveMagic = 0x57415645; // "WAVE"
        private const int FmtMagic = 0x666d7420; // "fmt "
        private const int DataMagic = 0x64617461; // "data"

        private const int HeaderSize = 44;
        private const int FmtChunkSize = 16;
        private const int PcmFormat = 1;

        private const int RiffHeaderOffset = 0;
        private const int FileSizeOffset = 4;
        private const int WaveFormatOffset = 8;
        private const int FmtMagicOffset = 12;
        private const int FmtSizeOffset = 16;
        private const int PcmFormatOffset = 20;
        private const int ChannelsOffset = 22;
        private const int SampleRateOffset = 24;
        private const int ByteRateOffset = 28;
        private const int BlockAlignOffset = 32;
        private const int BitsPerSampleOffset = 34;
        private const int DataMagicOffset = 36;
        private const int DataSizeOffset = 40;
        private const int SampleDataOffset = 44;

        public static byte[] Write(byte[] samples, int bitsPerSample = 8)
        {
            var dataSize = samples.Length;
            var fileSize = HeaderSize - 8 + dataSize;

            var buffer = new byte[HeaderSize + dataSize];

            WriteRiffHeader(buffer, fileSize);
            WriteFmtChunk(buffer, bitsPerSample);
            WriteDataChunk(buffer, dataSize);
            Array.Copy(samples, 0, buffer, SampleDataOffset, dataSize);

            return buffer;
        }

        public static void WriteToPath(byte[] samples, string path, int bitsPerSample = 8)
        {
            var wavData = Write(samples, bitsPerSample);
            File.WriteAllBytes(path, wavData);
        }

        private static void WriteRiffHeader(byte[] buffer, int fileSize)
        {
            WriteInt32BE(buffer, RiffHeaderOffset, RiffMagic);
            WriteInt32LE(buffer, FileSizeOffset, fileSize);
            WriteInt32BE(buffer, WaveFormatOffset, WaveMagic);
        }

        private static void WriteFmtChunk(byte[] buffer, int bitsPerSample)
        {
            WriteInt32BE(buffer, FmtMagicOffset, FmtMagic);
            WriteInt32LE(buffer, FmtSizeOffset, FmtChunkSize);
            WriteInt16LE(buffer, PcmFormatOffset, PcmFormat);
            WriteInt16LE(buffer, ChannelsOffset, Domain.Constants.NumChannels);
            WriteInt32LE(buffer, SampleRateOffset, Domain.Constants.SampleRate);
            WriteInt32LE(buffer, ByteRateOffset, Domain.Constants.SampleRate * Domain.Constants.NumChannels * bitsPerSample / 8);
            WriteInt16LE(buffer, BlockAlignOffset, Domain.Constants.NumChannels * bitsPerSample / 8);
            WriteInt16LE(buffer, BitsPerSampleOffset, bitsPerSample);
        }

        private static void WriteDataChunk(byte[] buffer, int dataSize)
        {
            WriteInt32BE(buffer, DataMagicOffset, DataMagic);
            WriteInt32LE(buffer, DataSizeOffset, dataSize);
        }

        private static void WriteInt16LE(byte[] buffer, int offset, int value)
        {
            buffer[offset] = (byte)value;
            buffer[offset + 1] = (byte)(value >> 8);
        }

        private static void WriteInt32BE(byte[] buffer, int offset, int value)
        {
            buffer[offset] = (byte)(value >> 24);
            buffer[offset + 1] = (byte)(value >> 16);
            buffer[offset + 2] = (byte)(value >> 8);
            buffer[offset + 3] = (byte)value;
        }

        private static void WriteInt32LE(byte[] buffer, int offset, int value)
        {
            buffer[offset] = (byte)value;
            buffer[offset + 1] = (byte)(value >> 8);
            buffer[offset + 2] = (byte)(value >> 16);
            buffer[offset + 3] = (byte)(value >> 24);
        }
    }
}
