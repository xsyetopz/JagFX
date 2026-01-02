package jagfx

import jagfx.io._
import jagfx.synth._
import java.nio.file.{Files, Paths}

/** Command-line interface for `.synth` to `.wav` conversion. */
object JagFXCli:
  /** Entry point. */
  def main(args: Array[String]): Unit =
    val cleanArgs =
      if args.nonEmpty && args(0) == "--" then args.drop(1)
      else args
    if cleanArgs.length < 2 then
      scribe.error("Usage: jagfx-cli <input.synth> <output.wav> [loopCount]")
      System.exit(1)

    val inputPath = Paths.get(cleanArgs(0))
    val outputPath = Paths.get(cleanArgs(1))
    val loopCount =
      if cleanArgs.length > 2 then cleanArgs(2).toInt else 1

    if !Files.exists(inputPath) then
      scribe.error(s"Input file not found: $inputPath")
      System.exit(1)

    scribe.info(s"Reading $inputPath...")
    SynthReader.readFromPath(inputPath) match
      case Left(error) =>
        scribe.error(s"Parse error: ${error.message}")
        System.exit(1)
      case Right(synthFile) =>
        val activeToneCount = synthFile.activeTones.size
        scribe.info(s"Loaded synth with $activeToneCount active tone(s)")
        scribe.debug(s"Loop: ${synthFile.loop.begin} -> ${synthFile.loop.end}")

        scribe.info(s"Synthesizing with loopCount=$loopCount...")
        val audio = TrackSynthesizer.synthesize(synthFile, loopCount)
        scribe.debug(s"Generated ${audio.length} sample(s)")

        val wavBytes = WavWriter.write(audio.toBytes)
        Files.write(outputPath, wavBytes)
        scribe.info(s"Wrote $outputPath (${wavBytes.length} byte(s))")
