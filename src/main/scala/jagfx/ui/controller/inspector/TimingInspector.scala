package jagfx.ui.controller.inspector

import javafx.scene.layout._
import javafx.scene.control.Label
import javafx.geometry.Pos
import jagfx.ui.viewmodel.ToneViewModel
import jagfx.ui.components.field._
import jagfx.Constants.Int16
import javafx.scene.layout.Region

private val _TimingFieldSize = 55

/** Inspector panel for tone timing parameters (`Duration`, `StartOffset`). */
class TimingInspector extends VBox:
  private var _currentTone: Option[ToneViewModel] = None

  setSpacing(8)
  getStyleClass.add("timing-inspector")

  private val _timingLabel = Label("TIMING")
  _timingLabel.getStyleClass.addAll("label", "h-head")

  private val _durField = JagNumericField(0, Int16.Range, 1000)
  _durField.setPrefWidth(_TimingFieldSize)
  _durField.valueProperty.addListener((_, _, nv) =>
    _currentTone.foreach(_.duration.set(nv.intValue))
  )

  private val _ofsField = JagNumericField(0, Int16.Range, 0)
  _ofsField.setPrefWidth(_TimingFieldSize)
  _ofsField.valueProperty.addListener((_, _, nv) =>
    _currentTone.foreach(_.startOffset.set(nv.intValue))
  )

  private val _timingRow = HBox(4)
  _timingRow.setAlignment(Pos.CENTER_LEFT)
  private val _lblDur = Label("DUR:")
  _lblDur.setMinWidth(Region.USE_PREF_SIZE)

  private val _lblOfs = Label("OFS:")
  _lblOfs.setMinWidth(Region.USE_PREF_SIZE)

  _timingRow.getChildren.addAll(
    _lblDur,
    _durField,
    new Region() { HBox.setHgrow(this, Priority.ALWAYS) },
    _lblOfs,
    _ofsField
  )

  getChildren.addAll(_timingLabel, _timingRow)

  /** Bind to tone view model. */
  def bind(tone: ToneViewModel): Unit =
    _currentTone = Some(tone)
    _durField.setValue(tone.duration.get)
    _ofsField.setValue(tone.startOffset.get)
