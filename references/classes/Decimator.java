public class Decimator {
	static int field402;
	int inputRate;
	int outputRate;
	int[][] table;

	public Decimator(int inputRate, int outputRate) {
		if (outputRate != inputRate) {
			int gcdA = inputRate;
			int gcdB = outputRate;
			if (outputRate > inputRate) {
				gcdA = outputRate;
				gcdB = inputRate;
			}

			while (gcdB != 0) {
				int remainder = gcdA % gcdB;
				gcdA = gcdB;
				gcdB = remainder;
			}

			inputRate /= gcdA;
			outputRate /= gcdA;
			this.inputRate = inputRate;
			this.outputRate = outputRate;
			this.table = new int[inputRate][14];

			for (int phaseIndex = 0; phaseIndex < inputRate; ++phaseIndex) {
				int[] coefficients = this.table[phaseIndex];
				double phaseOffset = (double)phaseIndex / (double)inputRate + 6.0D;
				int startIndex = (int)Math.floor(1.0D + (phaseOffset - 7.0D));
				if (startIndex < 0) {
					startIndex = 0;
				}

				int endIndex = (int)Math.ceil(phaseOffset + 7.0D);
				if (endIndex > 14) {
					endIndex = 14;
				}

				for (double scale = (double)outputRate / (double)inputRate; startIndex < endIndex; ++startIndex) {
					double distance = ((double)startIndex - phaseOffset) * 3.141592653589793D;
					double windowedSinc = scale;
					if (distance < -1.0E-4D || distance > 1.0E-4D) {
						windowedSinc = scale * (Math.sin(distance) / distance);
					}

					windowedSinc *= 0.54D + 0.46D * Math.cos(0.2243994752564138D * ((double)startIndex - phaseOffset));
					coefficients[startIndex] = (int)Math.floor(0.5D + 65536.0D * windowedSinc);
				}
			}
		}
	}

	byte[] resample(byte[] samples) {
		if (this.table != null) {
			int newLength = (int)((long)this.outputRate * (long)samples.length / (long)this.inputRate) + 14;
			int[] tempBuffer = new int[newLength];
			int outputIndex = 0;
			int phaseIndex = 0;

			int inputIndex;
			for (inputIndex = 0; inputIndex < samples.length; ++inputIndex) {
				byte sample = samples[inputIndex];
				int[] coefficients = this.table[phaseIndex];

				int coeffIndex;
				for (coeffIndex = 0; coeffIndex < 14; ++coeffIndex) {
					tempBuffer[coeffIndex + outputIndex] += coefficients[coeffIndex] * sample;
				}

				phaseIndex += this.outputRate;
				coeffIndex = phaseIndex / this.inputRate;
				outputIndex += coeffIndex;
				phaseIndex -= coeffIndex * this.inputRate;
			}

			samples = new byte[newLength];

			for (inputIndex = 0; inputIndex < newLength; ++inputIndex) {
				int clampedValue = tempBuffer[inputIndex] + 32768 >> 16;
				if (clampedValue < -128) {
					samples[inputIndex] = -128;
				} else if (clampedValue > 127) {
					samples[inputIndex] = 127;
				} else {
					samples[inputIndex] = (byte)clampedValue;
				}
			}
		}

		return samples;
	}

	int scaleRate(int rate) {
		if (this.table != null) {
			rate = (int)((long)this.outputRate * (long)rate / (long)this.inputRate);
		}

		return rate;
	}

	int scalePosition(int position) {
		if (this.table != null) {
			position = (int)((long)position * (long)this.outputRate / (long)this.inputRate) + 6;
		}

		return position;
	}
}
