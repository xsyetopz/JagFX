import java.util.Random;


public class Instrument {
	static int[] Instrument_samples;
	static int[] Instrument_noise;
	static int[] Instrument_sine;
	static int[] Instrument_phases;
	static int[] Instrument_delays;
	static int[] Instrument_volumeSteps;
	static int[] Instrument_pitchSteps;
	static int[] Instrument_pitchBaseSteps;
	SoundEnvelope pitch;
	SoundEnvelope volume;
	SoundEnvelope pitchModifier;
	SoundEnvelope pitchModifierAmplitude;
	SoundEnvelope volumeMultiplier;
	SoundEnvelope volumeMultiplierAmplitude;
	SoundEnvelope release;
	SoundEnvelope attack;
	int[] oscillatorVolume;
	int[] oscillatorPitch;
	int[] oscillatorDelays;
	int delayTime;
	int delayDecay;
	AudioFilter filter;
	SoundEnvelope filterEnvelope;
	int duration;
	int offset;

	static {
		Instrument_noise = new int[32768];
		Random random = new Random(0L);

		int index;
		for (index = 0; index < 32768; ++index) {
			Instrument_noise[index] = (random.nextInt() & 2) - 1;
		}

		Instrument_sine = new int[32768];

		for (index = 0; index < 32768; ++index) {
			Instrument_sine[index] = (int)(Math.sin((double)index / 5215.1903D) * 16384.0D);
		}

		Instrument_samples = new int[220500];
		Instrument_phases = new int[5];
		Instrument_delays = new int[5];
		Instrument_volumeSteps = new int[5];
		Instrument_pitchSteps = new int[5];
		Instrument_pitchBaseSteps = new int[5];
	}

	Instrument() {
		this.oscillatorVolume = new int[]{0, 0, 0, 0, 0};
		this.oscillatorPitch = new int[]{0, 0, 0, 0, 0};
		this.oscillatorDelays = new int[]{0, 0, 0, 0, 0};
		this.delayTime = 0;
		this.delayDecay = 100;
		this.duration = 500;
		this.offset = 0;
	}

	final int[] synthesize(int sampleCount, int duration) {
		ArrayUtils.clearIntArray(Instrument_samples, 0, sampleCount);
		if (duration < 10) {
			return Instrument_samples;
		} else {
			double sampleRate = (double)sampleCount / ((double)duration + 0.0D);
			this.pitch.reset();
			this.volume.reset();
			int pitchModRange = 0;
			int pitchModPhase = 0;
			int pitchModPhaseAccumulator = 0;
			if (this.pitchModifier != null) {
				this.pitchModifier.reset();
				this.pitchModifierAmplitude.reset();
				pitchModRange = (int)((double)(this.pitchModifier.end - this.pitchModifier.start) * 32.768D / sampleRate);
				pitchModPhase = (int)((double)this.pitchModifier.start * 32.768D / sampleRate);
			}

			int volumeModRange = 0;
			int volumeModPhase = 0;
			int volumeModPhaseAccumulator = 0;
			if (this.volumeMultiplier != null) {
				this.volumeMultiplier.reset();
				this.volumeMultiplierAmplitude.reset();
				volumeModRange = (int)((double)(this.volumeMultiplier.end - this.volumeMultiplier.start) * 32.768D / sampleRate);
				volumeModPhase = (int)((double)this.volumeMultiplier.start * 32.768D / sampleRate);
			}

			int oscillatorIndex;
			for (oscillatorIndex = 0; oscillatorIndex < 5; ++oscillatorIndex) {
				if (this.oscillatorVolume[oscillatorIndex] != 0) {
					Instrument_phases[oscillatorIndex] = 0;
					Instrument_delays[oscillatorIndex] = (int)((double)this.oscillatorDelays[oscillatorIndex] * sampleRate);
					Instrument_volumeSteps[oscillatorIndex] = (this.oscillatorVolume[oscillatorIndex] << 14) / 100;
					Instrument_pitchSteps[oscillatorIndex] = (int)((double)(this.pitch.end - this.pitch.start) * 32.768D * Math.pow(1.0057929410678534D, (double)this.oscillatorPitch[oscillatorIndex]) / sampleRate);
					Instrument_pitchBaseSteps[oscillatorIndex] = (int)((double)this.pitch.start * 32.768D / sampleRate);
				}
			}

			int pitchValue;
			int volumeValue;
			int modValue;
			int modAmplitude;
			int[] samplesArray;
			for (oscillatorIndex = 0; oscillatorIndex < sampleCount; ++oscillatorIndex) {
				pitchValue = this.pitch.doStep(sampleCount);
				volumeValue = this.volume.doStep(sampleCount);
				if (this.pitchModifier != null) {
					modValue = this.pitchModifier.doStep(sampleCount);
					modAmplitude = this.pitchModifierAmplitude.doStep(sampleCount);
					pitchValue += this.evaluateWave(pitchModPhaseAccumulator, modAmplitude, this.pitchModifier.form) >> 1;
					pitchModPhaseAccumulator = pitchModPhaseAccumulator + pitchModPhase + (modValue * pitchModRange >> 16);
				}

				if (this.volumeMultiplier != null) {
					modValue = this.volumeMultiplier.doStep(sampleCount);
					modAmplitude = this.volumeMultiplierAmplitude.doStep(sampleCount);
					volumeValue = volumeValue * ((this.evaluateWave(volumeModPhaseAccumulator, modAmplitude, this.volumeMultiplier.form) >> 1) + 32768) >> 15;
					volumeModPhaseAccumulator = volumeModPhaseAccumulator + volumeModPhase + (modValue * volumeModRange >> 16);
				}

				for (modValue = 0; modValue < 5; ++modValue) {
					if (this.oscillatorVolume[modValue] != 0) {
						modAmplitude = Instrument_delays[modValue] + oscillatorIndex;
						if (modAmplitude < sampleCount) {
							samplesArray = Instrument_samples;
							samplesArray[modAmplitude] += this.evaluateWave(Instrument_phases[modValue], volumeValue * Instrument_volumeSteps[modValue] >> 15, this.pitch.form);
							samplesArray = Instrument_phases;
							samplesArray[modValue] += (pitchValue * Instrument_pitchSteps[modValue] >> 16) + Instrument_pitchBaseSteps[modValue];
						}
					}
				}
			}

			int releaseValue;
			if (this.release != null) {
				this.release.reset();
				this.attack.reset();
				oscillatorIndex = 0;
				boolean releaseTrigger = false;
				boolean isAttackPhase = true;

				for (modValue = 0; modValue < sampleCount; ++modValue) {
					modAmplitude = this.release.doStep(sampleCount);
					releaseValue = this.attack.doStep(sampleCount);
					if (isAttackPhase) {
						pitchValue = (modAmplitude * (this.release.end - this.release.start) >> 8) + this.release.start;
					} else {
						pitchValue = (releaseValue * (this.release.end - this.release.start) >> 8) + this.release.start;
					}

					oscillatorIndex += 256;
					if (oscillatorIndex >= pitchValue) {
						oscillatorIndex = 0;
						isAttackPhase = !isAttackPhase;
					}

					if (isAttackPhase) {
						Instrument_samples[modValue] = 0;
					}
				}
			}

			if (this.delayTime > 0 && this.delayDecay > 0) {
				oscillatorIndex = (int)((double)this.delayTime * sampleRate);

				for (pitchValue = oscillatorIndex; pitchValue < sampleCount; ++pitchValue) {
					samplesArray = Instrument_samples;
					samplesArray[pitchValue] += Instrument_samples[pitchValue - oscillatorIndex] * this.delayDecay / 100;
				}
			}

			if (this.filter.pairs[0] > 0 || this.filter.pairs[1] > 0) {
				this.filterEnvelope.reset();
				oscillatorIndex = this.filterEnvelope.doStep(sampleCount + 1);
				pitchValue = this.filter.compute(0, (float)oscillatorIndex / 65536.0F);
				volumeValue = this.filter.compute(1, (float)oscillatorIndex / 65536.0F);
				if (sampleCount >= pitchValue + volumeValue) {
					modValue = 0;
					modAmplitude = volumeValue;
					if (volumeValue > sampleCount - pitchValue) {
						modAmplitude = sampleCount - pitchValue;
					}

					int filterCoeffIndex;
					while (modValue < modAmplitude) {
						releaseValue = (int)((long)Instrument_samples[modValue + pitchValue] * (long)AudioFilter.forwardMultiplier >> 16);

						for (filterCoeffIndex = 0; filterCoeffIndex < pitchValue; ++filterCoeffIndex) {
							releaseValue += (int)((long)Instrument_samples[modValue + pitchValue - 1 - filterCoeffIndex] * (long)AudioFilter.coefficients[0][filterCoeffIndex] >> 16);
						}

						for (filterCoeffIndex = 0; filterCoeffIndex < modValue; ++filterCoeffIndex) {
							releaseValue -= (int)((long)Instrument_samples[modValue - 1 - filterCoeffIndex] * (long)AudioFilter.coefficients[1][filterCoeffIndex] >> 16);
						}

						Instrument_samples[modValue] = releaseValue;
						oscillatorIndex = this.filterEnvelope.doStep(sampleCount + 1);
						++modValue;
					}

					boolean filterProcessing = true;
					modAmplitude = 128;

					while (true) {
						if (modAmplitude > sampleCount - pitchValue) {
							modAmplitude = sampleCount - pitchValue;
						}

						int backwardCoeffIndex;
						while (modValue < modAmplitude) {
							filterCoeffIndex = (int)((long)Instrument_samples[modValue + pitchValue] * (long)AudioFilter.forwardMultiplier >> 16);

							for (backwardCoeffIndex = 0; backwardCoeffIndex < pitchValue; ++backwardCoeffIndex) {
								filterCoeffIndex += (int)((long)Instrument_samples[modValue + pitchValue - 1 - backwardCoeffIndex] * (long)AudioFilter.coefficients[0][backwardCoeffIndex] >> 16);
							}

							for (backwardCoeffIndex = 0; backwardCoeffIndex < volumeValue; ++backwardCoeffIndex) {
								filterCoeffIndex -= (int)((long)Instrument_samples[modValue - 1 - backwardCoeffIndex] * (long)AudioFilter.coefficients[1][backwardCoeffIndex] >> 16);
							}

							Instrument_samples[modValue] = filterCoeffIndex;
							oscillatorIndex = this.filterEnvelope.doStep(sampleCount + 1);
							++modValue;
						}

						if (modValue >= sampleCount - pitchValue) {
							while (modValue < sampleCount) {
								filterCoeffIndex = 0;

								for (backwardCoeffIndex = modValue + pitchValue - sampleCount; backwardCoeffIndex < pitchValue; ++backwardCoeffIndex) {
									filterCoeffIndex += (int)((long)Instrument_samples[modValue + pitchValue - 1 - backwardCoeffIndex] * (long)AudioFilter.coefficients[0][backwardCoeffIndex] >> 16);
								}

								for (backwardCoeffIndex = 0; backwardCoeffIndex < volumeValue; ++backwardCoeffIndex) {
									filterCoeffIndex -= (int)((long)Instrument_samples[modValue - 1 - backwardCoeffIndex] * (long)AudioFilter.coefficients[1][backwardCoeffIndex] >> 16);
								}

								Instrument_samples[modValue] = filterCoeffIndex;
								this.filterEnvelope.doStep(sampleCount + 1);
								++modValue;
							}
							break;
						}

						pitchValue = this.filter.compute(0, (float)oscillatorIndex / 65536.0F);
						volumeValue = this.filter.compute(1, (float)oscillatorIndex / 65536.0F);
						modAmplitude += 128;
					}
				}
			}

			for (oscillatorIndex = 0; oscillatorIndex < sampleCount; ++oscillatorIndex) {
				if (Instrument_samples[oscillatorIndex] < -32768) {
					Instrument_samples[oscillatorIndex] = -32768;
				}

				if (Instrument_samples[oscillatorIndex] > 32767) {
					Instrument_samples[oscillatorIndex] = 32767;
				}
			}

			return Instrument_samples;
		}
	}

	final int evaluateWave(int phase, int amplitude, int waveform) {
		if (waveform == 1) {
			return (phase & 32767) < 16384 ? amplitude : -amplitude;
		} else if (waveform == 2) {
			return Instrument_sine[phase & 32767] * amplitude >> 14;
		} else if (waveform == 3) {
			return (amplitude * (phase & 32767) >> 14) - amplitude;
		} else {
			return waveform == 4 ? amplitude * Instrument_noise[phase / 2607 & 32767] : 0;
		}
	}

	final void decode(Buffer buffer) {
		this.pitch = new SoundEnvelope();
		this.pitch.decode(buffer);
		this.volume = new SoundEnvelope();
		this.volume.decode(buffer);
		int hasPitchModifier = buffer.readUnsignedByte();
		if (hasPitchModifier != 0) {
			--buffer.offset;
			this.pitchModifier = new SoundEnvelope();
			this.pitchModifier.decode(buffer);
			this.pitchModifierAmplitude = new SoundEnvelope();
			this.pitchModifierAmplitude.decode(buffer);
		}

		hasPitchModifier = buffer.readUnsignedByte();
		if (hasPitchModifier != 0) {
			--buffer.offset;
			this.volumeMultiplier = new SoundEnvelope();
			this.volumeMultiplier.decode(buffer);
			this.volumeMultiplierAmplitude = new SoundEnvelope();
			this.volumeMultiplierAmplitude.decode(buffer);
		}

		hasPitchModifier = buffer.readUnsignedByte();
		if (hasPitchModifier != 0) {
			--buffer.offset;
			this.release = new SoundEnvelope();
			this.release.decode(buffer);
			this.attack = new SoundEnvelope();
			this.attack.decode(buffer);
		}

		for (int oscillatorIndex = 0; oscillatorIndex < 10; ++oscillatorIndex) {
			int oscillatorVolumeValue = buffer.readUShortSmart();
			if (oscillatorVolumeValue == 0) {
				break;
			}

			this.oscillatorVolume[oscillatorIndex] = oscillatorVolumeValue;
			this.oscillatorPitch[oscillatorIndex] = buffer.readShortSmart();
			this.oscillatorDelays[oscillatorIndex] = buffer.readUShortSmart();
		}

		this.delayTime = buffer.readUShortSmart();
		this.delayDecay = buffer.readUShortSmart();
		this.duration = buffer.readUnsignedShort();
		this.offset = buffer.readUnsignedShort();
		this.filter = new AudioFilter();
		this.filterEnvelope = new SoundEnvelope();
		this.filter.decode(buffer, this.filterEnvelope);
	}
}
