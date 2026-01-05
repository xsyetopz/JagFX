package jagfx.ui.controller.inspector

import jagfx.constants.Int16
import jagfx.ui.components.field.*
import jagfx.ui.viewmodel.FilterViewModel
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.*

/** Inspector panel for filter parameters. */
class FilterInspector extends VBox:
  // Fields
  private var currentFilter: Option[FilterViewModel] = None
  private val pairsLabel = Label("PAIRS")
  private val ffField = JagNumericField(0, 4, 0)
  private val fbField = JagNumericField(0, 4, 0)
  private val pairsRow = HBox(4)
  private val unityLabel = Label("UNITY")
  private val unity0Field = JagNumericField(0, Int16.Range, 0)
  private val unity1Field = JagNumericField(0, Int16.Range, 0)
  private val unityRow = HBox(4)
  private val polesLabel = Label("POLES")
  private val polesEditor = FilterPolesEditor()

  // Init: styling
  setSpacing(8)

  pairsLabel.getStyleClass.addAll("label", "height-head")
  ffField.setPrefWidth(28)
  fbField.setPrefWidth(28)
  pairsRow.setAlignment(Pos.CENTER_LEFT)

  unityLabel.getStyleClass.addAll("label", "height-head")
  unity0Field.setPrefWidth(55)
  unity1Field.setPrefWidth(55)
  unityRow.setAlignment(Pos.CENTER_LEFT)

  polesLabel.getStyleClass.addAll("label", "height-head")

  // Init: listeners
  ffField.valueProperty.addListener((_, _, nv) =>
    currentFilter.foreach(_.pairCount0.set(nv.intValue))
  )
  fbField.valueProperty.addListener((_, _, nv) =>
    currentFilter.foreach(_.pairCount1.set(nv.intValue))
  )

  unity0Field.valueProperty.addListener((_, _, nv) =>
    currentFilter.foreach(_.unity0.set(nv.intValue))
  )
  unity1Field.valueProperty.addListener((_, _, nv) =>
    currentFilter.foreach(_.unity1.set(nv.intValue))
  )

  // Init: build hierarchy
  pairsRow.getChildren.addAll(
    Label("FF"),
    ffField,
    new Region() { HBox.setHgrow(this, Priority.ALWAYS) },
    Label("FB"),
    fbField
  )

  unityRow.getChildren.addAll(
    Label("S:"),
    unity0Field,
    new Region() { HBox.setHgrow(this, Priority.ALWAYS) },
    Label("E:"),
    unity1Field
  )

  getChildren.addAll(
    pairsLabel,
    pairsRow,
    unityLabel,
    unityRow,
    polesLabel,
    polesEditor
  )

  /** Binds filter view model to inspector. */
  def bind(filter: FilterViewModel): Unit =
    currentFilter = Some(filter)
    ffField.setValue(filter.pairCount0.get)
    fbField.setValue(filter.pairCount1.get)
    unity0Field.setValue(filter.unity0.get)
    unity1Field.setValue(filter.unity1.get)
    polesEditor.bind(filter)
