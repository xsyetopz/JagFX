package jagfx.utils

/** Colors synced with `variables.scss`. */
object ColorUtils:
  // Backgrounds
  final val BgBlack: Int = 0xff000000

  // Borders
  final val BorderDim: Int = 0xff333333

  // Text
  final val White: Int = 0xfff0f0f0

  // Accent
  final val Graph: Int = 0xffee7733

  // Filter
  final val FilterPole: Int = 0xff009e73
  final val FilterZero: Int = 0xffee7733
  final val FilterResponse: Int = 0xff33bb99

  // Semantic
  final val Output: Int = 0xff009e73
  final val Gating: Int = 0xffcc79a7

  // Grid
  final val GridLineFaint: Int = 0xff2a2a2a
  final val GridLineMinor: Int = 0xff1a1a1a

  // Editor control points
  final val PointNormal: Int = 0xffee7733
  final val PointHover: Int = 0xffffaa55
  final val PointSelected: Int = 0xffffffff

  /** Returns color with alpha channel dimmed to `~30%`. */
  inline def dimColor(color: Int): Int = (color & 0x00ffffff) | 0x50000000
