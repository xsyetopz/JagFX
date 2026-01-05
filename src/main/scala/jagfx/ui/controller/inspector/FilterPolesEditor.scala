package jagfx.ui.controller.inspector

import javafx.scene.layout._
import javafx.scene.control._
import javafx.geometry.Pos
import jagfx.ui.viewmodel.FilterViewModel
import jagfx.ui.components.field.JagNumericField
import jagfx.ui.components.group.JagToggleGroup
import jagfx.constants.Int16
import jagfx.ui.components.button.JagButton

class FilterPolesEditor extends VBox:
  private var _currentModel: Option[FilterViewModel] = None
  private var _currentDir = 0 // 0 = FF (Zeros), 1 = FB (Poles)
  private var _showEnd = false

  private val _typeGroup = JagToggleGroup(
    ("Zeros (FF)", ""),
    ("Poles (FB)", "")
  )

  private val _timeBtn = JagButton("S")

  private val _contentBox = VBox(2)

  setSpacing(4)
  getStyleClass.add("segment-editor")

  _typeGroup.setSelected("Zeros (FF)")
  _typeGroup.setAlignment(Pos.CENTER)
  _typeGroup.selectedProperty.addListener((_, _, newVal) =>
    _currentDir = if newVal == "Poles (FB)" then 1 else 0
    _refresh()
  )

  _timeBtn.setPrefWidth(20)
  _timeBtn.setOnAction(_ =>
    _showEnd = !_showEnd
    _timeBtn.setText(if _showEnd then "E" else "S")
    _refresh()
  )

  private val _headerRow = HBox(2)
  _headerRow.setAlignment(Pos.CENTER_LEFT)
  _headerRow.getChildren.addAll(
    _createHead("SLOT", 24),
    _createHead("MAG", 28),
    _createHead("PHS", 28),
    new Region() { HBox.setHgrow(this, Priority.ALWAYS) },
    _timeBtn
  )

  getChildren.addAll(_typeGroup, _headerRow, _contentBox)

  def bind(model: FilterViewModel): Unit =
    _currentModel = Some(model)
    model.addChangeListener(() => _refresh())
    _refresh()

  private def _createHead(text: String, w: Double): Label =
    val l = Label(text)
    l.setPrefWidth(w)
    l.getStyleClass.add("h-head-small")
    l

  private def _refresh(): Unit =
    _contentBox.getChildren.clear()
    _currentModel.foreach { model =>
      val count =
        if _currentDir == 0 then model.pairCount0.get else model.pairCount1.get
      for i <- 0 until 4 do
        val active = i < count
        _contentBox.getChildren.add(_createRow(i, active, model))
    }

  private def _createRow(
      idx: Int,
      active: Boolean,
      model: FilterViewModel
  ): HBox =
    val row = HBox(2)
    row.setAlignment(Pos.CENTER_LEFT)
    if !active then row.setOpacity(0.5)

    val idxLbl = Label((idx + 1).toString)
    idxLbl.setPrefWidth(24)
    idxLbl.getStyleClass.add("dim-label")

    val pointIdx = if _showEnd then 1 else 0

    val magProp = model.pairMagnitude(_currentDir)(idx)(pointIdx)
    val phsProp = model.pairPhase(_currentDir)(idx)(pointIdx)

    // Mag 0-100%
    val magField =
      JagNumericField(0, Int16.Range, 0, 100.0 / Int16.Range, "%.0f")
    magField.setPrefWidth(28)
    magField.valueProperty.bindBidirectional(magProp)

    // Phase 0-360
    val phsField =
      JagNumericField(0, Int16.Range, 0, 360.0 / Int16.Range, "%.0f")
    phsField.setPrefWidth(28)
    phsField.valueProperty.bindBidirectional(phsProp)

    row.getChildren.addAll(idxLbl, magField, phsField)
    row
