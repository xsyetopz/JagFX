package jagfx.ui.components.pane

import jagfx.ui.viewmodel.VoiceViewModel
import javafx.geometry.Pos
import javafx.scene.layout.*

/** Grouped container for `Gate` controls (`Silence` + `Duration`). Displays `2`
  * mini envelope cells in horizontal strip.
  */
class JagGatePane extends JagBasePane("GATE"):
  // Fields
  private val row = HBox(2)
  private val silenceCell = JagCellPane("SILENCE")
  private val durationCell = JagCellPane("DURATION")

  // Init: styling
  getStyleClass.add("gate-pane")
  setSpacing(2)

  row.setAlignment(Pos.CENTER)
  VBox.setVgrow(row, Priority.ALWAYS)

  silenceCell.setFeatures(false)
  durationCell.setFeatures(false)

  HBox.setHgrow(silenceCell, Priority.ALWAYS)
  HBox.setHgrow(durationCell, Priority.ALWAYS)

  // Init: build hierarchy
  row.getChildren.addAll(silenceCell, durationCell)
  getChildren.add(row)

  /** Binds gate envelopes from voice ViewModel. */
  def bind(voice: VoiceViewModel): Unit =
    silenceCell.setViewModel(voice.gateSilence)
    durationCell.setViewModel(voice.gateDuration)

  /** Returns child cells. */
  def getCells: Seq[JagCellPane] =
    Seq(silenceCell, durationCell)

object JagGatePane:
  /** Creates new gate pane. */
  def apply(): JagGatePane = new JagGatePane()
