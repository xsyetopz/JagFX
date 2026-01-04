public class AudioFilter {
	static float[][] floatCoefficients;
	static int[][] coefficients;
	static float gainFloat;
	static int forwardMultiplier;
	int[] pairs;
	int[][][] frequencies;
	int[][][] magnitudes;
	int[] gains;

	static {
		floatCoefficients = new float[2][8];
		coefficients = new int[2][8];
	}

	AudioFilter() {
		this.pairs = new int[2];
		this.frequencies = new int[2][2][4];
		this.magnitudes = new int[2][2][4];
		this.gains = new int[2];
	}

	float computeMagnitude(int channel, int pairIndex, float position) {
		float interpolatedValue = (float)this.magnitudes[channel][0][pairIndex] + position * (float)(this.magnitudes[channel][1][pairIndex] - this.magnitudes[channel][0][pairIndex]);
		interpolatedValue *= 0.0015258789F;
		return 1.0F - (float)Math.pow(10.0D, (double)(-interpolatedValue / 20.0F));
	}

	float computeFrequency(int channel, int pairIndex, float position) {
		float interpolatedValue = (float)this.frequencies[channel][0][pairIndex] + position * (float)(this.frequencies[channel][1][pairIndex] - this.frequencies[channel][0][pairIndex]);
		interpolatedValue *= 1.2207031E-4F;
		return normalize(interpolatedValue);
	}

	int compute(int channel, float position) {
		float value;
		if (channel == 0) {
			value = (float)this.gains[0] + (float)(this.gains[1] - this.gains[0]) * position;
			value *= 0.0030517578F;
			gainFloat = (float)Math.pow(0.1D, (double)(value / 20.0F));
			forwardMultiplier = (int)(gainFloat * 65536.0F);
		}

		if (this.pairs[channel] == 0) {
			return 0;
		} else {
			value = this.computeMagnitude(channel, 0, position);
			floatCoefficients[channel][0] = -2.0F * value * (float)Math.cos((double)this.computeFrequency(channel, 0, position));
			floatCoefficients[channel][1] = value * value;

			float[] coeffArray;
			int pairIndex;
			for (pairIndex = 1; pairIndex < this.pairs[channel]; ++pairIndex) {
				value = this.computeMagnitude(channel, pairIndex, position);
				float cosineProduct = -2.0F * value * (float)Math.cos((double)this.computeFrequency(channel, pairIndex, position));
				float squaredValue = value * value;
				floatCoefficients[channel][pairIndex * 2 + 1] = floatCoefficients[channel][pairIndex * 2 - 1] * squaredValue;
				floatCoefficients[channel][pairIndex * 2] = floatCoefficients[channel][pairIndex * 2 - 1] * cosineProduct + floatCoefficients[channel][pairIndex * 2 - 2] * squaredValue;

				for (int coeffIndex = pairIndex * 2 - 1; coeffIndex >= 2; --coeffIndex) {
					coeffArray = floatCoefficients[channel];
					coeffArray[coeffIndex] += floatCoefficients[channel][coeffIndex - 1] * cosineProduct + floatCoefficients[channel][coeffIndex - 2] * squaredValue;
				}

				coeffArray = floatCoefficients[channel];
				coeffArray[1] += floatCoefficients[channel][0] * cosineProduct + squaredValue;
				coeffArray = floatCoefficients[channel];
				coeffArray[0] += cosineProduct;
			}

			if (channel == 0) {
				for (pairIndex = 0; pairIndex < this.pairs[0] * 2; ++pairIndex) {
					coeffArray = floatCoefficients[0];
					coeffArray[pairIndex] *= gainFloat;
				}
			}

			for (pairIndex = 0; pairIndex < this.pairs[channel] * 2; ++pairIndex) {
				coefficients[channel][pairIndex] = (int)(floatCoefficients[channel][pairIndex] * 65536.0F);
			}

			return this.pairs[channel] * 2;
		}
	}

	final void decode(Buffer buffer, SoundEnvelope envelope) {
		int packedPairs = buffer.readUnsignedByte();
		this.pairs[0] = packedPairs >> 4;
		this.pairs[1] = packedPairs & 15;
		if (packedPairs != 0) {
			this.gains[0] = buffer.readUnsignedShort();
			this.gains[1] = buffer.readUnsignedShort();
			int modulationMask = buffer.readUnsignedByte();

			int channel;
			int pairIndex;
			for (channel = 0; channel < 2; ++channel) {
				for (pairIndex = 0; pairIndex < this.pairs[channel]; ++pairIndex) {
					this.frequencies[channel][0][pairIndex] = buffer.readUnsignedShort();
					this.magnitudes[channel][0][pairIndex] = buffer.readUnsignedShort();
				}
			}

			for (channel = 0; channel < 2; ++channel) {
				for (pairIndex = 0; pairIndex < this.pairs[channel]; ++pairIndex) {
					if ((modulationMask & 1 << channel * 4 << pairIndex) != 0) {
						this.frequencies[channel][1][pairIndex] = buffer.readUnsignedShort();
						this.magnitudes[channel][1][pairIndex] = buffer.readUnsignedShort();
					} else {
						this.frequencies[channel][1][pairIndex] = this.frequencies[channel][0][pairIndex];
						this.magnitudes[channel][1][pairIndex] = this.magnitudes[channel][0][pairIndex];
					}
				}
			}

			if (modulationMask != 0 || this.gains[1] != this.gains[0]) {
				envelope.decodeSegments(buffer);
			}
		} else {
			int[] gainArray = this.gains;
			this.gains[1] = 0;
			gainArray[0] = 0;
		}
	}

	static float normalize(float normalizedFrequency) {
		float frequency = 32.703197F * (float)Math.pow(2.0D, (double)normalizedFrequency);
		return frequency * 3.1415927F / 11025.0F;
	}
}
