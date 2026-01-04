package jagfx.ui.controller.inspector

import javafx.scene.layout._
import javafx.scene.control._
import javafx.geometry.Pos
import jagfx.ui.viewmodel.EnvelopeViewModel
import jagfx.ui.components.field.JagNumericField
import jagfx.ui.components.button.JagButton
import jagfx.utils.IconUtils
import jagfx.Constants.Int16

private val HeaderWidth = 55

class EnvelopeSegmentEditor extends VBox:
  private var currentModel: Option[EnvelopeViewModel] = None

  private val contentBox = VBox(2)
  private val addButton = JagButton()

  setSpacing(0) // Table look
  getStyleClass.add("segment-table")

  // Table Header
  private val headerRow = createHeader()

  private val scrollPane = new ScrollPane()
  scrollPane.setContent(contentBox)
  scrollPane.setFitToWidth(true)
  scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER)
  scrollPane.getStyleClass.add("segment-table-scroll")
  VBox.setVgrow(scrollPane, Priority.ALWAYS)

  addButton.setGraphic(IconUtils.icon("mdi2p-plus", 14))
  addButton.setMaxWidth(Double.MaxValue)
  addButton.getStyleClass.add("segment-add-btn")
  addButton.setTooltip(new Tooltip("Add new envelope segment"))
  addButton.setOnAction(_ => addSegment())

  getChildren.addAll(headerRow, scrollPane, addButton)

  private var currentListener: Option[() => Unit] = None

  def bind(model: EnvelopeViewModel): Unit =
    // clean up prev listener
    currentModel.foreach(m =>
      currentListener.foreach(l => m.removeChangeListener(l))
    )

    currentModel = Some(model)
    val listener = () => refresh()
    currentListener = Some(listener)
    model.addChangeListener(listener)

    refresh()

  private def refresh(): Unit =
    contentBox.getChildren.clear()
    currentModel.foreach { model =>
      model.getFullSegments.zipWithIndex.foreach { case (seg, idx) =>
        contentBox.getChildren.add(createRow(idx, seg.duration, seg.peak))
      }
    }

  private def createHeader(): HBox =
    val box = HBox(0)
    box.getStyleClass.add("segment-table-header")
    box.setAlignment(Pos.CENTER_LEFT)

    val lblIdx = new Label("#")
    lblIdx.setPrefWidth(32)
    lblIdx.getStyleClass.add("h-head-small")
    lblIdx.setAlignment(Pos.CENTER)
    lblIdx.setStyle("-fx-alignment: center;")

    val lblDur = new Label("DUR")
    lblDur.setPrefWidth(HeaderWidth)
    lblDur.getStyleClass.add("h-head-small")
    lblDur.setAlignment(Pos.CENTER_LEFT)

    val lblPeak = new Label("PEAK")
    lblPeak.setPrefWidth(HeaderWidth)
    lblPeak.getStyleClass.add("h-head-small")
    lblPeak.setAlignment(Pos.CENTER_LEFT)

    val lblAct = new Label("")
    lblAct.setPrefWidth(20)

    box.getChildren.addAll(lblIdx, lblDur, lblPeak, lblAct)
    box

  private def createRow(index: Int, duration: Int, peak: Int): HBox =
    val row = HBox(0)
    row.getStyleClass.add("segment-table-row")
    row.setAlignment(Pos.CENTER_LEFT)

    val idxLbl = Label((index + 1).toString)
    idxLbl.setPrefWidth(32) // Match header
    idxLbl.getStyleClass.add("dim-label")
    idxLbl.setAlignment(Pos.CENTER)

    val durField = JagNumericField(0, Int16.Range, duration)
    durField.setPrefWidth(55)
    durField.getStyleClass.add("table-field")
    durField.valueProperty.addListener((_, _, nv) =>
      update(index, nv.intValue, peak)
    )

    val peakField = JagNumericField(0, Int16.Range, peak)
    peakField.setPrefWidth(55)
    peakField.getStyleClass.add("table-field")
    peakField.valueProperty.addListener((_, _, nv) =>
      update(index, duration, nv.intValue)
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
    currentModel.foreach(_.addSegment(100, 32768))

  private def remove(index: Int): Unit =
    currentModel.foreach(_.removeSegment(index))

  private def update(index: Int, d: Int, p: Int): Unit =
    // avoid redundant updates IF coming FROM listener refresh
    currentModel.foreach { m =>
      val segs = m.getFullSegments
      if index < segs.length then
        val curr = segs(index)
        if curr.duration != d || curr.peak != p then
          m.updateSegment(index, d, p)
    }
