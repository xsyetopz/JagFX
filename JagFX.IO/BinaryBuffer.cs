using JagFX.Domain;
using SmartInt;

namespace JagFX.IO
{
    public class BinaryBuffer
    {
        private int _position;
        private bool _truncated;

        public byte[] Data { get; }
        public int Position => _position;
        public bool IsTruncated => _truncated;
        public int Remaining => Data.Length - _position;

        public BinaryBuffer(byte[] data) => Data = data ?? throw new ArgumentNullException(nameof(data));

        public BinaryBuffer(int size) => Data = new byte[size];

        public void Skip(int n) => _position += n;

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
            _position += 2;
            return ((Data[_position - 2] & 0xFF) << 8) + (Data[_position - 1] & 0xFF);
        }

        public int ReadUInt16LE()
        {
            if (CheckTruncation(2)) return 0;
            _position += 2;
            return (Data[_position - 2] & 0xFF) + ((Data[_position - 1] & 0xFF) << 8);
        }

        public int ReadInt16BE()
        {
            if (CheckTruncation(2)) return 0;
            _position += 2;
            var value = ((Data[_position - 2] & 0xFF) << 8) + (Data[_position - 1] & 0xFF);
            if (value > short.MaxValue) value -= Constants.FixedPoint.Scale;
            return value;
        }

        public int ReadInt32BE()
        {
            if (CheckTruncation(4)) return 0;
            _position += 4;
            return ((Data[_position - 4] & 0xFF) << 24) +
                   ((Data[_position - 3] & 0xFF) << 16) +
                   ((Data[_position - 2] & 0xFF) << 8) +
                   (Data[_position - 1] & 0xFF);
        }

        public short ReadSmart()
        {
            if (_position >= Data.Length) return 0;

            var b = Data[_position] & 0xFF;
            return b < 128 ? ReadSmartOneByte() : ReadSmartTwoBytes();
        }

        public ushort ReadUSmart()
        {
            if (_position >= Data.Length) return 0;

            var value = Data[_position] & 0xFF;
            return value < 128 ? ReadUSmartOneByte() : ReadUSmartTwoBytes();
        }

        public void WriteInt32BE(int value)
        {
            Data[_position] = (byte)(value >> 24);
            Data[_position + 1] = (byte)(value >> 16);
            Data[_position + 2] = (byte)(value >> 8);
            Data[_position + 3] = (byte)value;
            _position += 4;
        }

        public void WriteInt32LE(int value)
        {
            Data[_position] = (byte)value;
            Data[_position + 1] = (byte)(value >> 8);
            Data[_position + 2] = (byte)(value >> 16);
            Data[_position + 3] = (byte)(value >> 24);
            _position += 4;
        }

        public void WriteInt16LE(int value)
        {
            Data[_position] = (byte)value;
            Data[_position + 1] = (byte)(value >> 8);
            _position += 2;
        }

        public void WriteUInt8(int value)
        {
            Data[_position] = (byte)value;
            _position++;
        }

        public void WriteUInt16BE(int value)
        {
            Data[_position] = (byte)(value >> 8);
            Data[_position + 1] = (byte)value;
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

        private short ReadSmartOneByte()
        {
            var b = Data[_position] & 0xFF;
            _position++;
            return (short)(b - 128);
        }

        private short ReadSmartTwoBytes()
        {
            if (_position + 2 > Data.Length)
            {
                _truncated = true;
                _position += 2;
                return 0;
            }
            var b = Data[_position] & 0xFF;
            _position += 2;
            var value = ((b & 0x7F) << 8) | (Data[_position - 1] & 0xFF);
            return (short)(value > short.MaxValue ? value - ushort.MaxValue - 1 : value);
        }

        private ushort ReadUSmartOneByte()
        {
            _position++;
            return (ushort)(Data[_position - 1] & 0xFF);
        }

        private ushort ReadUSmartTwoBytes()
        {
            if (_position + 2 > Data.Length)
            {
                _truncated = true;
                _position = Data.Length;
                return 0;
            }
            _position += 2;
            return (ushort)(((Data[_position - 2] & 0xFF) << 8) + (Data[_position - 1] & 0xFF) - Constants.FixedPoint.Offset);
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
