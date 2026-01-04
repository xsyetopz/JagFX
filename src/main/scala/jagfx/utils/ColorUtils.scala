package jagfx.utils

/** Colors synced with `_variables.scss`. */
object ColorUtils:
  // Backgrounds
  val BgBlack: Int = 0xff000000

  // Borders
  val BorderDim: Int = 0xff333333

  // Text
  val White: Int = 0xfff0f0f0

  // Accent
  val Graph: Int = 0xffee7733 // $bg-graph (envelope curves)

  // Filter
  val FilterPole: Int = 0xff009e73 // $color-info teal for poles
  val FilterZero: Int = 0xffee7733 // $bg-graph orange for zeros
  val FilterResponse: Int = 0xff33bb99 // frequency response curve

  // Semantic
  val Output: Int = 0xff009e73 // $color-output
  val Gating: Int = 0xffcc79a7 // $color-gating

  // Grid
  val GridLineFaint: Int = 0xff2a2a2a
  val GridLineMinor: Int = 0xff1a1a1a

  // Editor control points
  val PointNormal: Int = 0xffee7733
  val PointHover: Int = 0xffffaa55
  val PointSelected: Int = 0xffffffff
