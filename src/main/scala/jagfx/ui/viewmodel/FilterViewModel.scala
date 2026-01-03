package jagfx.ui.viewmodel

import javafx.beans.property._
import jagfx.model._

/** `ViewModel` for `Filter` data (poles/zeros). */
class FilterViewModel extends IViewModel:
  // Pair counts (0-4 each)
  val pairCount0 = new SimpleIntegerProperty(0)
  val pairCount1 = new SimpleIntegerProperty(0)

  // Unity gain (start/end)
  val unity0 = new SimpleIntegerProperty(0)
  val unity1 = new SimpleIntegerProperty(0)

  // Phase and magnitude for each pole (2 directions * 4 poles * 2 interpolation points)
  private val phaseArrays = Array.fill(2, 4, 2)(new SimpleIntegerProperty(0))
  private val magnitudeArrays =
    Array.fill(2, 4, 2)(new SimpleIntegerProperty(0))

  def pairPhase(dir: Int)(slot: Int)(point: Int): IntegerProperty =
    phaseArrays(dir)(slot)(point)
  def pairMagnitude(dir: Int)(slot: Int)(point: Int): IntegerProperty =
    magnitudeArrays(dir)(slot)(point)

  def isEmpty: Boolean = pairCount0.get == 0 && pairCount1.get == 0

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
                phaseArrays(dir)(slot)(point).set(f.pairPhase(dir)(point)(slot))
                magnitudeArrays(dir)(slot)(point)
                  .set(f.pairMagnitude(dir)(point)(slot))

        notifyListeners()
      case None =>
        clear()

  def clear(): Unit =
    pairCount0.set(0)
    pairCount1.set(0)
    unity0.set(0)
    unity1.set(0)
    for dir <- 0 until 2; slot <- 0 until 4; point <- 0 until 2 do
      phaseArrays(dir)(slot)(point).set(0)
      magnitudeArrays(dir)(slot)(point).set(0)
    notifyListeners()

  def toModel(): Option[Filter] =
    if isEmpty then None
    else
      val pairPhase = Array.ofDim[Int](2, 2, 4)
      val pairMagnitude = Array.ofDim[Int](2, 2, 4)

      for dir <- 0 until 2; slot <- 0 until 4; point <- 0 until 2 do
        pairPhase(dir)(point)(slot) = phaseArrays(dir)(slot)(point).get
        pairMagnitude(dir)(point)(slot) = magnitudeArrays(dir)(slot)(point).get

      Some(
        Filter(
          Array(pairCount0.get, pairCount1.get),
          Array(unity0.get, unity1.get),
          pairPhase,
          pairMagnitude,
          None // envelope handled separately
        )
      )

  override protected def registerPropertyListeners(cb: () => Unit): Unit =
    pairCount0.addListener((_, _, _) => cb())
    pairCount1.addListener((_, _, _) => cb())
    unity0.addListener((_, _, _) => cb())
    unity1.addListener((_, _, _) => cb())
