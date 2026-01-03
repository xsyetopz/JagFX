package jagfx.ui

import javafx.scene.layout._
import javafx.scene.control.Label
import javafx.geometry.Pos
import jagfx.ui.components._
import jagfx.ui.viewmodel.FilterViewModel

/** Inspector panel for filter parameters. */
class FilterInspector extends VBox:
  private var currentFilter: Option[FilterViewModel] = None

  setSpacing(8)

  // Pairs section
  private val pairsLabel = Label("PAIRS")
  pairsLabel.getStyleClass.addAll("label", "h-head")

  private val ffField = JagNumericField(0, 4, 0)
  ffField.setPrefWidth(28)
  ffField.valueProperty.addListener((_, _, nv) =>
    currentFilter.foreach(_.pairCount0.set(nv.intValue))
  )

  private val fbField = JagNumericField(0, 4, 0)
  fbField.setPrefWidth(28)
  fbField.valueProperty.addListener((_, _, nv) =>
    currentFilter.foreach(_.pairCount1.set(nv.intValue))
  )

  private val pairsRow = HBox(4)
  pairsRow.setAlignment(Pos.CENTER_LEFT)
  pairsRow.getChildren.addAll(
    Label("FF"),
    ffField,
    new Region() { HBox.setHgrow(this, Priority.ALWAYS) },
    Label("FB"),
    fbField
  )

  // Unity section
  private val unityLabel = Label("UNITY")
  unityLabel.getStyleClass.addAll("label", "h-head")

  private val unity0Field = JagNumericField(0, 65535, 0)
  unity0Field.setPrefWidth(55)
  unity0Field.valueProperty.addListener((_, _, nv) =>
    currentFilter.foreach(_.unity0.set(nv.intValue))
  )

  private val unity1Field = JagNumericField(0, 65535, 0)
  unity1Field.setPrefWidth(55)
  unity1Field.valueProperty.addListener((_, _, nv) =>
    currentFilter.foreach(_.unity1.set(nv.intValue))
  )

  private val unityRow = HBox(4)
  unityRow.setAlignment(Pos.CENTER_LEFT)
  unityRow.getChildren.addAll(
    Label("S"),
    unity0Field,
    new Region() { HBox.setHgrow(this, Priority.ALWAYS) },
    Label("E"),
    unity1Field
  )

  // Poles info
  private val polesLabel = Label("POLES")
  polesLabel.getStyleClass.addAll("label", "h-head")

  private val polesInfoLabel = Label("0 active")
  polesInfoLabel.getStyleClass.add("label")
  polesInfoLabel.setStyle("-fx-text-fill: #888;")

  private val polesRow = HBox(4)
  polesRow.setAlignment(Pos.CENTER_LEFT)
  polesRow.getChildren.addAll(
    polesLabel,
    new Region() { HBox.setHgrow(this, Priority.ALWAYS) },
    polesInfoLabel
  )

  getChildren.addAll(pairsLabel, pairsRow, unityLabel, unityRow, polesRow)

  /** Bind to filter view model. */
  def bind(filter: FilterViewModel): Unit =
    currentFilter = Some(filter)
    ffField.setValue(filter.pairCount0.get)
    fbField.setValue(filter.pairCount1.get)
    unity0Field.setValue(filter.unity0.get)
    unity1Field.setValue(filter.unity1.get)
    val totalPoles = filter.pairCount0.get + filter.pairCount1.get
    polesInfoLabel.setText(s"$totalPoles active")

  /** Get help text for this inspector mode. */
  def getHelpText: String =
    "FF/FB: Feedforward/feedback pairs\nUnity: Gain range (S to E)\nPoles shown on unit circle"

object FilterInspector:
  def apply(): FilterInspector = new FilterInspector()
