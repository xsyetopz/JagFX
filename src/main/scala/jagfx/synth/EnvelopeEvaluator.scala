package jagfx.synth

import jagfx.model._

/** Stateful envelope evaluator that interpolates between segments over time.
  * Call `reset()` before each synthesis pass, then `evaluate()` for each
  * sample.
  *
  * @param envelope
  *   `Envelope` definition to evaluate
  */
class EnvelopeEvaluator(envelope: Envelope):
  private var threshold: Int = 0
  private var position: Int = 0
  private var delta: Int = 0
  private var amplitude: Int = 0
  private var ticks: Int = 0

  /** Resets evaluator state for new synthesis pass. */
  def reset(): Unit =
    threshold = 0
    position = 0
    delta = 0
    amplitude = 0
    ticks = 0

  /** Evaluates envelope at current tick, advancing internal state. Returns
    * interpolated value scaled to `0`-`65535` range.
    */
  def evaluate(deltaArg: Int): Int =
    if ticks >= threshold then
      amplitude = envelope.segments(position).peak << 15
      position += 1
      if position >= envelope.segments.length then
        position = envelope.segments.length - 1
      threshold = ((envelope
        .segments(position)
        .duration
        .toDouble / 65536.0) * deltaArg).toInt
      if threshold > ticks then
        delta = ((envelope
          .segments(position)
          .peak << 15) - amplitude) / (threshold - ticks)
    amplitude += delta
    ticks += 1
    (amplitude - delta) >> 15
