package jagfx.ui.controller.header

import javafx.stage._
import javafx.scene.Node
import java.io.File
import java.nio.file.Files
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.io._
import jagfx.synth.TrackSynthesizer
import jagfx.utils.UserPreferences
import javafx.scene.control.Alert

/** File I/O operations for `.synth` files and WAV export. */
class FileOperations(
    viewModel: SynthViewModel,
    getWindow: () => Window
):
  private var currentFile: Option[File] = None

  def open(): Unit =
    val chooser = new FileChooser()
    chooser.getExtensionFilters.add(
      new FileChooser.ExtensionFilter("Synth Files", "*.synth")
    )
    val file = chooser.showOpenDialog(getWindow())
    if file != null then
      SynthReader.readFromPath(file.toPath) match
        case Right(synth) =>
          if synth.warnings.nonEmpty then showWarningDialog(synth.warnings)

          viewModel.load(synth)
          viewModel.setCurrentFilePath(file.getAbsolutePath)
          currentFile = Some(file)
        case Left(err) =>
          showErrorDialog(s"Failed to load: ${err.message}")

  private def showWarningDialog(warnings: List[String]): Unit =
    val alert = new Alert(Alert.AlertType.WARNING)
    alert.setTitle("Corrupt Data Detected")
    alert.setHeaderText("Loaded file appears to be corrupted or truncated.")
    alert.setContentText(
      warnings.mkString(
        "\n"
      ) + "\n\nPartial data loaded, but playback may differ from original source."
    )
    alert.showAndWait()

  private def showErrorDialog(msg: String): Unit =
    val alert = new Alert(Alert.AlertType.ERROR)
    alert.setTitle("Load Error")
    alert.setHeaderText("Could not load file")
    alert.setContentText(msg)
    alert.showAndWait()

  def save(): Unit =
    currentFile match
      case Some(file) =>
        try
          val bytes = SynthWriter.write(viewModel.toModel())
          Files.write(file.toPath, bytes)
        catch
          case e: Exception => scribe.error(s"Failed to save: ${e.getMessage}")
      case None => saveAs(Some("*.synth"))

  def saveAs(filterObj: Option[String] = None): Unit =
    val chooser = new FileChooser()
    chooser.getExtensionFilters.addAll(
      new FileChooser.ExtensionFilter("Synth Files", "*.synth"),
      new FileChooser.ExtensionFilter("WAV Files", "*.wav")
    )

    filterObj.foreach { filter =>
      chooser.getExtensionFilters
        .filtered(f => f.getExtensions.contains(filter))
        .stream()
        .findFirst()
        .ifPresent(chooser.setSelectedExtensionFilter)
    }

    currentFile.foreach { file =>
      val name = file.getName.replaceFirst("\\.[^.]+$", "")
      chooser.setInitialFileName(name)
      chooser.setInitialDirectory(file.getParentFile)
    }

    val file = chooser.showSaveDialog(getWindow())
    if file != null then
      val path = file.toPath
      try
        if path.toString.endsWith(".wav") then
          val audio = TrackSynthesizer.synthesize(viewModel.toModel(), 1)
          val is16Bit = UserPreferences.export16Bit.get
          val bytes =
            if is16Bit then audio.toBytes16LE else audio.toBytesUnsigned
          val bits = if is16Bit then 16 else 8
          val wav = WavWriter.write(bytes, bits)
          Files.write(path, wav)
        else
          val bytes = SynthWriter.write(viewModel.toModel())
          Files.write(path, bytes)
          currentFile = Some(file)
          viewModel.setCurrentFilePath(file.getAbsolutePath)
      catch case e: Exception => scribe.error(e)
