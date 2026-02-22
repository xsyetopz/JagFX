package jagfx.ui.viewmodel

import jagfx.Constants
import jagfx.model.*
import jagfx.utils.ArrayUtils
import javafx.beans.property.*

/** View model for single `Voice`. */
class VoiceViewModel extends ViewModelLike:
  // Fields

  /** Whether this voice is enabled for synthesis. */
  val enabled = new SimpleBooleanProperty(false)

  /** Pitch envelope view model. */
  val pitch = new EnvelopeViewModel()

  /** Volume envelope view model. */
  val volume = new EnvelopeViewModel()

  /** Vibrato rate modulation envelope. */
  val vibratoRate = new EnvelopeViewModel()

  /** Vibrato depth modulation envelope. */
  val vibratoDepth = new EnvelopeViewModel()

  /** Tremolo rate modulation envelope. */
  val tremoloRate = new EnvelopeViewModel()

  /** Tremolo depth modulation envelope. */
  val tremoloDepth = new EnvelopeViewModel()

  /** Gate silence duration envelope. */
  val gateSilence = new EnvelopeViewModel()

  /** Gate audible duration envelope. */
  val gateDuration = new EnvelopeViewModel()

  /** Filter interpolation envelope. */
  val filterEnvelope = new EnvelopeViewModel()

  /** Filter poles/zeros view model. */
  val filterViewMode = new FilterViewModel()

  /** Voice duration in samples. */
  val duration = new SimpleIntegerProperty(1000)

  /** Start offset in samples. */
  val startOffset = new SimpleIntegerProperty(0)

  /** Echo delay in samples. */
  val echoDelay = new SimpleIntegerProperty(0)

  /** Echo mix level (`0-100`). */
  val echoMix = new SimpleIntegerProperty(0)

  /** Array of partial view models. */
  val partials: Array[PartialViewModel] =
    Array.fill(Constants.MaxPartials)(new PartialViewModel())

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
    partials.foreach(_.addChangeListener(cb))
    enabled.addListener((_, _, _) => cb())
    duration.addListener((_, _, _) => cb())
    startOffset.addListener((_, _, _) => cb())
    echoDelay.addListener((_, _, _) => cb())
    echoMix.addListener((_, _, _) => cb())

  /** Loads voice data from model `Voice`. */
  def load(voiceOpt: Option[Voice]): Unit =
    voiceOpt match
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
        echoDelay.set(t.echoDelay)
        echoMix.set(t.echoMix)

        for i <- 0 until Constants.MaxVoices do
          if i < t.partials.length then partials(i).load(t.partials(i))
          else partials(i).clear()

      case None =>
        clear()

  /** Resets all values to defaults. */
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
    echoDelay.set(0)
    echoMix.set(0)
    partials.foreach(_.clear())

  /** Converts view model state to model `Voice`. */
  def toModel(): Option[Voice] =
    if !enabled.get then None
    else
      val activePartials =
        partials
          .take(Constants.MaxPartials / 2)
          .filter(_.active.get)
          .map(_.toModel())
          .toVector

      val emptyIArray3D = ArrayUtils.emptyFilterIArray3D

      val filterModel = filterViewMode.toModel() match
        case Some(f) =>
          val env = optModel(filterEnvelope)
          Some(f.copy(envelope = env))
        case None if !filterEnvelope.isEmpty =>
          val empty = Filter(
            IArray(0, 0),
            IArray(0, 0),
            emptyIArray3D,
            emptyIArray3D,
            None
          )
          Some(empty.copy(envelope = Some(filterEnvelope.toModel())))
        case None => None

      Some(
        Voice(
          pitch.toModel(),
          volume.toModel(),
          optModel(vibratoRate),
          optModel(vibratoDepth),
          optModel(tremoloRate),
          optModel(tremoloDepth),
          optModel(gateSilence),
          optModel(gateDuration),
          activePartials,
          echoDelay.get,
          echoMix.get,
          duration.get,
          startOffset.get,
          filterModel
        )
      )

  private def optModel(vm: EnvelopeViewModel): Option[Envelope] =
    if vm.isEmpty then None else Some(vm.toModel())
