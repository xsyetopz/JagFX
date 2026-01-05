package jagfx

import java.nio.file.*

import jagfx.io.*
import jagfx.synth.*

/** Command-line interface for `.synth` to WAV conversion. */
object JagFXCli:
  /** Application entry point. */
  def main(args: Array[String]): Unit =
    val cleanArgs =
      if args.nonEmpty && args(0) == "--" then args.drop(1)
      else args

    if cleanArgs.contains("--help") || cleanArgs.contains("-h") then
      println("Usage: jagfx-cli <input.synth> <output.wav> [loopCount]")
      System.exit(0)

    if cleanArgs.length < 2 then
      scribe.error("Usage: jagfx-cli <input.synth> <output.wav> [loopCount]")
      System.exit(1)

    val inputPath = Paths.get(cleanArgs(0))
    val outputPath = Paths.get(cleanArgs(1))
    val loopCount = if cleanArgs.length > 2 then cleanArgs(2).toInt else 1

    if !Files.exists(inputPath) then
      scribe.error(s"Input file not found: $inputPath")
      System.exit(1)

    SynthReader.readFromPath(inputPath) match
      case Left(error) =>
        scribe.error(s"Parse error: ${error.message}")
        System.exit(1)
      case Right(synthFile) =>
        val audio = TrackSynthesizer.synthesize(synthFile, loopCount)
        val wavBytes = WavWriter.write(audio.toUBytes)
        Files.write(outputPath, wavBytes)
        System.exit(0)
