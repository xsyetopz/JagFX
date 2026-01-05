package jagfx.ui.controller.inspector

import jagfx.constants.Int16
import jagfx.ui.components.button.JagButton
import jagfx.ui.components.field.JagNumericField
import jagfx.ui.components.group.JagToggleGroup
import jagfx.ui.viewmodel.FilterViewModel
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*

/** Editor for filter poles/zeros with direction and time toggle. */
class FilterPolesEditor extends VBox:
  // Fields
  private var currentModel: Option[FilterViewModel] = None
  private var currentDir = 0
  private var showEnd = false
  private val typeGroup = JagToggleGroup(("Zeros (FF)", ""), ("Poles (FB)", ""))
  private val timeBtn = JagButton("S")
  private val contentBox = VBox(2)
  private val headerRow = HBox(2)

  // Init: styling
  setSpacing(4)
  getStyleClass.add("segment-editor")

  typeGroup.setSelected("Zeros (FF)")
  typeGroup.setAlignment(Pos.CENTER)

  timeBtn.setPrefWidth(20)
  headerRow.setAlignment(Pos.CENTER_LEFT)

  // Init: listeners
  typeGroup.selectedProperty.addListener((_, _, newVal) =>
    currentDir = if newVal == "Poles (FB)" then 1 else 0
    refresh()
  )

  timeBtn.setOnAction(_ =>
    showEnd = !showEnd
    timeBtn.setText(if showEnd then "E" else "S")
    refresh()
  )

  // Init: build hierarchy
  headerRow.getChildren.addAll(
    createHead("SLOT", 24),
    createHead("MAG", 28),
    createHead("PHS", 28),
    new Region() { HBox.setHgrow(this, Priority.ALWAYS) },
    timeBtn
  )

  getChildren.addAll(typeGroup, headerRow, contentBox)

  /** Binds filter view model to editor. */
  def bind(model: FilterViewModel): Unit =
    currentModel = Some(model)
    model.addChangeListener(() => refresh())
    refresh()

  private def createHead(text: String, width: Double): Label =
    val l = Label(text)
    l.setPrefWidth(width)
    l.getStyleClass.add("height-head-small")
    l

  private def refresh(): Unit =
    contentBox.getChildren.clear()
    currentModel.foreach { model =>
      val count =
        if currentDir == 0 then model.pairCount0.get else model.pairCount1.get
      for i <- 0 until 4 do
        val active = i < count
        contentBox.getChildren.add(createRow(i, active, model))
    }

  private def createRow(
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

    val pointIdx = if showEnd then 1 else 0
    val magProp = model.pairMagnitude(currentDir)(idx)(pointIdx)
    val phsProp = model.pairPhase(currentDir)(idx)(pointIdx)

    val magField =
      JagNumericField(0, Int16.Range, 0, 100.0 / Int16.Range, "%.0f")
    magField.setPrefWidth(28)
    magField.valueProperty.bindBidirectional(magProp)

    val phsField =
      JagNumericField(0, Int16.Range, 0, 360.0 / Int16.Range, "%.0f")
    phsField.setPrefWidth(28)
    phsField.valueProperty.bindBidirectional(phsProp)

    row.getChildren.addAll(idxLbl, magField, phsField)
    row
