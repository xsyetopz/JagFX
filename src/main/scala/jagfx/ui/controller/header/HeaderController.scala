package jagfx.ui.controller.header

import javafx.scene.layout._
import javafx.scene.control._
import javafx.geometry._
import javafx.scene.text._
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.ui.controller.ControllerLike
import jagfx.ui.components.button._
import jagfx.ui.components.field._
import jagfx.constants
import jagfx.utils._
import javafx.beans.property.SimpleBooleanProperty

private val _LoopParamSize = 34

/** Header controller containing transport, file, and settings controls. */
class HeaderController(viewModel: SynthViewModel)
    extends ControllerLike[GridPane]:
  import constants._

  private val _audioPlayer = AudioPlayer(viewModel)
  private val _fileOps =
    FileOperations(viewModel, () => view.getScene.getWindow)

  /** Callback for playhead position updates. */
  def onPlayheadUpdate: Double => Unit = _audioPlayer.onPlayheadUpdate
  def onPlayheadUpdate_=(f: Double => Unit): Unit =
    _audioPlayer.onPlayheadUpdate = f

  protected val view = GridPane()
  view.getStyleClass.add("header-grid")
  view.getStyleClass.add("header-root")

  private val _col1 = ColumnConstraints()
  _col1.setPercentWidth(20)
  _col1.setHalignment(HPos.LEFT)

  private val _col2 = ColumnConstraints()
  _col2.setPercentWidth(60)
  _col2.setHalignment(HPos.CENTER)

  private val _col3 = ColumnConstraints()
  _col3.setPercentWidth(20)
  _col3.setHalignment(HPos.RIGHT)

  view.getColumnConstraints.addAll(_col1, _col2, _col3)

  private val _transportGroup = _createTransportGroup()
  private val _tgtGroup = _createTargetGroup()
  private val _lenPosGroup = _createLenPosGroup()
  private val _loopGroup = _createLoopGroup()
  private val _fileGroup = _createFileGroup()
  private val _btn16Bit = _create16BitButton()

  private val _leftGroup = HBox(2)
  _leftGroup.setAlignment(Pos.CENTER_LEFT)
  _leftGroup.setPickOnBounds(false)

  _leftGroup.getChildren.add(_transportGroup)

  private val _centerGroup = HBox(2)
  _centerGroup.setAlignment(Pos.CENTER)
  _centerGroup.setPickOnBounds(false)
  _centerGroup.getChildren.addAll(
    _tgtGroup,
    _lenPosGroup,
    _loopGroup,
    _btn16Bit
  )

  private val _rightGroup = HBox(2)
  _rightGroup.setAlignment(Pos.CENTER_RIGHT)
  _rightGroup.setPickOnBounds(false)
  _rightGroup.getChildren.addAll(_fileGroup)

  view.add(_leftGroup, 0, 0)
  view.add(_centerGroup, 1, 0)
  view.add(_rightGroup, 2, 0)

  private def _createTransportGroup(): HBox =
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

    btnPlay.setOnAction(_ => _audioPlayer.play())
    btnStop.setOnAction(_ => _audioPlayer.stop())
    btnLoop.setOnAction(_ =>
      viewModel.loopEnabledProperty.set(!viewModel.isLoopEnabled)
    )

    viewModel.loopEnabledProperty.addListener((_, _, enabled) =>
      btnLoop.setActive(enabled)
    )

    val group = HBox(2, btnPlay, btnStop, btnLoop)
    group.getStyleClass.add("h-grp")
    group

  private def _createTargetGroup(): HBox =
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

  private def _createLenPosGroup(): HBox =
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

  private def _createLoopGroup(): HBox =
    val group = HBox(2)
    group.getStyleClass.add("h-grp")

    val fldL1 = JagNumericField(0, Int16.Range, 0)
    fldL1.setPrefWidth(_LoopParamSize)
    fldL1.setTooltip(new Tooltip("Loop start position in samples"))
    fldL1.valueProperty.bindBidirectional(viewModel.loopStartProperty)

    val fldL2 = JagNumericField(0, Int16.Range, 0)
    fldL2.setPrefWidth(_LoopParamSize)
    fldL2.setTooltip(new Tooltip("Loop end position in samples"))
    fldL2.valueProperty.bindBidirectional(viewModel.loopEndProperty)

    val fldCnt = JagNumericField(0, 100, 0)
    fldCnt.setPrefWidth(_LoopParamSize - 10)
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

  private def _createFileGroup(): HBox =
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

    btnOpen.setOnAction(_ => _fileOps.open())
    btnSave.setOnAction(_ => _fileOps.save())
    btnExport.setOnAction(_ => _fileOps.saveAs())

    group.getChildren.addAll(btnOpen, btnSave, btnExport)
    group

  private def _create16BitButton(): JagButton =
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
