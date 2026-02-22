package jagfx.ui.controller.header

import java.io.File
import java.nio.file.Files

import jagfx.io.*
import jagfx.synth.TrackSynthesizer
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.utils.UserPrefs
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.*

/** File I/O operations for `.synth` files and WAV export. */
class FileOperations(viewModel: SynthViewModel, getWindow: () => Window):
  // Fields
  private var currentFile: Option[File] = None

  /** Opens file chooser and loads selected `.synth` file. */
  def open(): Unit =
    val chooser = new FileChooser()
    chooser.getExtensionFilters.add(
      new FileChooser.ExtensionFilter("Synth Files", "*.synth")
    )
    val file = chooser.showOpenDialog(getWindow())
    if file != null then
      SynthReader.readFromPath(file.toPath) match
        case Right(synth) =>
          if synth.warnings.nonEmpty then
            showWarningDialog(file, synth.warnings)
          viewModel.load(synth)
          viewModel.setCurrentFilePath(file.getAbsolutePath)
          currentFile = Some(file)
        case Left(err) =>
          showErrorDialog(s"Failed to load: ${err.message}")

  /** Saves current file, or prompts Save As if no file loaded. */
  def save(): Unit =
    currentFile match
      case Some(file) =>
        try
          val bytes = SynthWriter.write(viewModel.toModel())
          Files.write(file.toPath, bytes)
        catch
          case e: Exception => scribe.error(s"Failed to save: ${e.getMessage}")
      case None => saveAs(Some("*.synth"))

  /** Opens Save As dialog for `.synth` or WAV export. */
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
          val is16Bit = UserPrefs.export16Bit.get
          val bytes = if is16Bit then audio.toBytes16LE else audio.toUBytes
          val bits = if is16Bit then 16 else 8
          val wav = WavWriter.write(bytes, bits)
          Files.write(path, wav)
        else
          val bytes = SynthWriter.write(viewModel.toModel())
          Files.write(path, bytes)
          currentFile = Some(file)
          viewModel.setCurrentFilePath(file.getAbsolutePath)
      catch case e: Exception => scribe.error(e)

  private def showWarningDialog(file: File, warnings: List[String]): Unit =
    showTechDialog(
      alertType = Alert.AlertType.WARNING,
      title = "Parse Warning",
      header = s"Issues parsing: ${file.getName}",
      content = warnings.mkString("\n") +
        "\n\nPartial data loaded; playback may differ from original.",
      file = Some(file),
      details = warnings
    )

  private def showErrorDialog(msg: String): Unit =
    showTechDialog(
      alertType = Alert.AlertType.ERROR,
      title = "Open Error",
      header = "Could not open file",
      content = msg,
      file = currentFile,
      details = List(msg)
    )

  private def showTechDialog(
      alertType: Alert.AlertType,
      title: String,
      header: String,
      content: String,
      file: Option[File],
      details: List[String]
  ): Unit =
    val alert = new Alert(alertType)
    alert.setTitle(title)
    alert.setHeaderText(header)
    alert.setContentText(content)

    val techReport = buildTechReport(alertType, file, details)

    val textArea = new TextArea(techReport)
    textArea.setEditable(false)
    textArea.setWrapText(false)
    textArea.setMaxWidth(Double.MaxValue)
    textArea.setMaxHeight(Double.MaxValue)
    textArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;")
    GridPane.setVgrow(textArea, Priority.ALWAYS)
    GridPane.setHgrow(textArea, Priority.ALWAYS)

    val expContent = new GridPane()
    expContent.setMaxWidth(Double.MaxValue)
    expContent.add(textArea, 0, 0)

    alert.getDialogPane.setExpandableContent(expContent)
    alert.getDialogPane.setPrefWidth(600)
    alert.showAndWait()

  private def buildTechReport(
      alertType: Alert.AlertType,
      file: Option[File],
      details: List[String]
  ): String =
    val typeLabel =
      if alertType == Alert.AlertType.ERROR then "ERROR" else "WARNING"
    val fileInfo = file match
      case Some(f) =>
        val sizeBytes = f.length()
        val hexDump =
          try
            val bytes = Files.readAllBytes(f.toPath)
            bytes
              .take(64)
              .grouped(16)
              .zipWithIndex
              .map { case (chunk, i) =>
                f"${i * 16}%04X: ${chunk.map(b => f"${b & 0xff}%02X").mkString(" ")}"
              }
              .mkString("\n")
          catch case _: Exception => "(unable to read)"

        Seq(
          s"File: ${f.getAbsolutePath}",
          s"Name: ${f.getName}",
          s"Size: $sizeBytes bytes (0x${sizeBytes.toHexString.toUpperCase})",
          "",
          "Hex (first 64 bytes):",
          hexDump
        ).mkString("\n")
      case None => "File: (none)"

    Seq(
      s"=== JAGFX $typeLabel ===",
      fileInfo,
      "",
      "Details:",
      details.map("  - " + _).mkString("\n"),
      "=".repeat(24)
    ).mkString("\n")
