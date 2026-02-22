package jagfx.ui.viewmodel

import jagfx.model.*
import jagfx.utils.ArrayUtils
import javafx.beans.property.*

/** View model for `Filter` data (poles/zeros). */
class FilterViewModel extends ViewModelLike:
  // Fields

  /** Feedforward pole pair count (`0-4`). */
  val pairCount0 = new SimpleIntegerProperty(0)

  /** Feedback pole pair count (`0-4`). */
  val pairCount1 = new SimpleIntegerProperty(0)

  /** Start unity gain value. */
  val unity0 = new SimpleIntegerProperty(0)

  /** End unity gain value. */
  val unity1 = new SimpleIntegerProperty(0)

  // Phase and magnitude for each pole (`2` directions * `4` poles * `2` interpolation points)
  private val phaseArrays = Array.fill(2, 4, 2)(new SimpleIntegerProperty(0))
  private val magnitudeArrays =
    Array.fill(2, 4, 2)(new SimpleIntegerProperty(0))

  /** Returns phase property for pole at `[direction][slot][point]`. */
  def pairPhase(dir: Int)(slot: Int)(point: Int): IntegerProperty =
    phaseArrays(dir)(slot)(point)

  /** Returns magnitude property for pole at `[direction][slot][point]`. */
  def pairMagnitude(dir: Int)(slot: Int)(point: Int): IntegerProperty =
    magnitudeArrays(dir)(slot)(point)

  /** Returns `true` if filter has no poles configured. */
  def isEmpty: Boolean = pairCount0.get == 0 && pairCount1.get == 0

  /** Loads filter data from model into this view model. */
  def load(filterOpt: Option[Filter]): Unit =
    filterOpt match
      case Some(f) =>
        pairCount0.set(f.pairCounts(0))
        pairCount1.set(f.pairCounts(1))
        unity0.set(f.unity(0))
        unity1.set(f.unity(1))

        for dir <- 0 until 2 do
          val maxPoles =
            if f.pairPhase.length > dir then f.pairPhase(dir)(0).length else 0
          for slot <- 0 until math.min(4, maxPoles) do
            for point <- 0 until 2 do
              if f.pairPhase.length > dir &&
                f.pairPhase(dir).length > point &&
                f.pairPhase(dir)(point).length > slot
              then
                phaseArrays(dir)(slot)(point)
                  .set(f.pairPhase(dir)(point)(slot))
                magnitudeArrays(dir)(slot)(point)
                  .set(f.pairMagnitude(dir)(point)(slot))

        notifyListeners()
      case None =>
        clear()

  /** Resets all values to defaults. */
  def clear(): Unit =
    pairCount0.set(0)
    pairCount1.set(0)
    unity0.set(0)
    unity1.set(0)
    for dir <- 0 until 2; slot <- 0 until 4; point <- 0 until 2 do
      phaseArrays(dir)(slot)(point).set(0)
      magnitudeArrays(dir)(slot)(point).set(0)
    notifyListeners()

  /** Converts view model state to model `Filter`. */
  def toModel(): Option[Filter] =
    if isEmpty then None
    else
      val pairPhase = Array.ofDim[Int](2, 2, 4)
      val pairMagnitude = Array.ofDim[Int](2, 2, 4)

      for dir <- 0 until 2; slot <- 0 until 4; point <- 0 until 2 do
        pairPhase(dir)(point)(slot) = phaseArrays(dir)(slot)(point).get
        pairMagnitude(dir)(point)(slot) = magnitudeArrays(dir)(slot)(point).get

      val phaseIArray = ArrayUtils.toFilterIArray3D(pairPhase)
      val magIArray = ArrayUtils.toFilterIArray3D(pairMagnitude)

      Some(
        Filter(
          IArray(pairCount0.get, pairCount1.get),
          IArray(unity0.get, unity1.get),
          phaseIArray,
          magIArray,
          None
        )
      )

  override protected def registerPropertyListeners(cb: () => Unit): Unit =
    pairCount0.addListener((_, _, _) => cb())
    pairCount1.addListener((_, _, _) => cb())
    unity0.addListener((_, _, _) => cb())
    unity1.addListener((_, _, _) => cb())
