using JagFX.Domain;

namespace JagFX.IO
{
    public static class SynthInspector
    {
        public static void Inspect(string filePath)
        {
            if (!File.Exists(filePath))
            {
                Console.WriteLine($"File not found: {filePath}");
                return;
            }

            Console.WriteLine($"INSPECTING: {filePath}");
            var bytes = File.ReadAllBytes(filePath);
            Console.WriteLine($"SIZE: {bytes.Length} bytes\n");

            var buf = new BinaryBuffer(bytes);

            try
            {
                InspectWavetable(buf);
                PrintParseResult(buf);
            }
            catch (Exception e)
            {
                PrintParseError(buf, e);
            }
        }

        private static void PrintParseResult(BinaryBuffer buf)
        {
            if (buf.Remaining > 0)
            {
                Console.WriteLine($"\n[WARNING] {buf.Remaining} BYTES REMAINING:");
                Console.WriteLine(DumpRemaining(buf, 0, 64));
            }
            else
            {
                Console.WriteLine("\n[SUCCESS] EOF REACHED CLEANLY");
            }
        }

        private static void PrintParseError(BinaryBuffer buf, Exception e)
        {
            Console.WriteLine($"\n[ERROR] PARSING FAILED AT OFFSET {buf.Position}: {e.Message}");
            Console.WriteLine("REMAINING CONTEXT:");
            Console.WriteLine(DumpRemaining(buf, 0, 64));
        }

        private static void InspectWavetable(BinaryBuffer buf)
        {
            for (var oscillatorIndex = 0; oscillatorIndex < 10; oscillatorIndex++)
            {
                if (buf.Remaining > 0)
                {
                    var marker = buf.Peek();
                    Console.WriteLine($"\n└─ OSCILLATOR {oscillatorIndex} (OFFSET: {buf.Position:X4})");
                    Console.WriteLine($"   │ HEADER: {marker} (ACTIVE: {marker != 0})");

                    if (marker != 0)
                    {
                        InspectOscillator(buf);
                    }
                    else
                    {
                        var emptyMarker = buf.ReadUInt8();
                        Console.WriteLine($"   └─ EMPTY MARKER: {emptyMarker}");
                        Console.WriteLine($"   └─ Empty oscillator");
                    }
                }
            }
        }

        private static void InspectOscillator(BinaryBuffer buf)
        {
            Console.WriteLine($"   ├─ PITCH MODULATION");
            InspectADSREnvelope(buf, "   │");

            Console.WriteLine($"   ├─ AMPLITUDE MODULATION");
            InspectADSREnvelope(buf, "   │");

            Console.WriteLine($"   ├─ VIBRATO LFO");
            InspectLFOPair(buf, "   │");

            Console.WriteLine($"   ├─ TREMOLO LFO");
            InspectLFOPair(buf, "   │");

            Console.WriteLine($"   ├─ GATE LFO");
            InspectLFOPair(buf, "   │");

            InspectPartialSeries(buf);

            Console.WriteLine($"   ├─ OSCILLATOR PARAMETERS");
            var feedbackAmount = buf.ReadUSmart();
            Console.WriteLine($"      │ FEEDBACK AMOUNT: {feedbackAmount}");
            var feedbackMix = buf.ReadUSmart();
            Console.WriteLine($"      │ FEEDBACK MIX: {feedbackMix}");
            var decayTime = buf.ReadUInt16BE();
            Console.WriteLine($"      │ DECAY TIME: {decayTime}");
            var phaseOffset = buf.ReadUInt16BE();
            Console.WriteLine($"      └─ PHASE OFFSET: {phaseOffset}");

            InspectFrequencyResponse(buf);
        }

        private static void InspectADSREnvelope(BinaryBuffer buf, string indent)
        {
            var waveformId = buf.ReadUInt8();
            Console.WriteLine($"{indent}  WAVEFORM ID: {waveformId}");
            var startAmplitude = buf.ReadInt32BE();
            Console.WriteLine($"{indent}  START AMPLITUDE: {startAmplitude}");
            var endAmplitude = buf.ReadInt32BE(); Console.WriteLine($"{indent}  END AMPLITUDE: {endAmplitude}");
            var segmentCount = buf.ReadUInt8(); Console.WriteLine($"{indent}  SEGMENT COUNT: {segmentCount}");
            for (var segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++)
            {
                var duration = buf.ReadUInt16BE(); Console.WriteLine($"{indent}  ├─ SEGMENT {segmentIndex} DURATION: {duration}");
                var peak = buf.ReadUInt16BE(); Console.WriteLine($"{indent}  └─ SEGMENT {segmentIndex} PEAK: {peak}");
            }
        }

        private static void InspectLFOPair(BinaryBuffer buf, string indent)
        {
            var marker = buf.Peek();
            Console.WriteLine($"{indent}  MARKER: {marker}");
            if (marker != 0)
            {
                Console.WriteLine($"{indent}  ├─ LFO WAVEFORM 1:");
                InspectADSREnvelope(buf, $"{indent}  │");
                Console.WriteLine($"{indent}  └─ LFO WAVEFORM 2:");
                InspectADSREnvelope(buf, $"{indent}  ");
            }
            else
            {
                var emptyMarker = buf.ReadUInt8(); Console.WriteLine($"{indent}  EMPTY MARKER: {emptyMarker}");
            }
        }

        private static void InspectPartialSeries(BinaryBuffer buf)
        {
            Console.WriteLine($"   ├─ OSCILLATOR SERIES");
            var continueFlag = true;
            var oscillatorIndex = 0;
            while (continueFlag && oscillatorIndex < Constants.MaxOscillators)
            {
                var marker = buf.Peek();
                if (marker != 0)
                {
                    var amplitude = buf.ReadSmart(); Console.WriteLine($"      ├─ OSCILLATOR {oscillatorIndex} AMPLITUDE: {amplitude}");
                    var pitchOffset = buf.ReadSmart(); Console.WriteLine($"      ├─ OSCILLATOR {oscillatorIndex} PITCH OFFSET: {pitchOffset}");
                    var delay = buf.ReadSmart(); Console.WriteLine($"      └─ OSCILLATOR {oscillatorIndex} DELAY: {delay}");
                    oscillatorIndex++;
                }
                else
                {
                    var seriesEnd = buf.ReadSmart(); Console.WriteLine($"      └─ SERIES END: {seriesEnd}");
                    continueFlag = false;
                }
            }
        }

        private static void InspectFrequencyResponse(BinaryBuffer buf)
        {
            Console.WriteLine($"   └─ FREQUENCY RESPONSE");
            if (buf.Remaining == 0)
            {
                Console.WriteLine($"      └─ EOF reached");
                return;
            }

            var (packed, channel0PairCount, channel1PairCount) = ReadChannelConfig(buf);
            if (packed == 0)
            {
                Console.WriteLine($"      └─ RESPONSE EMPTY");
                return;
            }

            ReadUnityGains(buf);
            var modMask = buf.ReadUInt8();
            Console.WriteLine($"      MODULATION MASK: {modMask}");

            InspectChannelPoles(buf, channel0PairCount, channel1PairCount);
            InspectChannelModulation(buf, channel0PairCount, channel1PairCount, modMask);

            if (modMask != 0)
            {
                Console.WriteLine("      RESPONSE ENVELOPE:");
                InspectEnvelopeSegments(buf, "      ");
            }
        }

        private static (int, int, int) ReadChannelConfig(BinaryBuffer buf)
        {
            var packed = buf.ReadUInt8();
            Console.WriteLine($"      CHANNEL CONFIG: {packed}");
            var channel0PairCount = packed >> 4;
            var channel1PairCount = packed & 0xF;
            Console.WriteLine($"      PAIR COUNTS: CH0={channel0PairCount}, CH1={channel1PairCount}");
            return (packed, channel0PairCount, channel1PairCount);
        }

        private static void ReadUnityGains(BinaryBuffer buf)
        {
            var unityCh0 = buf.ReadUInt16BE();
            Console.WriteLine($"      UNITY GAIN CH0: {unityCh0}");
            var unityCh1 = buf.ReadUInt16BE();
            Console.WriteLine($"      UNITY GAIN CH1: {unityCh1}");
        }

        private static void InspectChannelPoles(BinaryBuffer buf, int channel0PairCount, int channel1PairCount)
        {
            for (var channelIndex = 0; channelIndex < 2; channelIndex++)
            {
                var pairCount = channelIndex == 0 ? channel0PairCount : channel1PairCount;
                Console.WriteLine($"      ├─ CHANNEL {channelIndex} POLES ({pairCount})");
                for (var poleIndex = 0; poleIndex < pairCount; poleIndex++)
                {
                    var freq = buf.ReadUInt16BE();
                    Console.WriteLine($"         ├─ POLE {poleIndex} FREQUENCY: {freq}");
                    var mag = buf.ReadUInt16BE();
                    Console.WriteLine($"         └─ POLE {poleIndex} MAGNITUDE: {mag}");
                }
            }
        }

        private static void InspectChannelModulation(BinaryBuffer buf, int channel0PairCount, int channel1PairCount, int modMask)
        {
            for (var channelIndex = 0; channelIndex < 2; channelIndex++)
            {
                var pairCount = channelIndex == 0 ? channel0PairCount : channel1PairCount;
                Console.WriteLine($"      └─ CHANNEL {channelIndex} MODULATION");
                for (var poleIndex = 0; poleIndex < pairCount; poleIndex++)
                {
                    if ((modMask & (1 << (channelIndex * 4) << poleIndex)) != 0)
                    {
                        var freqMod = buf.ReadUInt16BE();
                        Console.WriteLine($"         ├─ POLE {poleIndex} FREQUENCY MODULATION: {freqMod}");
                        var magMod = buf.ReadUInt16BE();
                        Console.WriteLine($"         └─ POLE {poleIndex} MAGNITUDE MODULATION: {magMod}");
                    }
                }
            }
        }

        private static void InspectEnvelopeSegments(BinaryBuffer buf, string indent)
        {
            var segmentCount = buf.ReadUInt8(); Console.WriteLine($"{indent}   SEGMENT COUNT: {segmentCount}");
            for (var segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++)
            {
                var duration = buf.ReadUInt16BE(); Console.WriteLine($"{indent}   ├─ SEGMENT {segmentIndex} DURATION: {duration}");
                var peak = buf.ReadUInt16BE(); Console.WriteLine($"{indent}   └─ SEGMENT {segmentIndex} PEAK: {peak}");
            }
        }

        private static string DumpRemaining(BinaryBuffer buf, int offset = 0, int limit = 64)
        {
            var count = Math.Min(limit, buf.Remaining);
            var chunk = buf.Data.Skip(buf.Position + offset).Take(count).ToArray();
            return string.Join(" ", chunk.Select(b => $"{b:X2}"));
        }
    }
}
