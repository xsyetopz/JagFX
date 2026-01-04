public class SoundEffectTrackDefinition {
	public int start;
	public InstrumentDefinition[] instruments = new InstrumentDefinition[10];
	public int end;

	public final byte[] mix() {
		int instrumentIndex;
		int maxLength = 0;

		for (instrumentIndex = 0; instrumentIndex < 10; ++instrumentIndex) {
			if (this.instruments[instrumentIndex] == null || this.instruments[instrumentIndex].duration + this.instruments[instrumentIndex].offset <= maxLength) {
				continue;
			}

			maxLength = this.instruments[instrumentIndex].duration + this.instruments[instrumentIndex].offset;
		}

		if (maxLength == 0) {
			return new byte[0];
		}

		instrumentIndex = maxLength * 22050 / 1000;

		byte[] samples = new byte[instrumentIndex];

		for (int currentInstrument = 0; currentInstrument < 10; ++currentInstrument) {
			if (this.instruments[currentInstrument] == null) {
				continue;
			}

			int sampleCount = this.instruments[currentInstrument].duration * 22050 / 1000;
			int sampleOffset = this.instruments[currentInstrument].offset * 22050 / 1000;

			int[] instrumentSamples = this.instruments[currentInstrument].synthesize(sampleCount, this.instruments[currentInstrument].duration);

			for (int sampleIndex = 0; sampleIndex < sampleCount; ++sampleIndex) {
				int mixedValue = (instrumentSamples[sampleIndex] >> 8) + samples[sampleIndex + sampleOffset];

				if ((mixedValue + 128 & -256) != 0) {
					mixedValue = mixedValue >> 31 ^ 127;
				}

				samples[sampleIndex + sampleOffset] = (byte) mixedValue;
			}
		}

		return samples;
	}
}
