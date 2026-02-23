using JagFX.Core.Constants;
using JagFX.Core.Types;
using JagFX.Domain;
using JagFX.Domain.Models;
using JagFX.Domain.Utilities;
using JagFX.Synthesis.Data;
using JagFX.Synthesis.Processing;

namespace JagFX.Synthesis.Core;

public static class VoiceSynthesizer
{
    public static AudioBuffer Synthesize(Voice voice)
    {
        var sampleCount = (int)(voice.Duration * AudioConstants.SampleRatePerMillisecond);
        if (sampleCount <= 0 || voice.Duration < 10)
        {
            return AudioBuffer.Empty(0);
        }

        var samplesPerStep = sampleCount / (double)voice.Duration;
        var buffer = AudioBufferPool.Acquire(sampleCount);

        var state = InitSynthState(voice, samplesPerStep);
        RenderSamples(buffer, voice, state, sampleCount);

        ApplyGating(buffer, voice, sampleCount);
        ApplyEcho(buffer, voice, samplesPerStep, sampleCount);

        if (voice.Filter != null)
        {
            AudioFilter.Apply(buffer, voice.Filter, state.FilterEnvelopeEval, sampleCount);
        }

        AudioMath.ClipInt16(buffer, sampleCount);

        var output = new int[sampleCount];
        Array.Copy(buffer, 0, output, 0, sampleCount);

        AudioBufferPool.Release(buffer);
        return new AudioBuffer(output, AudioConstants.SampleRate);
    }

    private static SynthesisState InitSynthState(Voice voice, double samplesPerStep)
    {
        var freqBaseEval = new EnvelopeGenerator(voice.FrequencyEnvelope);
        var ampBaseEval = new EnvelopeGenerator(voice.AmplitudeEnvelope);
        freqBaseEval.Reset();
        ampBaseEval.Reset();

        var (freqModRateEval, freqModRangeEval, vibratoStep, vibratoBase) =
            InitFrequencyModulation(voice, samplesPerStep);
        var (ampModRateEval, ampModRangeEval, tremoloStep, tremoloBase) =
            InitAmplitudeModulation(voice, samplesPerStep);

        var (delays, volumes, semitones, starts) =
            InitOscillators(voice, samplesPerStep);

        EnvelopeGenerator? filterEnvelopeEval = null;
        if (voice.Filter != null && voice.Filter.Envelope != null)
        {
            filterEnvelopeEval = new EnvelopeGenerator(voice.Filter.Envelope);
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
            FrequencyStep = vibratoStep,
            FrequencyBase = vibratoBase,
            AmplitudeStep = tremoloStep,
            AmplitudeBase = tremoloBase,
            PartialDelays = delays,
            PartialVolumes = volumes,
            PartialSemitones = semitones,
            PartialStarts = starts,
            FilterEnvelopeEval = filterEnvelopeEval
        };
    }

    private static (EnvelopeGenerator? rateEval, EnvelopeGenerator? rangeEval, int step, int baseValue)
        InitFrequencyModulation(Voice voice, double samplesPerStep)
    {
        if (voice.PitchLfo != null)
        {
            var rateEval = new EnvelopeGenerator(voice.PitchLfo.Rate);
            var rangeEval = new EnvelopeGenerator(voice.PitchLfo.Depth);
            rateEval.Reset();
            rangeEval.Reset();

            var step = (int)((double)(voice.PitchLfo.Rate.End - voice.PitchLfo.Rate.Start) * AudioConstants.PhaseScale / samplesPerStep);
            var baseValue = (int)(voice.PitchLfo.Rate.Start * AudioConstants.PhaseScale / samplesPerStep);

            return (rateEval, rangeEval, step, baseValue);
        }

        return (null, null, 0, 0);
    }

    private static (EnvelopeGenerator? rateEval, EnvelopeGenerator? rangeEval, int step, int baseValue)
        InitAmplitudeModulation(Voice voice, double samplesPerStep)
    {
        if (voice.AmplitudeLfo != null)
        {
            var rateEval = new EnvelopeGenerator(voice.AmplitudeLfo.Rate);
            var rangeEval = new EnvelopeGenerator(voice.AmplitudeLfo.Depth);
            rateEval.Reset();
            rangeEval.Reset();

            var step = (int)((double)(voice.AmplitudeLfo.Rate.End - voice.AmplitudeLfo.Rate.Start) * AudioConstants.PhaseScale / samplesPerStep);
            var baseValue = (int)(voice.AmplitudeLfo.Rate.Start * AudioConstants.PhaseScale / samplesPerStep);

            return (rateEval, rangeEval, step, baseValue);
        }

        return (null, null, 0, 0);
    }

    private static (int[] delays, int[] volumes, int[] semitones, int[] starts)
        InitOscillators(Voice voice, double samplesPerStep)
    {
        var delays = new int[AudioConstants.MaxOscillators];
        var volumes = new int[AudioConstants.MaxOscillators];
        var semitones = new int[AudioConstants.MaxOscillators];
        var starts = new int[AudioConstants.MaxOscillators];

        var oscillatorCount = Math.Min(AudioConstants.MaxOscillators, voice.Oscillators.Count);
        for (var oscillator = 0; oscillator < oscillatorCount; oscillator++)
        {
            var height = voice.Oscillators[oscillator];
            if (height.Amplitude.Value != 0)
            {
                delays[oscillator] = (int)(height.Delay.Value * samplesPerStep);
                volumes[oscillator] = (height.Amplitude.Value << 14) / 100;
                semitones[oscillator] = (int)(
                    (voice.FrequencyEnvelope.End - voice.FrequencyEnvelope.Start) * AudioConstants.PhaseScale *
                    WaveformTables.GetPitchMultiplier(height.PitchOffset) / samplesPerStep);
                starts[oscillator] = (int)(voice.FrequencyEnvelope.Start * AudioConstants.PhaseScale / samplesPerStep);

                if (oscillator == 0)
                {
                    _ = WaveformTables.GetPitchMultiplier(height.PitchOffset);
                }
                else if (oscillator == 1)
                {
                    _ = WaveformTables.GetPitchMultiplier(height.PitchOffset);
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
        var phases = new int[AudioConstants.MaxOscillators];
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
        var oscillatorCount = Math.Min(AudioConstants.MaxOscillators, voice.Oscillators.Count);
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
            var nextPhase = phase + state.FrequencyBase + (rate * state.FrequencyStep >> 16);

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

            var newAmp = amplitude * (mod + AudioConstants.FixedPoint.Offset) >> 15;
            var nextPhase = phase + state.AmplitudeBase + (rate * state.AmplitudeStep >> 16);
            return (newAmp, nextPhase);
        }

        return (amplitude, phase);
    }

    private static int GenerateSample(int amplitude, int phase, Waveform waveform)
    {
        return waveform switch
        {
            Waveform.Square => (phase & AudioConstants.PhaseMask) < AudioConstants.FixedPoint.Quarter ? amplitude : -amplitude,
            Waveform.Sine => (WaveformTables.SinTable[phase & AudioConstants.PhaseMask] * amplitude) >> 14,
            Waveform.Saw => (((phase & AudioConstants.PhaseMask) * amplitude) >> 14) - amplitude,
            Waveform.Noise => WaveformTables.NoiseTable[(phase / AudioConstants.NoisePhaseDiv) & AudioConstants.PhaseMask] * amplitude,
            Waveform.Off => 0,
            _ => 0
        };
    }

    private static void ApplyGating(int[] buffer, Voice voice, int sampleCount)
    {
        if (voice.GateSilence != null && voice.GateDuration != null)
        {
            var silenceEval = new EnvelopeGenerator(voice.GateSilence);
            var durationEval = new EnvelopeGenerator(voice.GateDuration);
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
        public required EnvelopeGenerator FreqBaseEval { get; init; }
        public required EnvelopeGenerator AmpBaseEval { get; init; }
        public EnvelopeGenerator? FreqModRateEval { get; init; }
        public EnvelopeGenerator? FreqModRangeEval { get; init; }
        public EnvelopeGenerator? AmpModRateEval { get; init; }
        public EnvelopeGenerator? AmpModRangeEval { get; init; }
        public int FrequencyStep { get; init; }
        public int FrequencyBase { get; init; }
        public int AmplitudeStep { get; init; }
        public int AmplitudeBase { get; init; }
        public int[] PartialDelays { get; init; } = null!;
        public int[] PartialVolumes { get; init; } = null!;
        public int[] PartialSemitones { get; init; } = null!;
        public int[] PartialStarts { get; init; } = null!;
        public EnvelopeGenerator? FilterEnvelopeEval { get; init; }
    }
}