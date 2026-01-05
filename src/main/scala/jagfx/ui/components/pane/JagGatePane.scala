package jagfx.ui.components.pane

import javafx.scene.layout._
import javafx.scene.control.Label
import javafx.geometry.Pos
import jagfx.ui.viewmodel.ToneViewModel

/** Grouped container for `Gate` controls (`Silence` + `Duration`). Displays 2
  * mini envelope cells in horizontal strip.
  */
class JagGatePane extends JagBasePane("GATE"):
  getStyleClass.add("gate-pane")
  setSpacing(2)

  private val _row = HBox(2)
  _row.setAlignment(Pos.CENTER)
  VBox.setVgrow(_row, Priority.ALWAYS)

  private val _silenceCell = JagCellPane("SILENCE")
  private val _durationCell = JagCellPane("DURATION")

  // Compact mode for mini cells
  _silenceCell.setFeatures(false, false)
  _durationCell.setFeatures(false, false)

  HBox.setHgrow(_silenceCell, Priority.ALWAYS)
  HBox.setHgrow(_durationCell, Priority.ALWAYS)

  _row.getChildren.addAll(_silenceCell, _durationCell)

  getChildren.add(_row)

  def bind(tone: ToneViewModel): Unit =
    _silenceCell.setViewModel(tone.gateSilence)
    _durationCell.setViewModel(tone.gateDuration)

  def getCells: Seq[JagCellPane] =
    Seq(_silenceCell, _durationCell)

object JagGatePane:
  def apply(): JagGatePane = new JagGatePane()
