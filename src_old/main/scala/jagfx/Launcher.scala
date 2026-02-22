package jagfx

object Launcher:
  def main(args: Array[String]): Unit =
    scribe.Logger.root
      .clearHandlers()
      .withHandler(minimumLevel = Some(scribe.Level.Debug))
      .replace()
    JagFX.main(args)
