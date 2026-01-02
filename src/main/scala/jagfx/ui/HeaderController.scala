package jagfx.ui

import javafx.embed.swing.SwingFXUtils
import javafx.scene.layout._
import javafx.scene.control.Label
import javafx.stage.FileChooser
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.ui.components._
import jagfx.io._
import jagfx.synth.TrackSynthesizer
import java.io.File
import javax.sound.sampled._
import scala.util.Using
import java.nio.file.Files
import jagfx.Constants
import jagfx.utils.{AudioUtils, IconUtils}
import javafx.geometry.Pos

class HeaderController(viewModel: SynthViewModel):
  import Constants._
  private var currentFile: Option[File] = None
  private var currentClip: Option[Clip] = None

  private val view = HBox()
  view.getStyleClass.add("header")
  view.setSpacing(4)
  view.setAlignment(Pos.CENTER_LEFT)

  private val title = Label("JAGFX")
  title.setStyle(
    "-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #f0f0f0; -fx-padding: 0 4px 0 0;"
  )

  private val btnPlay = JagButton("")
  btnPlay.setGraphic(IconUtils.icon("mdi2p-play"))
  private val btnStop = JagButton("")
  btnStop.setGraphic(IconUtils.icon("mdi2s-stop"))
  private val btnLoop = JagButton("")
  btnLoop.setGraphic(IconUtils.icon("mdi2r-repeat"))
  btnLoop.setId("btn-loop")

  btnPlay.setOnAction(_ => playAudio())
  btnStop.setOnAction(_ => stopAudio())

  private val transportGroup = HBox(4, btnPlay, btnStop, btnLoop)
  transportGroup.getStyleClass.add("h-grp")

  private val tgtGroup = HBox(4)
  tgtGroup.getStyleClass.add("h-grp")
  private val btnTone = JagButton("TONE")
  private val btnAll = JagButton("ALL")

  btnTone.setOnAction(_ => viewModel.targetModeProperty.set(false))
  btnAll.setOnAction(_ => viewModel.targetModeProperty.set(true))

  viewModel.targetModeProperty.addListener((_, _, isAll) =>
    btnTone.setActive(!isAll)
    btnAll.setActive(isAll)
  )
  btnTone.setActive(true)

  tgtGroup.getChildren.addAll(Label("TGT"), btnTone, btnAll)

  private val lenPosGroup = HBox(4)
  lenPosGroup.getStyleClass.add("h-grp")
  val lenField = JagNumericField(0, Int16.Range, 1200)
  val posField = JagNumericField(0, Int16.Range, 0)
  lenPosGroup.getChildren.addAll(Label("LEN"), lenField, Label("POS"), posField)

  private val loopGroup = HBox(4)
  loopGroup.getStyleClass.add("h-grp")

  private val l1 = JagNumericField(0, Int16.Range, 0)
  l1.setPrefWidth(34)
  l1.valueProperty.bindBidirectional(viewModel.loopStartProperty)

  private val l2 = JagNumericField(0, Int16.Range, 0)
  l2.setPrefWidth(34)
  l2.valueProperty.bindBidirectional(viewModel.loopEndProperty)

  private val cnt = JagNumericField(0, 100, 0)
  cnt.setPrefWidth(24)
  cnt.valueProperty.bindBidirectional(viewModel.loopCountProperty)

  loopGroup.getChildren.addAll(
    Label("L1"),
    l1,
    Label("L2"),
    l2,
    Label("CNT"),
    cnt
  )

  btnLoop.setOnAction(_ =>
    viewModel.loopEnabledProperty.set(!viewModel.isLoopEnabled)
  )

  viewModel.loopEnabledProperty.addListener((_, _, enabled) =>
    btnLoop.setActive(enabled)
    loopGroup.setDisable(!enabled)
  )
  loopGroup.setDisable(true)

  private val spacer = Region()
  HBox.setHgrow(spacer, Priority.ALWAYS)

  private val fileGroup = HBox(4)
  fileGroup.setStyle("-fx-border-color: transparent;")
  fileGroup.setAlignment(Pos.CENTER)

  private val btnOpen = JagButton("")
  btnOpen.setGraphic(IconUtils.icon("mdi2f-folder-open"))
  private val btnSave = JagButton("")
  btnSave.setGraphic(IconUtils.icon("mdi2c-content-save"))
  private val btnExport = JagButton("")
  btnExport.setGraphic(IconUtils.icon("mdi2e-export-variant"))

  btnOpen.setOnAction(_ => openFile())
  btnSave.setOnAction(_ => saveFile())
  btnExport.setOnAction(_ => saveAsOrExport())

  fileGroup.getChildren.addAll(btnOpen, btnSave, btnExport)

  private val leftGroup = HBox(4)
  leftGroup.setAlignment(Pos.CENTER_LEFT)
  leftGroup.setMinWidth(170)
  leftGroup.setPrefWidth(170)
  leftGroup.setMaxWidth(170)
  leftGroup.getChildren.addAll(title, transportGroup)

  private val centerGroup = HBox(4)
  centerGroup.setAlignment(Pos.CENTER)
  HBox.setHgrow(centerGroup, Priority.ALWAYS)
  centerGroup.getChildren.addAll(tgtGroup, lenPosGroup, loopGroup)

  private val rightGroup = HBox(4)
  rightGroup.setAlignment(Pos.CENTER_RIGHT)
  rightGroup.setMinWidth(170)
  rightGroup.setPrefWidth(170)
  rightGroup.setMaxWidth(170)
  rightGroup.getChildren.add(fileGroup)

  view.getChildren.addAll(leftGroup, centerGroup, rightGroup)

  def getView: HBox = view

  private def openFile(): Unit =
    val chooser = new FileChooser()
    chooser.getExtensionFilters.add(
      new FileChooser.ExtensionFilter("Synth Files", "*.synth")
    )
    val file = chooser.showOpenDialog(view.getScene.getWindow)
    if file != null then
      SynthReader.readFromPath(file.toPath) match
        case Right(synth) =>
          viewModel.load(synth)
          currentFile = Some(file)
        case Left(err) => scribe.error(s"Failed to load: ${err.message}")

  private def saveFile(): Unit =
    currentFile match
      case Some(file) =>
        try
          val bytes = SynthWriter.write(viewModel.toModel())
          Files.write(file.toPath, bytes)
        catch
          case e: Exception => scribe.error(s"Failed to save: ${e.getMessage}")
      case None => saveAsOrExport(Some("*.synth"))

  private def saveAsOrExport(filterObj: Option[String] = None): Unit =
    val chooser = new FileChooser()
    chooser.getExtensionFilters.addAll(
      new FileChooser.ExtensionFilter("Synth Files", "*.synth"),
      new FileChooser.ExtensionFilter("WAV Files", "*.wav")
    )

    if filterObj.isDefined then
      chooser.setSelectedExtensionFilter(
        chooser.getExtensionFilters
          .filtered(f => f.getExtensions.contains(filterObj.get))
          .get(0)
      )

    val file = chooser.showSaveDialog(view.getScene.getWindow)
    if file != null then
      val path = file.toPath
      if path.toString.endsWith(".wav") then
        val audio = TrackSynthesizer.synthesize(viewModel.toModel(), 1)
        val wav = WavWriter.write(audio.toBytesUnsigned)
        Files.write(path, wav)
      else
        val bytes = SynthWriter.write(viewModel.toModel())
        Files.write(path, bytes)
        currentFile = Some(file)

  private def playAudio(): Unit =
    stopAudio()

    val toneFilter =
      if viewModel.isTargetAll then -1 else viewModel.getActiveToneIndex
    val loopCount =
      if viewModel.isLoopEnabled then viewModel.loopCountProperty.get else 1
    val audio =
      TrackSynthesizer.synthesize(viewModel.toModel(), loopCount, toneFilter)

    val clip = AudioSystem.getClip()
    currentClip = Some(clip)

    val format = new AudioFormat(Constants.SampleRate, 16, 1, true, true)
    clip.open(format, audio.toBytes16BE, 0, audio.toBytes16BE.length)

    if viewModel.isLoopEnabled && configureLoopPoints(clip) then
      val count = viewModel.loopCountProperty.get
      clip.loop(if count == 0 then Clip.LOOP_CONTINUOUSLY else count - 1)
    else clip.start()

  /** Configures loop points on clip. Returns `true` if loop points are valid.
    */
  private def configureLoopPoints(clip: Clip): Boolean =
    val startMs = math.max(0, viewModel.loopStartProperty.get)
    val endMs = math.max(startMs, viewModel.loopEndProperty.get)
    if endMs <= startMs then return false

    val startFrames = AudioUtils.msToSamples(startMs)
    val endFrames = AudioUtils.msToSamples(endMs)
    val len = clip.getFrameLength

    val validEnd = math.min(endFrames, len - 1).toInt
    val validStart = math.min(startFrames, validEnd).toInt

    if validEnd > validStart then
      clip.setLoopPoints(validStart, validEnd)
      true
    else false

  private def stopAudio(): Unit =
    currentClip.foreach { clip =>
      if clip.isRunning then clip.stop()
      clip.close()
    }
    currentClip = None
