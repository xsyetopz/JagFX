using JagFX.Domain;
using JagFX.Domain.Models;
using JagFX.Domain.Utilities;

namespace JagFX.Synthesis;

public static class ToneSynthesizer
{
    public static SampleBuffer Synthesize(Voice voice)
    {
        var sampleCount = (int)(voice.Duration * Constants.SampleRatePerMillisecond);
        if (sampleCount <= 0 || voice.Duration < 10)
        {
            return SampleBuffer.Empty(0);
        }

        var samplesPerStep = sampleCount / (double)voice.Duration;
        var buffer = SampleBufferPool.Acquire(sampleCount);

        var state = InitSynthState(voice, samplesPerStep);
        RenderSamples(buffer, voice, state, sampleCount);

        ApplyGating(buffer, voice, sampleCount);
        ApplyEcho(buffer, voice, samplesPerStep, sampleCount);

        if (voice.Filter != null)
        {
            FilterProcessor.Apply(buffer, voice.Filter, state.FilterEnvelopeEval, sampleCount);
        }

        MathUtils.ClipInt16(buffer, sampleCount);

        var output = new int[sampleCount];
        Array.Copy(buffer, 0, output, 0, sampleCount);

        SampleBufferPool.Release(buffer);
        return new SampleBuffer(output, Constants.SampleRate);
    }

    private static SynthesisState InitSynthState(Voice voice, double samplesPerStep)
    {
        var freqBaseEval = new EnvelopeEvaluator(voice.FrequencyEnvelope);
        var ampBaseEval = new EnvelopeEvaluator(voice.AmplitudeEnvelope);
        freqBaseEval.Reset();
        ampBaseEval.Reset();

        var (freqModRateEval, freqModRangeEval, vibratoStart, vibratoDuration) =
            InitFrequencyModulation(voice, samplesPerStep);

        var (ampModRateEval, ampModRangeEval, amplitudeStart, amplitudeDuration) =
            InitAmplitudeModulation(voice, samplesPerStep);

        var (delays, volumes, semitones, starts) =
            InitOscillators(voice, samplesPerStep);

        EnvelopeEvaluator? filterEnvelopeEval = null;
        if (voice.Filter != null && voice.Filter.Envelope != null)
        {
            filterEnvelopeEval = new EnvelopeEvaluator(voice.Filter.Envelope);
            filterEnvelopeEval.Reset();
        }

        return new SynthesisState
        {
            FreqBaseEval = freqBaseEval,
            AmpBaseEval = ampBaseEval,
            FreqModRateEval = freqModRateEval,
            FreqModRangeEval = freqModRangeEval,
            AmpModRateEval = ampModRateEval,
            AmpModRangeEval = ampModRangeEval,
            FrequencyStart = vibratoStart,
            FrequencyDuration = vibratoDuration,
            AmplitudeStart = amplitudeStart,
            AmplitudeDuration = amplitudeDuration,
            PartialDelays = delays,
            PartialVolumes = volumes,
            PartialSemitones = semitones,
            PartialStarts = starts,
            FilterEnvelopeEval = filterEnvelopeEval
        };
    }

    private static (EnvelopeEvaluator? rateEval, EnvelopeEvaluator? rangeEval, int start, int duration)
        InitFrequencyModulation(Voice voice, double samplesPerStep)
    {
        if (voice.PitchLfo != null)
        {
            var rateEval = new EnvelopeEvaluator(voice.PitchLfo.Rate);
            var rangeEval = new EnvelopeEvaluator(voice.PitchLfo.Depth);
            rateEval.Reset();
            rangeEval.Reset();

            var start = (int)((double)(voice.PitchLfo.Rate.End - voice.PitchLfo.Rate.Start) * Constants.PhaseScale / samplesPerStep);
            var duration = (int)(voice.PitchLfo.Rate.Start * Constants.PhaseScale / samplesPerStep);

            return (rateEval, rangeEval, start, duration);
        }

        return (null, null, 0, 0);
    }

    private static (EnvelopeEvaluator? rateEval, EnvelopeEvaluator? rangeEval, int start, int duration)
        InitAmplitudeModulation(Voice voice, double samplesPerStep)
    {
        if (voice.AmplitudeLfo != null)
        {
            var rateEval = new EnvelopeEvaluator(voice.AmplitudeLfo.Rate);
            var rangeEval = new EnvelopeEvaluator(voice.AmplitudeLfo.Depth);
            rateEval.Reset();
            rangeEval.Reset();

            var start = (int)((double)(voice.AmplitudeLfo.Rate.End - voice.AmplitudeLfo.Rate.Start) * Constants.PhaseScale / samplesPerStep);
            var duration = (int)(voice.AmplitudeLfo.Rate.Start * Constants.PhaseScale / samplesPerStep);

            return (rateEval, rangeEval, start, duration);
        }

        return (null, null, 0, 0);
    }

    private static (int[] delays, int[] volumes, int[] semitones, int[] starts)
        InitOscillators(Voice voice, double samplesPerStep)
    {
        var delays = new int[Constants.MaxOscillators];
        var volumes = new int[Constants.MaxOscillators];
        var semitones = new int[Constants.MaxOscillators];
        var starts = new int[Constants.MaxOscillators];

        var oscillatorCount = Math.Min(Constants.MaxOscillators, voice.Oscillators.Count);

        for (var oscillator = 0; oscillator < oscillatorCount; oscillator++)
        {
            var height = voice.Oscillators[oscillator];
            if (height.Amplitude.Value != 0)
            {
                delays[oscillator] = (int)(height.Delay.Value * samplesPerStep);
                volumes[oscillator] = (height.Amplitude.Value << 14) / 100;
                semitones[oscillator] = (int)(
                    (voice.FrequencyEnvelope.End - voice.FrequencyEnvelope.Start) * Constants.PhaseScale *
                    LookupTables.GetPitchMultiplier(height.PitchOffset) / samplesPerStep);
                starts[oscillator] = (int)(voice.FrequencyEnvelope.Start * Constants.PhaseScale / samplesPerStep);

                if (oscillator == 0)
                {
                    _ = LookupTables.GetPitchMultiplier(height.PitchOffset);
                }
                else if (oscillator == 1)
                {
                    _ = LookupTables.GetPitchMultiplier(height.PitchOffset);
                }
            }
        }

        return (delays, volumes, semitones, starts);
    }

    private static void RenderSamples(
        int[] buffer,
        Voice voice,
        SynthesisState state,
        int sampleCount)
    {
        var phases = new int[Constants.MaxOscillators];
        var frequencyPhase = 0;
        var amplitudePhase = 0;

        for (var sample = 0; sample < sampleCount; sample++)
        {
            var frequency = state.FreqBaseEval.Evaluate(sampleCount);
            var amplitude = state.AmpBaseEval.Evaluate(sampleCount);

            (frequency, frequencyPhase) = ApplyVibrato(frequency, frequencyPhase, sampleCount, state, voice);
            (amplitude, amplitudePhase) = ApplyTremolo(amplitude, amplitudePhase, sampleCount, state, voice);

            RenderOscillators(
                buffer,
                voice,
                state,
                sample,
                sampleCount,
                frequency,
                amplitude,
                phases);
        }
    }

    private static void RenderOscillators(
        int[] buffer,
        Voice voice,
        SynthesisState state,
        int sample,
        int sampleCount,
        int frequency,
        int amplitude,
        int[] phases)
    {
        var oscillatorCount = Math.Min(Constants.MaxOscillators, voice.Oscillators.Count);
        for (var oscillator = 0; oscillator < oscillatorCount; oscillator++)
        {
            if (voice.Oscillators[oscillator].Amplitude.Value != 0)
            {
                var position = sample + state.PartialDelays[oscillator];
                if (position >= 0 && position < sampleCount)
                {
                    var sampleValue = GenerateSample(
                        amplitude * state.PartialVolumes[oscillator] >> 15,
                        phases[oscillator],
                        voice.FrequencyEnvelope.Waveform);

                    buffer[position] += sampleValue;
                    phases[oscillator] += (frequency * state.PartialSemitones[oscillator] >> 16) +
                        state.PartialStarts[oscillator];
                }
            }
        }
    }

    private static (int frequency, int phase) ApplyVibrato(
        int frequency,
        int phase,
        int sampleCount,
        SynthesisState state,
        Voice voice)
    {
        if (state.FreqModRateEval != null && state.FreqModRangeEval != null)
        {
            var rate = state.FreqModRateEval.Evaluate(sampleCount);
            var range = state.FreqModRangeEval.Evaluate(sampleCount);
            var mod = GenerateSample(range, phase, voice.PitchLfo!.Rate.Waveform) >> 1;
            var nextPhase = phase + (rate * state.FrequencyStart >> 16) + state.FrequencyDuration;

            return (frequency + mod, nextPhase);
        }

        return (frequency, phase);
    }

    private static (int amplitude, int phase) ApplyTremolo(
        int amplitude,
        int phase,
        int sampleCount,
        SynthesisState state,
        Voice voice)
    {
        if (state.AmpModRateEval != null && state.AmpModRangeEval != null)
        {
            var rate = state.AmpModRateEval.Evaluate(sampleCount);
            var range = state.AmpModRangeEval.Evaluate(sampleCount);
            var mod = GenerateSample(range, phase, voice.AmplitudeLfo!.Rate.Waveform) >> 1;

            var newAmp = amplitude * (mod + Constants.FixedPoint.Offset) >> 15;
            var nextPhase = phase + (rate * state.AmplitudeStart >> 16) + state.AmplitudeDuration;
            return (newAmp, nextPhase);
        }

        return (amplitude, phase);
    }

    private static int GenerateSample(int amplitude, int phase, Waveform waveform)
    {
        return waveform switch
        {
            Waveform.Square => (phase & Constants.PhaseMask) < Constants.FixedPoint.Quarter ? amplitude : -amplitude,
            Waveform.Sine => (LookupTables.SinTable[phase & Constants.PhaseMask] * amplitude) >> 14,
            Waveform.Saw => (((phase & Constants.PhaseMask) * amplitude) >> 14) - amplitude,
            Waveform.Noise => LookupTables.NoiseTable[(phase / Constants.NoisePhaseDiv) & Constants.PhaseMask] * amplitude,
            Waveform.Off => 0,
            _ => 0
        };
    }

    private static void ApplyGating(int[] buffer, Voice voice, int sampleCount)
    {
        if (voice.GateSilence != null && voice.GateDuration != null)
        {
            var silenceEval = new EnvelopeEvaluator(voice.GateSilence);
            var durationEval = new EnvelopeEvaluator(voice.GateDuration);
            silenceEval.Reset();
            durationEval.Reset();

            var counter = 0;
            var muted = true;

            for (var sample = 0; sample < sampleCount; sample++)
            {
                var stepOn = silenceEval.Evaluate(sampleCount);
                var stepOff = durationEval.Evaluate(sampleCount);
                var threshold = muted
                    ? voice.GateSilence.Start + ((voice.GateSilence.End - voice.GateSilence.Start) * stepOn >> 8)
                    : voice.GateSilence.Start + ((voice.GateSilence.End - voice.GateSilence.Start) * stepOff >> 8);

                counter += 256;
                if (counter >= threshold)
                {
                    counter = 0;
                    muted = !muted;
                }

                if (muted)
                {
                    buffer[sample] = 0;
                }
            }
        }
    }

    private static void ApplyEcho(int[] buffer, Voice voice, double samplesPerStep, int sampleCount)
    {
        if (voice.FeedbackDelay.Delay > 0 && voice.FeedbackDelay.Mix > 0)
        {
            var start = (int)(voice.FeedbackDelay.Delay * samplesPerStep);
            for (var sample = start; sample < sampleCount; sample++)
            {
                buffer[sample] += buffer[sample - start] * voice.FeedbackDelay.Mix / 100;
            }
        }
    }

    private class SynthesisState
    {
        public required EnvelopeEvaluator FreqBaseEval { get; init; }
        public required EnvelopeEvaluator AmpBaseEval { get; init; }
        public EnvelopeEvaluator? FreqModRateEval { get; init; }
        public EnvelopeEvaluator? FreqModRangeEval { get; init; }
        public EnvelopeEvaluator? AmpModRateEval { get; init; }
        public EnvelopeEvaluator? AmpModRangeEval { get; init; }
        public int FrequencyStart { get; init; }
        public int FrequencyDuration { get; init; }
        public int AmplitudeStart { get; init; }
        public int AmplitudeDuration { get; init; }
        public int[] PartialDelays { get; init; } = null!;
        public int[] PartialVolumes { get; init; } = null!;
        public int[] PartialSemitones { get; init; } = null!;
        public int[] PartialStarts { get; init; } = null!;
        public EnvelopeEvaluator? FilterEnvelopeEval { get; init; }
    }
}
