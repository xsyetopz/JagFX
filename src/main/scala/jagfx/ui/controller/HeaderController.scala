package jagfx.ui.controller

import javafx.embed.swing.SwingFXUtils
import javafx.scene.layout._
import javafx.scene.control._
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
import jagfx.utils._
import javafx.geometry._
import javafx.scene.text._
import javafx.animation.AnimationTimer

class HeaderController(viewModel: SynthViewModel) extends IController[GridPane]:
  import Constants._
  private var currentFile: Option[File] = None
  private var currentClip: Option[Clip] = None
  private var playheadTimer: Option[AnimationTimer] = None

  /** Callback for playhead position updates (`0.0` to `1.0`, or `-1` when
    * stopped).
    */
  var onPlayheadUpdate: Double => Unit = _ => ()

  protected val view = GridPane()
  view.getStyleClass.add("header")

  private val col1 = ColumnConstraints()
  col1.setPercentWidth(25)
  col1.setHalignment(HPos.LEFT)

  private val col2 = ColumnConstraints()
  col2.setPercentWidth(50)
  col2.setHalignment(HPos.CENTER)

  private val col3 = ColumnConstraints()
  col3.setPercentWidth(25)
  col3.setHalignment(HPos.CENTER)

  view.getColumnConstraints.addAll(col1, col2, col3)

  private val transportGroup = createTransportGroup()
  private val tgtGroup = createTargetGroup()
  private val lenPosGroup = createLenPosGroup()
  private val loopGroup = createLoopGroup()
  private val fileGroup = createFileGroup()
  private val btn16 = create16BitButton()

  private val leftGroup = HBox(2)
  leftGroup.setAlignment(Pos.CENTER_LEFT)
  leftGroup.setPickOnBounds(false)

  private val title = new TextFlow()
  title.setPadding(new Insets(0, 6, 0, 0))

  val txtJag = new Text("JAG")
  txtJag.setStyle(
    "-fx-fill: #f0f0f0; -fx-font-weight: 900; -fx-font-size: 14px;"
  )
  val txtFx = new Text("FX")
  txtFx.setStyle(
    "-fx-fill: #33bbee; -fx-font-weight: 900; -fx-font-size: 14px;"
  )

  title.getChildren.addAll(txtJag, txtFx)

  val separator =
    new Separator(Orientation.VERTICAL)
  separator.setPadding(new Insets(0, 4, 0, 4))

  leftGroup.getChildren.addAll(title, separator, transportGroup)

  private val centerGroup = HBox(2)
  centerGroup.setAlignment(Pos.CENTER)
  centerGroup.setPickOnBounds(false)
  centerGroup.getChildren.addAll(tgtGroup, lenPosGroup, loopGroup)

  private val rightGroup = HBox(2)
  rightGroup.setAlignment(Pos.CENTER_RIGHT)
  rightGroup.setPickOnBounds(false)
  rightGroup.setMaxWidth(Double.MaxValue)

  // spacer to push 'fileGroup' to right
  val rightSpacer = new Region()
  HBox.setHgrow(rightSpacer, Priority.ALWAYS)

  rightGroup.getChildren.addAll(btn16, rightSpacer, fileGroup)

  view.add(leftGroup, 0, 0)
  view.add(centerGroup, 1, 0)
  view.add(rightGroup, 2, 0)

  private def createTransportGroup(): HBox =
    val btnPlay = JagButton()
    btnPlay.setGraphic(IconUtils.icon("mdi2p-play"))
    val btnStop = JagButton()
    btnStop.setGraphic(IconUtils.icon("mdi2s-stop"))
    val btnLoop = JagButton()
    btnLoop.setGraphic(IconUtils.icon("mdi2r-repeat"))
    btnLoop.setId("btn-loop")

    btnPlay.setOnAction(_ => playAudio())
    btnStop.setOnAction(_ => stopAudio())
    btnLoop.setOnAction(_ =>
      viewModel.loopEnabledProperty.set(!viewModel.isLoopEnabled)
    )

    viewModel.loopEnabledProperty.addListener((_, _, enabled) =>
      btnLoop.setActive(enabled)
    )

    val group = HBox(2, btnPlay, btnStop, btnLoop)
    group.getStyleClass.add("h-grp")
    group

  private def createTargetGroup(): HBox =
    val group = HBox(2)
    group.getStyleClass.add("h-grp")

    val btnTone = JagButton("TONE")
    val btnAll = JagButton("ALL")
    btnTone.setOnAction(_ => viewModel.targetModeProperty.set(false))
    btnAll.setOnAction(_ => viewModel.targetModeProperty.set(true))

    viewModel.targetModeProperty.addListener((_, _, isAll) =>
      btnTone.setActive(!isAll)
      btnAll.setActive(isAll)
    )
    btnTone.setActive(true)

    group.getChildren.addAll(Label("TGT"), btnTone, btnAll)
    group

  private def createLenPosGroup(): HBox =
    val group = HBox(2)
    group.getStyleClass.add("h-grp")
    val lenField = JagNumericField(0, Int16.Range, 1200)
    lenField.valueProperty.bindBidirectional(viewModel.totalDurationProperty)
    val posField = JagNumericField(0, Int16.Range, 0)
    group.getChildren.addAll(Label("LEN"), lenField, Label("POS"), posField)
    group

  private def createLoopGroup(): HBox =
    val group = HBox(2)
    group.getStyleClass.add("h-grp")

    val l1 = JagNumericField(0, Int16.Range, 0)
    l1.setPrefWidth(34)
    l1.valueProperty.bindBidirectional(viewModel.loopStartProperty)

    val l2 = JagNumericField(0, Int16.Range, 0)
    l2.setPrefWidth(34)
    l2.valueProperty.bindBidirectional(viewModel.loopEndProperty)

    val cnt = JagNumericField(0, 100, 0)
    cnt.setPrefWidth(24)
    cnt.valueProperty.bindBidirectional(viewModel.loopCountProperty)

    group.getChildren.addAll(
      Label("L1"),
      l1,
      Label("L2"),
      l2,
      Label("CNT"),
      cnt
    )

    viewModel.loopEnabledProperty.addListener((_, _, enabled) =>
      group.setDisable(!enabled)
    )
    group.setDisable(true)
    group

  private def createFileGroup(): HBox =
    val group = HBox(2)
    group.setStyle("-fx-border-color: transparent;")
    group.setAlignment(Pos.CENTER)

    val btnInit = JagButton()
    btnInit.setGraphic(IconUtils.icon("mdi2f-file-plus"))

    val btnOpen = JagButton()
    btnOpen.setGraphic(IconUtils.icon("mdi2f-folder-open"))
    val btnSave = JagButton()
    btnSave.setGraphic(IconUtils.icon("mdi2c-content-save"))
    val btnExport = JagButton()
    btnExport.setGraphic(IconUtils.icon("mdi2e-export-variant"))

    btnInit.setOnAction(_ => viewModel.reset())
    btnOpen.setOnAction(_ => openFile())
    btnSave.setOnAction(_ => saveFile())
    btnExport.setOnAction(_ => saveAsOrExport())

    group.getChildren.addAll(btnInit, btnOpen, btnSave, btnExport)
    group

  private def create16BitButton(): JagButton =
    val btn16 = JagButton("16-BIT")
    btn16.setOnAction(_ =>
      UserPreferences.export16Bit.set(!UserPreferences.export16Bit.get)
    )
    UserPreferences.export16Bit.addListener((_, _, enabled) =>
      btn16.setActive(enabled)
    )
    btn16.setActive(UserPreferences.export16Bit.get)
    btn16

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
          viewModel.setCurrentFilePath(file.getAbsolutePath)
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
        val is16Bit = UserPreferences.export16Bit.get
        val bytes = if is16Bit then audio.toBytes16LE else audio.toBytesUnsigned
        val bits = if is16Bit then 16 else 8
        val wav = WavWriter.write(bytes, bits)
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
    val bytes = audio.toBytes16BE
    clip.open(format, bytes, 0, bytes.length)

    if viewModel.isLoopEnabled && configureLoopPoints(clip) then
      val count = viewModel.loopCountProperty.get
      clip.loop(if count == 0 then Clip.LOOP_CONTINUOUSLY else count - 1)
    else clip.start()

    val totalFrames = clip.getFrameLength.toDouble
    val timer = new AnimationTimer:
      def handle(now: Long): Unit =
        currentClip.foreach { c =>
          if c.isRunning then
            val pos = c.getFramePosition.toDouble / totalFrames
            onPlayheadUpdate(pos)
          else
            onPlayheadUpdate(-1)
            this.stop()
        }
    timer.start()
    playheadTimer = Some(timer)

  /** Configures loop points on clip.
    *
    * Returns `true` if loop points are valid.
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
    playheadTimer.foreach(_.stop())
    playheadTimer = None
    onPlayheadUpdate(-1)
    currentClip.foreach { clip =>
      if clip.isRunning then clip.stop()
      clip.close()
    }
    currentClip = None
