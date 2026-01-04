public class SoundEffect {
	Instrument[] instruments;
	int start;
	int end;

	SoundEffect(Buffer buffer) {
		this.instruments = new Instrument[10];

		for (int instrumentIndex = 0; instrumentIndex < 10; ++instrumentIndex) {
			int hasInstrument = buffer.readUnsignedByte();
			if (hasInstrument != 0) {
				--buffer.offset;
				this.instruments[instrumentIndex] = new Instrument();
				this.instruments[instrumentIndex].decode(buffer);
			}
		}

		this.start = buffer.readUnsignedShort();
		this.end = buffer.readUnsignedShort();
	}

	public RawSound toRawSound() {
		byte[] samples = this.mix();
		return new RawSound(22050, samples, this.start * 22050 / 1000, this.end * 22050 / 1000);
	}

	public final int calculateDelay() {
		int minDelay = 9999999;

		int instrumentIndex;
		for (instrumentIndex = 0; instrumentIndex < 10; ++instrumentIndex) {
			if (this.instruments[instrumentIndex] != null && this.instruments[instrumentIndex].offset / 20 < minDelay) {
				minDelay = this.instruments[instrumentIndex].offset / 20;
			}
		}

		if (this.start < this.end && this.start / 20 < minDelay) {
			minDelay = this.start / 20;
		}

		if (minDelay != 9999999 && minDelay != 0) {
			for (instrumentIndex = 0; instrumentIndex < 10; ++instrumentIndex) {
				if (this.instruments[instrumentIndex] != null) {
					Instrument instrument = this.instruments[instrumentIndex];
					instrument.offset -= minDelay * 20;
				}
			}

			if (this.start < this.end) {
				this.start -= minDelay * 20;
				this.end -= minDelay * 20;
			}

			return minDelay;
		} else {
			return 0;
		}
	}

	final byte[] mix() {
		int maxLength = 0;

		int instrumentIndex;
		for (instrumentIndex = 0; instrumentIndex < 10; ++instrumentIndex) {
			if (this.instruments[instrumentIndex] != null && this.instruments[instrumentIndex].duration + this.instruments[instrumentIndex].offset > maxLength) {
				maxLength = this.instruments[instrumentIndex].duration + this.instruments[instrumentIndex].offset;
			}
		}

		if (maxLength == 0) {
			return new byte[0];
		} else {
			instrumentIndex = maxLength * 22050 / 1000;
			byte[] samples = new byte[instrumentIndex];

			for (int currentInstrument = 0; currentInstrument < 10; ++currentInstrument) {
				if (this.instruments[currentInstrument] != null) {
					int sampleCount = this.instruments[currentInstrument].duration * 22050 / 1000;
					int sampleOffset = this.instruments[currentInstrument].offset * 22050 / 1000;
					int[] instrumentSamples = this.instruments[currentInstrument].synthesize(sampleCount, this.instruments[currentInstrument].duration);

					for (int sampleIndex = 0; sampleIndex < sampleCount; ++sampleIndex) {
						int mixedValue = (instrumentSamples[sampleIndex] >> 8) + samples[sampleIndex + sampleOffset];
						if ((mixedValue + 128 & -256) != 0) {
							mixedValue = mixedValue >> 31 ^ 127;
						}

						samples[sampleIndex + sampleOffset] = (byte)mixedValue;
					}
				}
			}

			return samples;
		}
	}

	public static SoundEffect readSoundEffect(AbstractArchive archive, int groupId, int fileId) {
		byte[] data = archive.takeFile(groupId, fileId);
		return data == null ? null : new SoundEffect(new Buffer(data));
	}
}
