package jagfx.ui.controller.inspector

import javafx.scene.layout._
import javafx.scene.control._
import javafx.geometry.Pos
import jagfx.ui.viewmodel.EnvelopeViewModel
import jagfx.ui.components.field.JagNumericField
import jagfx.ui.components.button.JagButton
import jagfx.utils.IconUtils
import jagfx.constants.Int16

private val _HeaderWidth = 55

class EnvelopeSegmentEditor extends VBox:
  private var _currentModel: Option[EnvelopeViewModel] = None
  private var _isRefreshing: Boolean = false

  private val _contentBox = VBox(2)
  private val _addButton = JagButton()

  setSpacing(0) // Table look
  getStyleClass.add("segment-table")

  private val _tableHeader = _createHeader()

  private val _scrollPane = new ScrollPane()
  _scrollPane.setContent(_contentBox)
  _scrollPane.setFitToWidth(true)
  _scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER)
  _scrollPane.getStyleClass.add("segment-table-scroll")
  VBox.setVgrow(_scrollPane, Priority.ALWAYS)

  _addButton.setGraphic(IconUtils.icon("mdi2p-plus", 14))
  _addButton.setMaxWidth(Double.MaxValue)
  _addButton.getStyleClass.add("segment-add-btn")
  _addButton.setTooltip(new Tooltip("Add new envelope segment"))
  _addButton.setOnAction(_ => _addSegment())

  getChildren.addAll(_tableHeader, _scrollPane, _addButton)

  private var _currentListener: Option[() => Unit] = None

  def bind(model: EnvelopeViewModel): Unit =
    // clean up prev listener
    _currentModel.foreach(m =>
      _currentListener.foreach(l => m.removeChangeListener(l))
    )

    _currentModel = Some(model)
    val listener = () => _refresh()
    _currentListener = Some(listener)
    model.addChangeListener(listener)

    _refresh()

  private def _refresh(): Unit =
    _isRefreshing = true
    try
      _currentModel.foreach { model =>
        val segments = model.getFullSegments
        val currentRows = _contentBox.getChildren
        if currentRows.size > segments.length then
          currentRows.remove(segments.length, currentRows.size)
        if currentRows.size < segments.length then
          for i <- currentRows.size until segments.length do
            val seg = segments(i)
            _contentBox.getChildren.add(_createRow(i, seg.duration, seg.peak))

        for i <- 0 until segments.length do
          val seg = segments(i)
          val row = currentRows.get(i).asInstanceOf[HBox]
          // row children: [Label(#), DurField, PeakField, DelBtn]
          val durField = row.getChildren.get(1).asInstanceOf[JagNumericField]
          val peakField = row.getChildren.get(2).asInstanceOf[JagNumericField]

          if durField.getValue != seg.duration then
            durField.setValue(seg.duration)
          if peakField.getValue != seg.peak then peakField.setValue(seg.peak)
      }
    finally
      _isRefreshing = false

  private def _createHeader(): HBox =
    val box = HBox(0)
    box.getStyleClass.add("segment-table-header")
    box.setAlignment(Pos.CENTER_LEFT)

    val lblIdx = new Label("#")
    lblIdx.setPrefWidth(32)
    lblIdx.getStyleClass.add("h-head-small")
    lblIdx.setAlignment(Pos.CENTER)
    lblIdx.setStyle("-fx-alignment: center;")

    val lblDur = new Label("DUR")
    lblDur.setPrefWidth(_HeaderWidth)
    lblDur.getStyleClass.add("h-head-small")
    lblDur.setAlignment(Pos.CENTER_LEFT)

    val lblPeak = new Label("PEAK")
    lblPeak.setPrefWidth(_HeaderWidth)
    lblPeak.getStyleClass.add("h-head-small")
    lblPeak.setAlignment(Pos.CENTER_LEFT)

    val lblAct = new Label("")
    lblAct.setPrefWidth(20)

    box.getChildren.addAll(lblIdx, lblDur, lblPeak, lblAct)
    box

  private def _createRow(index: Int, duration: Int, peak: Int): HBox =
    val row = HBox(0)
    row.getStyleClass.add("segment-table-row")
    row.setAlignment(Pos.CENTER_LEFT)

    val idxLbl = Label((index + 1).toString)
    idxLbl.setPrefWidth(32)
    idxLbl.getStyleClass.add("dim-label")
    idxLbl.setAlignment(Pos.CENTER)

    val scale = Int16.Range.toDouble / 100.0 // 655.35
    val fmt = "%.2f"
    val helpText = "\nScroll: ±1%\nShift: ±10%\nAlt/Cmd: ±0.01%"

    val durField = JagNumericField(0, Int16.Range, duration, scale, fmt)
    durField.setPrefWidth(55)
    durField.getStyleClass.add("table-field")
    val durTip = new Tooltip()
    durTip.textProperty.bind(
      durField.valueProperty.asString("Raw: %d").concat(helpText)
    )
    durField.setTooltip(durTip)

    val peakField = JagNumericField(0, Int16.Range, peak, scale, fmt)
    durField.valueProperty.addListener((_, _, nv) =>
      if !_isRefreshing then
        val currentPeak = peakField.getValue.intValue
        _update(index, nv.intValue, currentPeak)
    )

    peakField.setPrefWidth(55)
    peakField.getStyleClass.add("table-field")
    val peakTip = new Tooltip()
    peakTip.textProperty.bind(
      peakField.valueProperty.asString("Raw: %d").concat(helpText)
    )
    peakField.setTooltip(peakTip)
    peakField.valueProperty.addListener((_, _, nv) =>
      if !_isRefreshing then
        val currentDur = durField.getValue.intValue
        _update(index, currentDur, nv.intValue)
    )

    val delBtn = JagButton()
    delBtn.setGraphic(IconUtils.icon("mdi2m-minus", 10))
    delBtn.getStyleClass.add("icon-btn-small")
    delBtn.setPrefWidth(20)
    delBtn.setTooltip(new Tooltip("Remove this segment"))
    delBtn.setOnAction(_ => _remove(index))

    row.getChildren.addAll(idxLbl, durField, peakField, delBtn)
    row

  private def _addSegment(): Unit =
    _currentModel.foreach(_.addSegment(100, Int16.UnsignedMaxValue))

  private def _remove(index: Int): Unit =
    _currentModel.foreach(_.removeSegment(index))

  private def _update(index: Int, d: Int, p: Int): Unit =
    // avoid redundant updates IF coming FROM listener refresh
    _currentModel.foreach { m =>
      val segs = m.getFullSegments
      if index < segs.length then
        val curr = segs(index)
        if curr.duration != d || curr.peak != p then
          m.updateSegment(index, d, p)
    }
