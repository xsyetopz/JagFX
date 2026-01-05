package jagfx.utils

/** Array construction utilities for filter data structures. */
object ArrayUtils:
  /** Creates 3D `IArray` from mutable 3D array (2x2x4 shape for filter data).
    */
  inline def toFilterIArray3D(
      arr: Array[Array[Array[Int]]]
  ): IArray[IArray[IArray[Int]]] =
    IArray.tabulate(2)(d =>
      IArray.tabulate(2)(p => IArray.tabulate(4)(i => arr(d)(p)(i)))
    )

  /** Creates empty 3D `IArray` for filter data (2x2x4 zeros). */
  inline def emptyFilterIArray3D: IArray[IArray[IArray[Int]]] =
    IArray.tabulate(2)(_ => IArray.tabulate(2)(_ => IArray.fill(4)(0)))
