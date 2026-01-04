public class SoundEffectDefinition {
	public int[][][] phases = new int[2][2][4];
	public int[] pairs = new int[2];
	public int[] unity = new int[2];
	public int[][][] magnitudes = new int[2][2][4];

	public static float[][] minCoefficients = new float[2][8];
	public static int[][] coefficients = new int[2][8];
	public static float forwardMinCoefficientMultiplier;
	public static int forwardMultiplier;

	public int compute(int channel, float position) {
		float value;
		int pairIndex;

		if (channel == 0) {
			value = (float) this.unity[0] + (float) (this.unity[1] - this.unity[0]) * position;
			value *= 0.0030517578f;
			forwardMinCoefficientMultiplier = (float) Math.pow(0.1, value / 20.0f);
			forwardMultiplier = (int) (forwardMinCoefficientMultiplier * 65536.0f);
		}

		if (this.pairs[channel] == 0) {
			return 0;
		}

		value = this.interpolateMagnitude(channel, 0, position);

		minCoefficients[channel][0] = -2.0f * value * (float) Math.cos(this.interpolatePhase(channel, 0, position));
		minCoefficients[channel][1] = value * value;

		for (pairIndex = 1; pairIndex < this.pairs[channel]; ++pairIndex) {
			value = this.interpolateMagnitude(channel, pairIndex, position);

			float cosineProduct = -2.0f * value * (float) Math.cos(this.interpolatePhase(channel, pairIndex, position));
			float squaredValue = value * value;

			minCoefficients[channel][pairIndex * 2 + 1] = minCoefficients[channel][pairIndex * 2 - 1] * squaredValue;
			minCoefficients[channel][pairIndex * 2] = minCoefficients[channel][pairIndex * 2 - 1] * cosineProduct + minCoefficients[channel][pairIndex * 2 - 2] * squaredValue;

			for (int coeffIndex = pairIndex * 2 - 1; coeffIndex >= 2; --coeffIndex) {
				float[] coeffArray = minCoefficients[channel];
				int index = coeffIndex;
				coeffArray[index] = coeffArray[index] + (minCoefficients[channel][coeffIndex - 1] * cosineProduct + minCoefficients[channel][coeffIndex - 2] * squaredValue);
			}

			float[] coeffArray = minCoefficients[channel];
			coeffArray[1] = coeffArray[1] + (minCoefficients[channel][0] * cosineProduct + squaredValue);

			float[] coeffArray2 = minCoefficients[channel];
			coeffArray2[0] = coeffArray2[0] + cosineProduct;
		}

		if (channel == 0) {
			pairIndex = 0;
			while (pairIndex < this.pairs[0] * 2) {
				float[] coeffArray = minCoefficients[0];
				int index = pairIndex++;
				coeffArray[index] = coeffArray[index] * forwardMinCoefficientMultiplier;
			}
		}

		for (pairIndex = 0; pairIndex < this.pairs[channel] * 2; ++pairIndex) {
			coefficients[channel][pairIndex] = (int) (minCoefficients[channel][pairIndex] * 65536.0f);
		}

		return this.pairs[channel] * 2;
	}

	public float interpolateMagnitude(int channel, int pairIndex, float position) {
		float interpolatedValue = (float) this.magnitudes[channel][0][pairIndex] + position * (float) (this.magnitudes[channel][1][pairIndex] - this.magnitudes[channel][0][pairIndex]);
		interpolatedValue *= 0.0015258789f;
		return 1.0f - (float) Math.pow(10.0, (-interpolatedValue) / 20.0f);
	}

	public float interpolatePhase(int channel, int pairIndex, float position) {
		float interpolatedValue = (float) this.phases[channel][0][pairIndex] + position * (float) (this.phases[channel][1][pairIndex] - this.phases[channel][0][pairIndex]);
		interpolatedValue *= 1.2207031E-4f;
		return normalise(interpolatedValue);
	}

	public static float normalise(float normalizedFrequency) {
		float frequency = 32.703197f * (float) Math.pow(2.0, normalizedFrequency);
		return frequency * 3.1415927f / 11025.0f;
	}
}
