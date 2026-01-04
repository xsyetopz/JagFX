public class AudioEnvelopeDefinition {
	public int segments = 2;
	public int[] durations = new int[2];
	public int[] phases = new int[2];
	public int start;
	public int end;
	public int form;
	public int ticks;
	public int phaseIndex;
	public int step;
	public int amplitude;
	public int max;

	public AudioEnvelopeDefinition() {
		this.durations[0] = 0;
		this.durations[1] = '\uffff';
		this.phases[0] = 0;
		this.phases[1] = '\uffff';
	}

	public final int step(int sampleCount) {
		if (this.max >= this.ticks) {
			this.amplitude = this.phases[this.phaseIndex++] << 15;

			if (this.phaseIndex >= this.segments) {
				this.phaseIndex = this.segments - 1;
			}

			this.ticks = (int) ((double) this.durations[this.phaseIndex] / 65536.0 * (double) sampleCount);

			if (this.ticks > this.max) {
				this.step = ((this.phases[this.phaseIndex] << 15) - this.amplitude) / (this.ticks - this.max);
			}
		}
		this.amplitude += this.step;
		++this.max;

		return this.amplitude - this.step >> 15;
	}

	public final void reset() {
		this.ticks = 0;
		this.phaseIndex = 0;
		this.step = 0;
		this.amplitude = 0;
		this.max = 0;
	}
}
