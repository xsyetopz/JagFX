package jagfx.ui.viewmodel

import javafx.beans.property._
import jagfx.model._
import jagfx.Constants

/** `ViewModel` for single `Tone`. */
class ToneViewModel:
  val enabled = new SimpleBooleanProperty(false)

  // Envelopes
  val pitch = new EnvelopeViewModel()
  val volume = new EnvelopeViewModel()

  // Modulation
  val vibratoRate = new EnvelopeViewModel()
  val vibratoDepth = new EnvelopeViewModel()
  val tremoloRate = new EnvelopeViewModel()
  val tremoloDepth = new EnvelopeViewModel()

  // Gate
  val gateSilence = new EnvelopeViewModel()
  val gateDuration = new EnvelopeViewModel()

  // Properties
  val duration = new SimpleIntegerProperty(1000)
  val startOffset = new SimpleIntegerProperty(0)
  val reverbDelay = new SimpleIntegerProperty(0)
  val reverbVolume = new SimpleIntegerProperty(0)

  // Harmonics (10 slots)
  val harmonics = Array.fill(Constants.MaxHarmonics)(new HarmonicViewModel())

  def load(toneOpt: Option[Tone]): Unit =
    toneOpt match
      case Some(t) =>
        enabled.set(true)
        pitch.load(t.pitchEnvelope)
        volume.load(t.volumeEnvelope)

        vibratoRate.clear()
        vibratoDepth.clear()
        tremoloRate.clear()
        tremoloDepth.clear()
        gateSilence.clear()
        gateDuration.clear()

        t.vibratoRate.foreach(vibratoRate.load)
        t.vibratoDepth.foreach(vibratoDepth.load)
        t.tremoloRate.foreach(tremoloRate.load)
        t.tremoloDepth.foreach(tremoloDepth.load)
        t.gateSilence.foreach(gateSilence.load)
        t.gateDuration.foreach(gateDuration.load)

        duration.set(t.duration)
        startOffset.set(t.start)
        reverbDelay.set(t.reverbDelay)
        reverbVolume.set(t.reverbVolume)

        for i <- 0 until 10 do
          if i < t.harmonics.length then harmonics(i).load(t.harmonics(i))
          else harmonics(i).clear()

      case None =>
        clear()

  def clear(): Unit =
    enabled.set(false)
    pitch.clear()
    volume.clear()
    vibratoRate.clear()
    vibratoDepth.clear()
    tremoloRate.clear()
    tremoloDepth.clear()
    gateSilence.clear()
    gateDuration.clear()
    duration.set(1000)
    startOffset.set(0)
    reverbDelay.set(0)
    reverbVolume.set(0)
    harmonics.foreach(_.clear())

  def toModel(): Option[Tone] =
    if !enabled.get then None
    else
      val activeHarmonics =
        harmonics.take(5).filter(_.active.get).map(_.toModel()).toVector
      Some(
        Tone(
          pitch.toModel(),
          volume.toModel(),
          if vibratoRate.isEmpty then None else Some(vibratoRate.toModel()),
          if vibratoDepth.isEmpty then None else Some(vibratoDepth.toModel()),
          if tremoloRate.isEmpty then None else Some(tremoloRate.toModel()),
          if tremoloDepth.isEmpty then None else Some(tremoloDepth.toModel()),
          if gateSilence.isEmpty then None else Some(gateSilence.toModel()),
          if gateDuration.isEmpty then None else Some(gateDuration.toModel()),
          activeHarmonics,
          reverbDelay.get,
          reverbVolume.get,
          duration.get,
          startOffset.get
        )
      )

class HarmonicViewModel:
  val active = new SimpleBooleanProperty(false)
  val semitone = new SimpleIntegerProperty(0)
  val volume = new SimpleIntegerProperty(0)
  val delay = new SimpleIntegerProperty(0)

  def load(h: Harmonic): Unit =
    active.set(true)
    semitone.set(h.semitone)
    volume.set(h.volume)
    delay.set(h.delay)

  def clear(): Unit =
    active.set(false)
    semitone.set(0)
    volume.set(0)
    delay.set(0)

  def toModel(): Harmonic =
    Harmonic(volume.get, semitone.get, delay.get)
