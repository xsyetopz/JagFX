package jagfx

object TestFixtures:
  lazy val cowDeathHex: Array[Byte] = _loadResource("/cow_death.synth")
  lazy val protectFromMagicHex: Array[Byte] = _loadResource(
    "/protect_from_magic.synth"
  )
  lazy val iceCastHex: Array[Byte] = _loadResource("/ice_cast.synth")

  private def _loadResource(path: String): Array[Byte] =
    val stream = getClass.getResourceAsStream(path)
    if stream == null then
      throw new RuntimeException(s"Resource not found: $path")
    try stream.readAllBytes()
    finally stream.close()
