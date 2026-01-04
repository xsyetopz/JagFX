public class RawSound extends AbstractSound {
	public int sampleRate;
	public byte[] samples;
	public int start;
	int end;
	public boolean field261;

	RawSound(int sampleRate, byte[] samples, int start, int end) {
		this.sampleRate = sampleRate;
		this.samples = samples;
		this.start = start;
		this.end = end;
	}

	RawSound(int sampleRate, byte[] samples, int start, int end, boolean field261) {
		this.sampleRate = sampleRate;
		this.samples = samples;
		this.start = start;
		this.end = end;
		this.field261 = field261;
	}

	public RawSound resample(Decimator decimator) {
		this.samples = decimator.resample(this.samples);
		this.sampleRate = decimator.scaleRate(this.sampleRate);
		if (this.start == this.end) {
			this.start = this.end = decimator.scalePosition(this.start);
		} else {
			this.start = decimator.scalePosition(this.start);
			this.end = decimator.scalePosition(this.end);
			if (this.start == this.end) {
				--this.start;
			}
		}

		return this;
	}
}
