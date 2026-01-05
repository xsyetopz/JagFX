package jagfx.ui.controller.inspector

import javafx.scene.layout._
import javafx.scene.control.Label
import javafx.geometry.Pos
import jagfx.ui.viewmodel.FilterViewModel
import jagfx.ui.components.field._
import jagfx.ui.components.group._
import jagfx.ui.components.slider._
import jagfx.Constants.Int16

/** Inspector panel for filter parameters. */
class FilterInspector extends VBox:
  private var _currentFilter: Option[FilterViewModel] = None

  setSpacing(8)

  // Pairs section
  private val _pairsLabel = Label("PAIRS")
  _pairsLabel.getStyleClass.addAll("label", "h-head")

  private val _ffField = JagNumericField(0, 4, 0)
  _ffField.setPrefWidth(28)
  _ffField.valueProperty.addListener((_, _, nv) =>
    _currentFilter.foreach(_.pairCount0.set(nv.intValue))
  )

  private val _fbField = JagNumericField(0, 4, 0)
  _fbField.setPrefWidth(28)
  _fbField.valueProperty.addListener((_, _, nv) =>
    _currentFilter.foreach(_.pairCount1.set(nv.intValue))
  )

  private val _pairsRow = HBox(4)
  _pairsRow.setAlignment(Pos.CENTER_LEFT)
  _pairsRow.getChildren.addAll(
    Label("FF"),
    _ffField,
    new Region() { HBox.setHgrow(this, Priority.ALWAYS) },
    Label("FB"),
    _fbField
  )

  // Unity section
  private val _unityLabel = Label("UNITY")
  _unityLabel.getStyleClass.addAll("label", "h-head")

  private val _unity0Field = JagNumericField(0, Int16.Range, 0)
  _unity0Field.setPrefWidth(55)
  _unity0Field.valueProperty.addListener((_, _, nv) =>
    _currentFilter.foreach(_.unity0.set(nv.intValue))
  )

  private val _unity1Field = JagNumericField(0, Int16.Range, 0)
  _unity1Field.setPrefWidth(55)
  _unity1Field.valueProperty.addListener((_, _, nv) =>
    _currentFilter.foreach(_.unity1.set(nv.intValue))
  )

  private val _unityRow = HBox(4)
  _unityRow.setAlignment(Pos.CENTER_LEFT)
  _unityRow.getChildren.addAll(
    Label("S:"),
    _unity0Field,
    new Region() { HBox.setHgrow(this, Priority.ALWAYS) },
    Label("E:"),
    _unity1Field
  )

  // Poles info
  private val _polesLabel = Label("POLES")
  _polesLabel.getStyleClass.addAll("label", "h-head")

  private val _polesEditor = FilterPolesEditor()

  getChildren.addAll(
    _pairsLabel,
    _pairsRow,
    _unityLabel,
    _unityRow,
    _polesLabel,
    _polesEditor
  )

  /** Bind to filter view model. */
  def bind(filter: FilterViewModel): Unit =
    _currentFilter = Some(filter)
    _ffField.setValue(filter.pairCount0.get)
    _fbField.setValue(filter.pairCount1.get)
    _unity0Field.setValue(filter.unity0.get)
    _unity1Field.setValue(filter.unity1.get)
    _polesEditor.bind(filter)
