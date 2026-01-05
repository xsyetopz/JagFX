package jagfx.synth

import jagfx.model._
import jagfx.constants.Int16

/** Stateful envelope evaluator that interpolates between segments over time.
  * Call `reset()` before each synthesis pass, then `evaluate()` for each
  * sample.
  *
  * @param envelope
  *   `Envelope` definition to evaluate
  */
class EnvelopeEvaluator(envelope: Envelope):
  private var _threshold: Int = 0
  private var _position: Int = 0
  private var _delta: Int = 0
  private var _amplitude: Int = 0
  private var _ticks: Int = 0

  /** Resets evaluator state for new synthesis pass. */
  def reset(): Unit =
    _threshold = 0
    _position = 0
    _delta = 0
    _amplitude = envelope.start << 15
    _ticks = 0

  /** Evaluates envelope at current tick, advancing internal state.
    *
    * Returns interpolated value scaled to `0-65535` range.
    */
  def evaluate(period: Int): Int =
    if envelope.segments.isEmpty then return envelope.start

    if _ticks >= _threshold then
      _amplitude = envelope.segments(_position).peak << 15
      _position += 1

      if _position >= envelope.segments.length then
        _position = envelope.segments.length - 1

      _threshold = ((envelope
        .segments(_position)
        .duration
        .toDouble / Int16.Range) * period).toInt

      if _threshold > _ticks then
        _delta = ((envelope
          .segments(_position)
          .peak << 15) - _amplitude) / (_threshold - _ticks)
      else _delta = 0

    _amplitude += _delta
    _ticks += 1
    (_amplitude - _delta) >> 15
