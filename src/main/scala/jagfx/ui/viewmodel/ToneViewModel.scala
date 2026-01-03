package jagfx.ui.viewmodel

import javafx.beans.property._
import jagfx.model._
import jagfx.Constants

/** `ViewModel` for single `Tone`. */
class ToneViewModel extends IViewModel:
  val enabled = new SimpleBooleanProperty(false)

  val pitch = new EnvelopeViewModel()
  val volume = new EnvelopeViewModel()

  val vibratoRate = new EnvelopeViewModel()
  val vibratoDepth = new EnvelopeViewModel()
  val tremoloRate = new EnvelopeViewModel()
  val tremoloDepth = new EnvelopeViewModel()

  val gateSilence = new EnvelopeViewModel()
  val gateDuration = new EnvelopeViewModel()

  val filterEnvelope = new EnvelopeViewModel()

  val filterViewMode = new FilterViewModel()

  val duration = new SimpleIntegerProperty(1000)
  val startOffset = new SimpleIntegerProperty(0)
  val reverbDelay = new SimpleIntegerProperty(0)
  val reverbVolume = new SimpleIntegerProperty(0)

  val harmonics = Array.fill(Constants.MaxHarmonics)(new HarmonicViewModel())

  override protected def registerPropertyListeners(cb: () => Unit): Unit =
    Seq(
      pitch,
      volume,
      vibratoRate,
      vibratoDepth,
      tremoloRate,
      tremoloDepth,
      gateSilence,
      gateDuration,
      filterEnvelope
    ).foreach(_.addChangeListener(cb))
    filterViewMode.addChangeListener(cb)
    harmonics.foreach(_.addChangeListener(cb))
    enabled.addListener((_, _, _) => cb())
    duration.addListener((_, _, _) => cb())
    startOffset.addListener((_, _, _) => cb())
    reverbDelay.addListener((_, _, _) => cb())
    reverbVolume.addListener((_, _, _) => cb())

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
        filterEnvelope.clear()

        t.vibratoRate.foreach(vibratoRate.load)
        t.vibratoDepth.foreach(vibratoDepth.load)
        t.tremoloRate.foreach(tremoloRate.load)
        t.tremoloDepth.foreach(tremoloDepth.load)
        t.gateSilence.foreach(gateSilence.load)
        t.gateDuration.foreach(gateDuration.load)

        filterViewMode.load(t.filter)
        t.filter.flatMap(_.envelope).foreach(filterEnvelope.load)

        duration.set(t.duration)
        startOffset.set(t.start)
        reverbDelay.set(t.reverbDelay)
        reverbVolume.set(t.reverbVolume)

        for i <- 0 until Constants.MaxTones do
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
    filterEnvelope.clear()
    filterViewMode.clear()

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

      val filterModel = filterViewMode.toModel() match
        case Some(f) =>
          val env =
            if filterEnvelope.isEmpty then None
            else Some(filterEnvelope.toModel())
          Some(f.copy(envelope = env))
        case None if !filterEnvelope.isEmpty =>
          val empty = Filter(
            Array(0, 0),
            Array(0, 0),
            Array.ofDim(2, 2, 4),
            Array.ofDim(2, 2, 4),
            None
          )
          Some(empty.copy(envelope = Some(filterEnvelope.toModel())))
        case None => None

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
          startOffset.get,
          filterModel
        )
      )
