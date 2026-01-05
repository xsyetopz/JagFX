package jagfx.ui.controller.inspector

import jagfx.Constants.Int16
import jagfx.ui.components.button.JagButton
import jagfx.ui.components.field.JagNumericField
import jagfx.ui.viewmodel.EnvelopeViewModel
import jagfx.utils.IconUtils
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*

private val HeaderWidth = 55

/** Editor for envelope segments with add/remove controls. */
class EnvelopeSegmentEditor extends VBox:
  // Fields
  private var currentModel: Option[EnvelopeViewModel] = None
  private var currentListener: Option[() => Unit] = None
  private var isRefreshing: Boolean = false
  private val contentBox = VBox(2)
  private val addButton = JagButton()
  private val tableHeader = createHeader()
  private val scrollPane = new ScrollPane()

  // Init: styling
  setSpacing(0)
  getStyleClass.add("segment-table")

  scrollPane.setContent(contentBox)
  scrollPane.setFitToWidth(true)
  scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER)
  scrollPane.getStyleClass.add("segment-table-scroll")
  VBox.setVgrow(scrollPane, Priority.ALWAYS)

  addButton.setGraphic(IconUtils.icon("mdi2p-plus", 14))
  addButton.setMaxWidth(Double.MaxValue)
  addButton.getStyleClass.add("segment-add-btn")
  addButton.setTooltip(new Tooltip("Add new envelope segment"))

  // Init: listeners
  addButton.setOnAction(_ => addSegment())

  // Init: build hierarchy
  getChildren.addAll(tableHeader, scrollPane, addButton)

  /** Binds envelope view model to editor. */
  def bind(model: EnvelopeViewModel): Unit =
    currentModel.foreach(m =>
      currentListener.foreach(l => m.removeChangeListener(l))
    )
    currentModel = Some(model)
    val listener = () => refresh()
    currentListener = Some(listener)
    model.addChangeListener(listener)
    refresh()

  private def createHeader(): HBox =
    val box = HBox(0)
    box.getStyleClass.add("segment-table-header")
    box.setAlignment(Pos.CENTER_LEFT)

    val lblIdx = new Label("#")
    lblIdx.setPrefWidth(32)
    lblIdx.getStyleClass.add("height-head-small")
    lblIdx.setAlignment(Pos.CENTER)
    lblIdx.setStyle("-fx-alignment: center;")

    val lblDur = new Label("DUR")
    lblDur.setPrefWidth(HeaderWidth)
    lblDur.getStyleClass.add("height-head-small")
    lblDur.setAlignment(Pos.CENTER_LEFT)

    val lblPeak = new Label("PEAK")
    lblPeak.setPrefWidth(HeaderWidth)
    lblPeak.getStyleClass.add("height-head-small")
    lblPeak.setAlignment(Pos.CENTER_LEFT)

    val lblAct = new Label("")
    lblAct.setPrefWidth(20)

    box.getChildren.addAll(lblIdx, lblDur, lblPeak, lblAct)
    box

  private def refresh(): Unit =
    isRefreshing = true
    try
      currentModel.foreach { model =>
        val segments = model.getFullSegments
        val currentRows = contentBox.getChildren
        if currentRows.size > segments.length then
          currentRows.remove(segments.length, currentRows.size)
        if currentRows.size < segments.length then
          for i <- currentRows.size until segments.length do
            val seg = segments(i)
            contentBox.getChildren.add(createRow(i, seg.duration, seg.peak))

        for i <- 0 until segments.length do
          val seg = segments(i)
          val row = currentRows.get(i).asInstanceOf[HBox]
          val durField = row.getChildren.get(1).asInstanceOf[JagNumericField]
          val peakField = row.getChildren.get(2).asInstanceOf[JagNumericField]

          if durField.getValue != seg.duration then
            durField.setValue(seg.duration)
          if peakField.getValue != seg.peak then peakField.setValue(seg.peak)
      }
    finally
      isRefreshing = false

  private def createRow(index: Int, duration: Int, peak: Int): HBox =
    val row = HBox(0)
    row.getStyleClass.add("segment-table-row")
    row.setAlignment(Pos.CENTER_LEFT)

    val idxLbl = Label((index + 1).toString)
    idxLbl.setPrefWidth(32)
    idxLbl.getStyleClass.add("dim-label")
    idxLbl.setAlignment(Pos.CENTER)

    val scale = Int16.Range.toDouble / 100.0
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
      if !isRefreshing then
        val currentPeak = peakField.getValue.intValue
        update(index, nv.intValue, currentPeak)
    )

    peakField.setPrefWidth(55)
    peakField.getStyleClass.add("table-field")
    val peakTip = new Tooltip()
    peakTip.textProperty.bind(
      peakField.valueProperty.asString("Raw: %d").concat(helpText)
    )
    peakField.setTooltip(peakTip)
    peakField.valueProperty.addListener((_, _, nv) =>
      if !isRefreshing then
        val currentDur = durField.getValue.intValue
        update(index, currentDur, nv.intValue)
    )

    val delBtn = JagButton()
    delBtn.setGraphic(IconUtils.icon("mdi2m-minus", 10))
    delBtn.getStyleClass.add("icon-btn-small")
    delBtn.setPrefWidth(20)
    delBtn.setTooltip(new Tooltip("Remove this segment"))
    delBtn.setOnAction(_ => remove(index))

    row.getChildren.addAll(idxLbl, durField, peakField, delBtn)
    row

  private def addSegment(): Unit =
    currentModel.foreach(_.addSegment(100, Int16.UnsignedMaxValue))

  private def remove(index: Int): Unit =
    currentModel.foreach(_.removeSegment(index))

  private def update(index: Int, d: Int, p: Int): Unit =
    currentModel.foreach { m =>
      val segs = m.getFullSegments
      if index < segs.length then
        val curr = segs(index)
        if curr.duration != d || curr.peak != p then
          m.updateSegment(index, d, p)
    }
