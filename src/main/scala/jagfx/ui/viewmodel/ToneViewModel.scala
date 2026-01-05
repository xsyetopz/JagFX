package jagfx.ui.viewmodel

import javafx.beans.property._
import jagfx.model._
import jagfx.constants

/** `ViewModel` for single `Tone`. */
class ToneViewModel extends ViewModelLike:
  val enabled = new SimpleBooleanProperty(false)

  val pitch = new EnvelopeViewModel()
  val volume = new EnvelopeViewModel()

  val vibratoRate = new EnvelopeViewModel()
  val vibratoDepth = new EnvelopeViewModel()
  val tremoloRate = new EnvelopeViewModel()
  val tremoloDepth = new EnvelopeViewModel()

  val gateRelease = new EnvelopeViewModel()
  val gateAttack = new EnvelopeViewModel()

  val filterEnvelope = new EnvelopeViewModel()

  val filterViewMode = new FilterViewModel()

  val duration = new SimpleIntegerProperty(1000)
  val startOffset = new SimpleIntegerProperty(0)
  val echoDelay = new SimpleIntegerProperty(0)
  val echoMix = new SimpleIntegerProperty(0)

  val partials = Array.fill(constants.MaxPartials)(new PartialViewModel())

  override protected def registerPropertyListeners(cb: () => Unit): Unit =
    Seq(
      pitch,
      volume,
      vibratoRate,
      vibratoDepth,
      tremoloRate,
      tremoloDepth,
      gateRelease,
      gateAttack,
      filterEnvelope
    ).foreach(_.addChangeListener(cb))
    filterViewMode.addChangeListener(cb)
    partials.foreach(_.addChangeListener(cb))
    enabled.addListener((_, _, _) => cb())
    duration.addListener((_, _, _) => cb())
    startOffset.addListener((_, _, _) => cb())
    echoDelay.addListener((_, _, _) => cb())
    echoMix.addListener((_, _, _) => cb())

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
        gateRelease.clear()
        gateAttack.clear()
        filterEnvelope.clear()

        t.vibratoRate.foreach(vibratoRate.load)
        t.vibratoDepth.foreach(vibratoDepth.load)
        t.tremoloRate.foreach(tremoloRate.load)
        t.tremoloDepth.foreach(tremoloDepth.load)
        t.gateRelease.foreach(gateRelease.load)
        t.gateAttack.foreach(gateAttack.load)

        filterViewMode.load(t.filter)
        t.filter.flatMap(_.envelope).foreach(filterEnvelope.load)

        duration.set(t.duration)
        startOffset.set(t.start)
        echoDelay.set(t.echoDelay)
        echoMix.set(t.echoMix)

        for i <- 0 until constants.MaxTones do
          if i < t.partials.length then partials(i).load(t.partials(i))
          else partials(i).clear()

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
    gateRelease.clear()
    gateAttack.clear()
    filterEnvelope.clear()
    filterViewMode.clear()

    duration.set(1000)
    startOffset.set(0)
    echoDelay.set(0)
    echoMix.set(0)
    partials.foreach(_.clear())

  def toModel(): Option[Tone] =
    if !enabled.get then None
    else
      val activePartials =
        partials
          .take(constants.MaxPartials / 2)
          .filter(_.active.get)
          .map(_.toModel())
          .toVector

      val emptyIArray3D =
        IArray.tabulate(2)(_ => IArray.tabulate(2)(_ => IArray.fill(4)(0)))

      val filterModel = filterViewMode.toModel() match
        case Some(f) =>
          val env = _optModel(filterEnvelope)
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
        Tone(
          pitch.toModel(),
          volume.toModel(),
          _optModel(vibratoRate),
          _optModel(vibratoDepth),
          _optModel(tremoloRate),
          _optModel(tremoloDepth),
          _optModel(gateRelease),
          _optModel(gateAttack),
          activePartials,
          echoDelay.get,
          echoMix.get,
          duration.get,
          startOffset.get,
          filterModel
        )
      )

  private def _optModel(vm: EnvelopeViewModel): Option[Envelope] =
    if vm.isEmpty then None else Some(vm.toModel())
