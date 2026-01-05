package jagfx.ui.controller.inspector

import jagfx.Constants.Int16
import jagfx.ui.components.field.*
import jagfx.ui.viewmodel.ToneViewModel
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.*

private val TimingFieldSize = 55

/** Inspector panel for tone timing parameters (`Duration`, `StartOffset`). */
class TimingInspector extends VBox:
  // Fields
  private var currentTone: Option[ToneViewModel] = None
  private val timingLabel = Label("TIMING")
  private val durField = JagNumericField(0, Int16.Range, 1000)
  private val ofsField = JagNumericField(0, Int16.Range, 0)
  private val timingRow = HBox(4)
  private val lblDur = Label("DUR:")
  private val lblOfs = Label("OFS:")

  // Init: styling
  setSpacing(8)
  getStyleClass.add("timing-inspector")

  timingLabel.getStyleClass.addAll("label", "height-head")
  durField.setPrefWidth(TimingFieldSize)
  ofsField.setPrefWidth(TimingFieldSize)
  timingRow.setAlignment(Pos.CENTER_LEFT)
  lblDur.setMinWidth(Region.USE_PREF_SIZE)
  lblOfs.setMinWidth(Region.USE_PREF_SIZE)

  // Init: listeners
  durField.valueProperty.addListener((_, _, nv) =>
    currentTone.foreach(_.duration.set(nv.intValue))
  )
  ofsField.valueProperty.addListener((_, _, nv) =>
    currentTone.foreach(_.startOffset.set(nv.intValue))
  )

  // Init: build hierarchy
  timingRow.getChildren.addAll(
    lblDur,
    durField,
    new Region() { HBox.setHgrow(this, Priority.ALWAYS) },
    lblOfs,
    ofsField
  )

  getChildren.addAll(timingLabel, timingRow)

  /** Binds tone view model to inspector. */
  def bind(tone: ToneViewModel): Unit =
    currentTone = Some(tone)
    durField.setValue(tone.duration.get)
    ofsField.setValue(tone.startOffset.get)
