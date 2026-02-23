using JagFX.Core.Constants;
using JagFX.Domain;
using SmartInt;
using System.Buffers.Binary;

namespace JagFX.Io
{
    public class BinaryBuffer
    {
        private int _position;
        private bool _truncated;

        public BinaryBuffer(byte[] data) => Data = data ?? throw new ArgumentNullException(nameof(data));
        public BinaryBuffer(int size) => Data = new byte[size];

        public byte[] Data { get; }
        public int Position => _position;
        public bool IsTruncated => _truncated;
        public int Remaining => Data.Length - _position;


        public void Skip(int n) => _position += n;

        public void SetPosition(int position)
        {
            if (position < 0)
                _position = 0;
            else if (position > Data.Length)
                _position = Data.Length;
            else
                _position = position;
        }

        public int Peek() => _position >= Data.Length ? 0 : Data[_position] & 0xFF;

        public int PeekAt(int offset)
        {
            var pos = _position + offset;
            return pos >= Data.Length ? 0 : Data[pos] & 0xFF;
        }

        public int ReadUInt8()
        {
            if (CheckTruncation(1)) return 0;
            var v = Data[_position] & 0xFF;
            _position++;
            return v;
        }

        public int ReadInt8()
        {
            if (CheckTruncation(1)) return 0;
            var v = Data[_position];
            _position++;
            return v;
        }

        public int ReadUInt16BE()
        {
            if (CheckTruncation(2)) return 0;
            var value = BinaryPrimitives.ReadUInt16BigEndian(Data.AsSpan(_position, 2));
            _position += 2;
            return value;
        }

        public int ReadUInt16LE()
        {
            if (CheckTruncation(2)) return 0;
            var value = BinaryPrimitives.ReadUInt16LittleEndian(Data.AsSpan(_position, 2));
            _position += 2;
            return value;
        }

        public int ReadInt16BE()
        {
            if (CheckTruncation(2)) return 0;
            var value = BinaryPrimitives.ReadInt16BigEndian(Data.AsSpan(_position, 2));
            _position += 2;
            return value;
        }

        public int ReadInt32BE()
        {
            if (CheckTruncation(4)) return 0;
            var value = BinaryPrimitives.ReadInt32BigEndian(Data.AsSpan(_position, 4));
            _position += 4;
            return value;
        }

        public short ReadSmart()
        {
            return ReadSmartBase<short>(
                static (span) => (Smart16.FromEncoded(span, out var bytesRead).Value, bytesRead),
                Smart16.SmartOneByteThreshold
            );
        }

        public ushort ReadUSmart()
        {
            return ReadSmartBase<ushort>(
                static (span) => (USmart16.FromEncoded(span, out var bytesRead).Value, bytesRead),
                USmart16.USmartOneByteThreshold
            );
        }

        public void WriteInt32BE(int value)
        {
            BinaryPrimitives.WriteInt32BigEndian(Data.AsSpan(_position, 4), value);
            _position += 4;
        }

        public void WriteInt32LE(int value)
        {
            BinaryPrimitives.WriteInt32LittleEndian(Data.AsSpan(_position, 4), value);
            _position += 4;
        }

        public void WriteInt16LE(int value)
        {
            BinaryPrimitives.WriteInt16LittleEndian(Data.AsSpan(_position, 2), (short)value);
            _position += 2;
        }

        public void WriteUInt8(int value)
        {
            Data[_position] = (byte)value;
            _position++;
        }

        public void WriteUInt16BE(int value)
        {
            BinaryPrimitives.WriteUInt16BigEndian(Data.AsSpan(_position, 2), (ushort)value);
            _position += 2;
        }

        public void WriteSmart(short value)
        {
            var smart = new Smart16(value);
            _position += smart.Encode(Data.AsSpan(_position));
        }

        public void WriteUSmart(ushort value)
        {
            var smart = new USmart16(value);
            _position += smart.Encode(Data.AsSpan(_position));
        }

        private delegate (T Smart, int BytesRead) SmartDecoder<T>(ReadOnlySpan<byte> span);

        private T ReadSmartBase<T>(SmartDecoder<T> fromEncoded, int oneByteThreshold)
        {
            if (_position >= Data.Length) return default!;
            var b = Data[_position] & 0xFF;
            var needed = b < oneByteThreshold ? 1 : 2;
            if (CheckTruncation(needed)) return default!;
            var (smart, bytesRead) = fromEncoded(Data.AsSpan(_position));
            _position += bytesRead;
            return smart;
        }

        private bool CheckTruncation(int bytes)
        {
            if (_position + bytes > Data.Length)
            {
                _truncated = true;
                _position += bytes;
                return true;
            }
            return false;
        }
    }
}
