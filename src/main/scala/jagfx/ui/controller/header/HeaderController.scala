package jagfx.ui.controller.header

import javafx.scene.layout._
import javafx.scene.control._
import javafx.geometry._
import javafx.scene.text._
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.ui.controller.IController
import jagfx.ui.components.button._
import jagfx.ui.components.field._
import jagfx.Constants
import jagfx.utils._
import javafx.beans.property.SimpleBooleanProperty

/** Header controller containing transport, file, and settings controls. */
class HeaderController(viewModel: SynthViewModel) extends IController[GridPane]:
  import Constants._

  private val audioPlayer = AudioPlayer(viewModel)
  private val fileOps = FileOperations(viewModel, () => view.getScene.getWindow)

  /** Callback for playhead position updates. */
  def onPlayheadUpdate: Double => Unit = audioPlayer.onPlayheadUpdate
  def onPlayheadUpdate_=(f: Double => Unit): Unit =
    audioPlayer.onPlayheadUpdate = f

  protected val view = GridPane()
  view.getStyleClass.add("header-grid")
  view.getStyleClass.add("header-root")

  private val col1 = ColumnConstraints()
  col1.setPercentWidth(20)
  col1.setHalignment(HPos.LEFT)

  private val col2 = ColumnConstraints()
  col2.setPercentWidth(60)
  col2.setHalignment(HPos.CENTER)

  private val col3 = ColumnConstraints()
  col3.setPercentWidth(20)
  col3.setHalignment(HPos.RIGHT)

  view.getColumnConstraints.addAll(col1, col2, col3)

  private val LoopParamSize = 34

  private val transportGroup = createTransportGroup()
  private val tgtGroup = createTargetGroup()
  private val lenPosGroup = createLenPosGroup()
  private val loopGroup = createLoopGroup()
  private val fileGroup = createFileGroup()
  private val btn16 = create16BitButton()

  private val leftGroup = HBox(2)
  leftGroup.setAlignment(Pos.CENTER_LEFT)
  leftGroup.setPickOnBounds(false)

  leftGroup.getChildren.add(transportGroup)

  private val centerGroup = HBox(2)
  centerGroup.setAlignment(Pos.CENTER)
  centerGroup.setPickOnBounds(false)
  centerGroup.getChildren.addAll(tgtGroup, lenPosGroup, loopGroup, btn16)

  private val rightGroup = HBox(2)
  rightGroup.setAlignment(Pos.CENTER_RIGHT)
  rightGroup.setPickOnBounds(false)
  rightGroup.getChildren.addAll(fileGroup)

  view.add(leftGroup, 0, 0)
  view.add(centerGroup, 1, 0)
  view.add(rightGroup, 2, 0)

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
    group.getStyleClass.add("h-grp")
    group

  private def createTargetGroup(): HBox =
    val group = HBox(2)
    group.getStyleClass.add("h-grp")

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
    group.getStyleClass.add("h-grp")
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
    group.getStyleClass.add("h-grp")

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
      UserPreferences.export16Bit.set(!UserPreferences.export16Bit.get)
    )
    UserPreferences.export16Bit.addListener((_, _, enabled) =>
      btn16.setActive(enabled)
    )
    btn16.setActive(UserPreferences.export16Bit.get)
    btn16
