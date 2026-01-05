package jagfx.ui.controller.header

import jagfx.Constants
import jagfx.ui.components.button.*
import jagfx.ui.components.field.*
import jagfx.ui.controller.ControllerLike
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.utils.*
import javafx.geometry.*
import javafx.scene.control.*
import javafx.scene.layout.*

// Constants
private final val LoopParamSize = 120

/** Header controller containing transport, file, and settings controls. */
class HeaderController(viewModel: SynthViewModel)
    extends ControllerLike[GridPane]:
  import Constants._

  // Fields
  protected val view: GridPane = GridPane()
  private val audioPlayer = AudioPlayer(viewModel)
  private val fileOps = FileOperations(viewModel, () => view.getScene.getWindow)
  private val col1 = ColumnConstraints()
  private val col2 = ColumnConstraints()
  private val col3 = ColumnConstraints()
  private val transportGroup = createTransportGroup()
  private val tgtGroup = createTargetGroup()
  private val lenPosGroup = createLenPosGroup()
  private val loopGroup = createLoopGroup()
  private val fileGroup = createFileGroup()
  private val btn16Bit = create16BitButton()
  private val leftGroup = HBox(2)
  private val centerGroup = HBox(2)
  private val rightGroup = HBox(2)

  // Init: styling
  view.getStyleClass.add("header-grid")
  view.getStyleClass.add("header-root")

  col1.setPercentWidth(20)
  col1.setHalignment(HPos.LEFT)
  col2.setPercentWidth(60)
  col2.setHalignment(HPos.CENTER)
  col3.setPercentWidth(20)
  col3.setHalignment(HPos.RIGHT)

  leftGroup.setAlignment(Pos.CENTER_LEFT)
  leftGroup.setPickOnBounds(false)

  centerGroup.setAlignment(Pos.CENTER)
  centerGroup.setPickOnBounds(false)

  rightGroup.setAlignment(Pos.CENTER_RIGHT)
  rightGroup.setPickOnBounds(false)

  // Init: build hierarchy
  view.getColumnConstraints.addAll(col1, col2, col3)

  leftGroup.getChildren.add(transportGroup)
  centerGroup.getChildren.addAll(tgtGroup, lenPosGroup, loopGroup, btn16Bit)
  rightGroup.getChildren.addAll(fileGroup)

  view.add(leftGroup, 0, 0)
  view.add(centerGroup, 1, 0)
  view.add(rightGroup, 2, 0)

  /** Callback for playhead position updates. */
  def onPlayheadUpdate: Double => Unit = audioPlayer.onPlayheadUpdate

  /** Sets playhead update callback. */
  def onPlayheadUpdate_=(f: Double => Unit): Unit =
    audioPlayer.onPlayheadUpdate = f

  private def createTransportGroup(): HBox =
    val btnPlay = JagButton()
    btnPlay.setGraphic(IconUtils.icon("mdi2p-play"))
    btnPlay.setTooltip(
      new Tooltip("Play current tone, or all tones if TGT=ALL")
    )
    val btnStop = JagButton()
    btnStop.setGraphic(IconUtils.icon("mdi2s-stop"))
    btnStop.setTooltip(new Tooltip("Stop playback"))
    val btnLoop = JagButton()
    btnLoop.setGraphic(IconUtils.icon("mdi2r-repeat"))
    btnLoop.setId("btn-loop")
    btnLoop.setTooltip(
      new Tooltip("Toggle loop playback between start and end")
    )

    btnPlay.setOnAction(_ => audioPlayer.play())
    btnStop.setOnAction(_ => audioPlayer.stop())
    btnLoop.setOnAction(_ =>
      viewModel.loopEnabledProperty.set(!viewModel.isLoopEnabled)
    )

    viewModel.loopEnabledProperty.addListener((_, _, enabled) =>
      btnLoop.setActive(enabled)
    )

    val group = HBox(2, btnPlay, btnStop, btnLoop)
    group.getStyleClass.add("height-grp")
    group

  private def createTargetGroup(): HBox =
    val group = HBox(2)
    group.getStyleClass.add("height-grp")

    val btnAll = JagButton("ALL")
    btnAll.setTooltip(new Tooltip("Edit affects all enabled tones"))
    val btnOne = JagButton("ONE")
    btnOne.setTooltip(new Tooltip("Edit affects only active tone"))

    btnAll.setOnAction(_ => viewModel.targetModeProperty.set(true))
    btnOne.setOnAction(_ => viewModel.targetModeProperty.set(false))

    viewModel.targetModeProperty.addListener((_, _, isAll) =>
      btnAll.setActive(isAll)
      btnOne.setActive(!isAll)
    )
    btnOne.setActive(true)

    group.getChildren.addAll(Label("TGT"), btnOne, btnAll)
    group

  private def createLenPosGroup(): HBox =
    val group = HBox(2)
    group.getStyleClass.add("height-grp")
    val durationField = JagNumericField(0, Int16.Range, 1200)
    durationField.setEditable(false)
    durationField.setTooltip(
      new Tooltip("Total tone duration in samples (read-only)")
    )
    durationField.valueProperty.bindBidirectional(
      viewModel.totalDurationProperty
    )
    val startOffsetField = JagNumericField(0, Int16.Range, 0)
    startOffsetField.setTooltip(new Tooltip("Start offset in samples"))
    group.getChildren.addAll(
      Label("DUR:"),
      durationField,
      Label("STO:"),
      startOffsetField
    )
    group

  private def createLoopGroup(): HBox =
    val group = HBox(2)
    group.getStyleClass.add("height-grp")

    val fldL1 = JagNumericField(0, Int16.Range, 0)
    fldL1.setPrefWidth(LoopParamSize)
    fldL1.setTooltip(new Tooltip("Loop start position in samples"))
    fldL1.valueProperty.bindBidirectional(viewModel.loopStartProperty)

    val fldL2 = JagNumericField(0, Int16.Range, 0)
    fldL2.setPrefWidth(LoopParamSize)
    fldL2.setTooltip(new Tooltip("Loop end position in samples"))
    fldL2.valueProperty.bindBidirectional(viewModel.loopEndProperty)

    val fldCnt = JagNumericField(0, 100, 0)
    fldCnt.setPrefWidth(LoopParamSize - 10)
    fldCnt.setTooltip(new Tooltip("Number of loop repetitions (preview only)"))
    fldCnt.valueProperty.bindBidirectional(viewModel.loopCountProperty)

    val lblStart = Label("S:")
    lblStart.getStyleClass.add("label")
    lblStart.setMinWidth(Region.USE_PREF_SIZE)

    val lblEnd = Label("E:")
    lblEnd.getStyleClass.add("label")
    lblEnd.setMinWidth(Region.USE_PREF_SIZE)

    group.getChildren.addAll(
      lblStart,
      fldL1,
      lblEnd,
      fldL2,
      Label("xN"),
      fldCnt
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

    val btnOpen = JagButton()
    btnOpen.setGraphic(IconUtils.icon("mdi2f-folder-open"))
    btnOpen.setTooltip(new Tooltip("Open .synth file"))
    val btnSave = JagButton()
    btnSave.setGraphic(IconUtils.icon("mdi2c-content-save"))
    btnSave.setTooltip(new Tooltip("Save current file"))
    val btnExport = JagButton()
    btnExport.setGraphic(IconUtils.icon("mdi2e-export-variant"))
    btnExport.setTooltip(new Tooltip("Save as .synth or export to WAV"))

    btnOpen.setOnAction(_ => fileOps.open())
    btnSave.setOnAction(_ => fileOps.save())
    btnExport.setOnAction(_ => fileOps.saveAs())

    group.getChildren.addAll(btnOpen, btnSave, btnExport)
    group

  private def create16BitButton(): JagButton =
    val btn16 = JagButton("16-BIT")
    btn16.setTooltip(new Tooltip("8-bit (OFF) / 16-bit (ON)"))
    btn16.setOnAction(_ =>
      UserPrefs.export16Bit.set(!UserPrefs.export16Bit.get)
    )
    UserPrefs.export16Bit.addListener((_, _, enabled) =>
      btn16.setActive(enabled)
    )
    btn16.setActive(UserPrefs.export16Bit.get)
    btn16
