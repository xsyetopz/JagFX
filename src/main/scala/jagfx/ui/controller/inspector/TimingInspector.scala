package jagfx.ui.controller.inspector

import javafx.scene.layout._
import javafx.scene.control.Label
import javafx.geometry.Pos
import jagfx.ui.viewmodel.ToneViewModel
import jagfx.ui.components.field._
import jagfx.Constants.Int16
import javafx.scene.layout.Region

private val TimingFieldSize = 55

/** Inspector panel for tone timing parameters (`Duration`, `StartOffset`). */
class TimingInspector extends VBox:
  private var currentTone: Option[ToneViewModel] = None

  setSpacing(8)
  getStyleClass.add("timing-inspector")

  private val timingLabel = Label("TIMING")
  timingLabel.getStyleClass.addAll("label", "h-head")

  private val durField = JagNumericField(0, Int16.Range, 1000)
  durField.setPrefWidth(TimingFieldSize)
  durField.valueProperty.addListener((_, _, nv) =>
    currentTone.foreach(_.duration.set(nv.intValue))
  )

  private val ofsField = JagNumericField(0, Int16.Range, 0)
  ofsField.setPrefWidth(TimingFieldSize)
  ofsField.valueProperty.addListener((_, _, nv) =>
    currentTone.foreach(_.startOffset.set(nv.intValue))
  )

  private val timingRow = HBox(4)
  timingRow.setAlignment(Pos.CENTER_LEFT)
  private val lblDur = Label("DUR:")
  lblDur.setMinWidth(Region.USE_PREF_SIZE)

  private val lblOfs = Label("OFS:")
  lblOfs.setMinWidth(Region.USE_PREF_SIZE)

  timingRow.getChildren.addAll(
    lblDur,
    durField,
    new Region() { HBox.setHgrow(this, Priority.ALWAYS) },
    lblOfs,
    ofsField
  )

  getChildren.addAll(timingLabel, timingRow)

  /** Bind to tone view model. */
  def bind(tone: ToneViewModel): Unit =
    currentTone = Some(tone)
    durField.setValue(tone.duration.get)
    ofsField.setValue(tone.startOffset.get)
