package jagfx.ui.controller.rack

import javafx.scene.layout._
import javafx.scene.control.Label
import jagfx.ui.components.canvas.JagEnvelopeEditorCanvas
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.ui.viewmodel.EnvelopeViewModel

class RackEditor(viewModel: SynthViewModel):
  private var _editorModeCell: Option[Int] = None

  val canvas = JagEnvelopeEditorCanvas()

  val header = new HBox():
    getStyleClass.add("editor-header")
    setAlignment(javafx.geometry.Pos.CENTER_LEFT)
    setSpacing(8)
    setOnMouseClicked(e => if e.getClickCount == 2 then exitEditorMode())

  val title = new Label("")
  title.getStyleClass.add("editor-title")
  title.setOnMouseClicked(e => if e.getClickCount == 2 then exitEditorMode())
  header.getChildren.add(title)

  val content = new VBox():
    getStyleClass.add("editor-content")
  content.getChildren.addAll(header, canvas)
  VBox.setVgrow(canvas, Priority.ALWAYS)

  val overlay = new StackPane():
    getStyleClass.add("editor-overlay")
    setVisible(false)
    setPickOnBounds(true)
    getChildren.add(content)

  def getView: StackPane = overlay

  def exitEditorMode(): Unit =
    _editorModeCell = None
    overlay.setVisible(false)

  def toggleEditorMode(cellIdx: Int): Unit =
    _editorModeCell match
      case Some(current) if current == cellIdx => exitEditorMode()
      case _                                   =>
        val cellDef = RackDefs.cellDefs(cellIdx)
        cellDef.cellType match
          case CellType.Envelope(getter, _) =>
            val tone = viewModel.getActiveTone
            val env = getter(tone)
            canvas.setViewModel(env)
            title.setText(s"${cellDef.title} EDITOR")
            _editorModeCell = Some(cellIdx)
            overlay.setVisible(true)
            overlay.toFront()
          case _ => // ignore
