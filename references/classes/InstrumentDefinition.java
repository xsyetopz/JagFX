import java.util.Random;

public class InstrumentDefinition {
	public AudioEnvelopeDefinition volume;
	public AudioEnvelopeDefinition pitchModifier;
	public AudioEnvelopeDefinition attack;
	public AudioEnvelopeDefinition release;
	public AudioEnvelopeDefinition volumeMultiplier;
	public AudioEnvelopeDefinition volumeMultiplierAmplitude;
	public AudioEnvelopeDefinition pitchModifierAmplitude;
	public AudioEnvelopeDefinition pitch;

	public int[] oscillatorDelays = new int[]{0, 0, 0, 0, 0};
	public int[] oscillatorPitch = new int[]{0, 0, 0, 0, 0};
	public int[] oscillatorVolume = new int[]{0, 0, 0, 0, 0};

	public SoundEffectDefinition filter;
	public AudioEnvelopeDefinition filterEnvelope;

	static int[] samples = new int[220500];

	static int[] NOISE = new int[32768];
	static int[] AUDIO_SINE = new int[32768];

	static int[] phases = new int[5];
	static int[] delays = new int[5];

	static int[] volumeSteps = new int[5];
	static int[] pitchSteps = new int[5];
	static int[] pitchBaseSteps = new int[5];

	public int duration = 500;
	public int delayDecay = 100;
	public int delayTime = 0;
	public int offset = 0;

	static {
		Random random = new Random(0);

		for (int i = 0; i < 32768; ++i) {
			InstrumentDefinition.NOISE[i] = (random.nextInt() & 2) - 1;
			InstrumentDefinition.AUDIO_SINE[i] = (int) (Math.sin((double) i / 5215.1903) * 16384.0);
		}
	}

	public final int[] synthesize(int sampleCount, int duration) {
		int releaseValue;
		int modAmplitude;
		int modValue;
		int oscillatorIndex;
		int pitchValue;
		int volumeValue;

		clearIntArray(samples, 0, sampleCount);

		if (duration < 10) {
			return samples;
		}

		double sampleRate = (double) sampleCount / ((double) duration + 0.0);

		this.pitch.reset();
		this.volume.reset();

		int pitchModRange = 0;
		int pitchModPhase = 0;
		int pitchModPhaseAccumulator = 0;

		if (this.pitchModifier != null) {
			this.pitchModifier.reset();
			this.pitchModifierAmplitude.reset();

			pitchModRange = (int) ((double) (this.pitchModifier.end - this.pitchModifier.start) * 32.768 / sampleRate);
			pitchModPhase = (int) ((double) this.pitchModifier.start * 32.768 / sampleRate);
		}

		int volumeModRange = 0;
		int volumeModPhase = 0;
		int volumeModPhaseAccumulator = 0;

		if (this.volumeMultiplier != null) {
			this.volumeMultiplier.reset();
			this.volumeMultiplierAmplitude.reset();

			volumeModRange = (int) ((double) (this.volumeMultiplier.end - this.volumeMultiplier.start) * 32.768 / sampleRate);
			volumeModPhase = (int) ((double) this.volumeMultiplier.start * 32.768 / sampleRate);
		}

		for (oscillatorIndex = 0; oscillatorIndex < 5; ++oscillatorIndex) {
			if (this.oscillatorVolume[oscillatorIndex] == 0) {
				continue;
			}

			InstrumentDefinition.phases[oscillatorIndex] = 0;
			InstrumentDefinition.delays[oscillatorIndex] = (int) ((double) this.oscillatorDelays[oscillatorIndex] * sampleRate);
			InstrumentDefinition.volumeSteps[oscillatorIndex] = (this.oscillatorVolume[oscillatorIndex] << 14) / 100;
			InstrumentDefinition.pitchSteps[oscillatorIndex] = (int) ((double) (this.pitch.end - this.pitch.start) * 32.768 * Math.pow(1.0057929410678534, this.oscillatorPitch[oscillatorIndex]) / sampleRate);
			InstrumentDefinition.pitchBaseSteps[oscillatorIndex] = (int) ((double) this.pitch.start * 32.768 / sampleRate);
		}

		for (oscillatorIndex = 0; oscillatorIndex < sampleCount; ++oscillatorIndex) {
			pitchValue = this.pitch.step(sampleCount);
			volumeValue = this.volume.step(sampleCount);

			if (this.pitchModifier != null) {
				modValue = this.pitchModifier.step(sampleCount);
				modAmplitude = this.pitchModifierAmplitude.step(sampleCount);
				pitchValue += this.evaluateWave(pitchModPhaseAccumulator, modAmplitude, this.pitchModifier.form) >> 1;
				pitchModPhaseAccumulator = pitchModPhaseAccumulator + pitchModPhase + (modValue * pitchModRange >> 16);
			}

			if (this.volumeMultiplier != null) {
				modValue = this.volumeMultiplier.step(sampleCount);
				modAmplitude = this.volumeMultiplierAmplitude.step(sampleCount);
				volumeValue = volumeValue * ((this.evaluateWave(volumeModPhaseAccumulator, modAmplitude, this.volumeMultiplier.form) >> 1) + 32768) >> 15;
				volumeModPhaseAccumulator = volumeModPhaseAccumulator + volumeModPhase + (modValue * volumeModRange >> 16);
			}

			for (modValue = 0; modValue < 5; ++modValue) {
				if (this.oscillatorVolume[modValue] == 0 || (modAmplitude = delays[modValue] + oscillatorIndex) >= sampleCount) {
					continue;
				}

				int[] samplesArray = samples;
				int index = modAmplitude;
				samplesArray[index] = samplesArray[index] + this.evaluateWave(phases[modValue], volumeValue * volumeSteps[modValue] >> 15, this.pitch.form);
				int[] phasesArray = phases;
				int phaseIndex = modValue;
				phasesArray[phaseIndex] = phasesArray[phaseIndex] + ((pitchValue * pitchSteps[modValue] >> 16) + pitchBaseSteps[modValue]);
			}
		}

		if (this.release != null) {
			this.release.reset();
			this.attack.reset();

			oscillatorIndex = 0;
			boolean isAttackPhase = true;

			for (modValue = 0; modValue < sampleCount; ++modValue) {
				modAmplitude = this.release.step(sampleCount);
				releaseValue = this.attack.step(sampleCount);
				pitchValue = isAttackPhase ? (modAmplitude * (this.release.end - this.release.start) >> 8) + this.release.start : (releaseValue * (this.release.end - this.release.start) >> 8) + this.release.start;
				if ((oscillatorIndex += 256) >= pitchValue) {
					oscillatorIndex = 0;
					isAttackPhase = !isAttackPhase;
				}
				if (!isAttackPhase) {
					continue;
				}
				InstrumentDefinition.samples[modValue] = 0;
			}
		}

		if (this.delayTime > 0 && this.delayDecay > 0) {
			for (pitchValue = oscillatorIndex = (int) ((double) this.delayTime * sampleRate); pitchValue < sampleCount; ++pitchValue) {
				int[] samplesArray = samples;
				int index = pitchValue;
				samplesArray[index] = samplesArray[index] + samples[pitchValue - oscillatorIndex] * this.delayDecay / 100;
			}
		}

		if (this.filter.pairs[0] > 0 || this.filter.pairs[1] > 0) {
			this.filterEnvelope.reset();

			oscillatorIndex = this.filterEnvelope.step(sampleCount + 1);
			pitchValue = this.filter.compute(0, (float) oscillatorIndex / 65536.0f);
			volumeValue = this.filter.compute(1, (float) oscillatorIndex / 65536.0f);

			if (sampleCount >= pitchValue + volumeValue) {
				int filterCoeffIndex;

				modValue = 0;
				modAmplitude = volumeValue;

				if (volumeValue > sampleCount - pitchValue) {
					modAmplitude = sampleCount - pitchValue;
				}

				while (modValue < modAmplitude) {
					releaseValue = (int) ((long) samples[modValue + pitchValue] * (long) SoundEffectDefinition.forwardMultiplier >> 16);

					for (filterCoeffIndex = 0; filterCoeffIndex < pitchValue; ++filterCoeffIndex) {
						releaseValue += (int) ((long) samples[modValue + pitchValue - 1 - filterCoeffIndex] * (long) SoundEffectDefinition.coefficients[0][filterCoeffIndex] >> 16);
					}

					for (filterCoeffIndex = 0; filterCoeffIndex < modValue; ++filterCoeffIndex) {
						releaseValue -= (int) ((long) samples[modValue - 1 - filterCoeffIndex] * (long) SoundEffectDefinition.coefficients[1][filterCoeffIndex] >> 16);
					}

					InstrumentDefinition.samples[modValue] = releaseValue;
					oscillatorIndex = this.filterEnvelope.step(sampleCount + 1);
					++modValue;
				}

				modAmplitude = 128;

				do {
					int backwardCoeffIndex;

					if (modAmplitude > sampleCount - pitchValue) {
						modAmplitude = sampleCount - pitchValue;
					}

					while (modValue < modAmplitude) {
						filterCoeffIndex = (int) ((long) samples[modValue + pitchValue] * (long) SoundEffectDefinition.forwardMultiplier >> 16);

						for (backwardCoeffIndex = 0; backwardCoeffIndex < pitchValue; ++backwardCoeffIndex) {
							filterCoeffIndex += (int) ((long) samples[modValue + pitchValue - 1 - backwardCoeffIndex] * (long) SoundEffectDefinition.coefficients[0][backwardCoeffIndex] >> 16);
						}

						for (backwardCoeffIndex = 0; backwardCoeffIndex < volumeValue; ++backwardCoeffIndex) {
							filterCoeffIndex -= (int) ((long) samples[modValue - 1 - backwardCoeffIndex] * (long) SoundEffectDefinition.coefficients[1][backwardCoeffIndex] >> 16);
						}

						InstrumentDefinition.samples[modValue] = filterCoeffIndex;
						oscillatorIndex = this.filterEnvelope.step(sampleCount + 1);
						++modValue;
					}

					if (modValue >= sampleCount - pitchValue) {
						while (modValue < sampleCount) {
							filterCoeffIndex = 0;
							for (backwardCoeffIndex = modValue + pitchValue - sampleCount; backwardCoeffIndex < pitchValue; ++backwardCoeffIndex) {
								filterCoeffIndex += (int) ((long) samples[modValue + pitchValue - 1 - backwardCoeffIndex] * (long) SoundEffectDefinition.coefficients[0][backwardCoeffIndex] >> 16);
							}
							for (backwardCoeffIndex = 0; backwardCoeffIndex < volumeValue; ++backwardCoeffIndex) {
								filterCoeffIndex -= (int) ((long) samples[modValue - 1 - backwardCoeffIndex] * (long) SoundEffectDefinition.coefficients[1][backwardCoeffIndex] >> 16);
							}
							InstrumentDefinition.samples[modValue] = filterCoeffIndex;
							this.filterEnvelope.step(sampleCount + 1);
							++modValue;
						}
						break;
					}
					pitchValue = this.filter.compute(0, (float) oscillatorIndex / 65536.0f);
					volumeValue = this.filter.compute(1, (float) oscillatorIndex / 65536.0f);
					modAmplitude += 128;
				} while (true);
			}
		}

		for (oscillatorIndex = 0; oscillatorIndex < sampleCount; ++oscillatorIndex) {
			if (samples[oscillatorIndex] < -32768) {
				InstrumentDefinition.samples[oscillatorIndex] = -32768;
			}

			if (samples[oscillatorIndex] <= 32767) {
				continue;
			}

			InstrumentDefinition.samples[oscillatorIndex] = 32767;
		}

		return samples;
	}

	private static void clearIntArray(int[] array, int offset, int length) {
		length = length + offset - 7;

		while (offset < length) {
			array[offset++] = 0;
			array[offset++] = 0;
			array[offset++] = 0;
			array[offset++] = 0;
			array[offset++] = 0;
			array[offset++] = 0;
			array[offset++] = 0;
			array[offset++] = 0;
		}

		while (offset < (length += 7)) {
			array[offset++] = 0;
		}
	}

	public final int evaluateWave(int phase, int amplitude, int waveform) {
		return waveform == 1 ? ((phase & 32767) < 16384 ? amplitude : -amplitude) : (waveform == 2 ? AUDIO_SINE[phase & 32767] * amplitude >> 14 : (waveform == 3 ? (amplitude * (phase & 32767) >> 14) - amplitude : (waveform == 4 ? amplitude * NOISE[phase / 2607 & 32767] : 0)));
	}
}
