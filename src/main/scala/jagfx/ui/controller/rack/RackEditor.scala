package jagfx.ui.controller.rack

import jagfx.ui.components.button.JagButton
import jagfx.ui.components.canvas.JagEnvelopeEditorCanvas
import jagfx.ui.viewmodel.SynthViewModel
import jagfx.utils.IconUtils
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.*

/** Overlay editor for envelope editing in expanded mode. */
class RackEditor(viewModel: SynthViewModel):
  // Fields
  private var editorModeCell: Option[Int] = None
  private val canvas = JagEnvelopeEditorCanvas()
  private val header = HBox()
  private val title = Label("")
  private val spacer = Region()
  private val minBtn = JagButton()
  private val content = VBox()
  private val overlay = StackPane()

  // Init: styling
  header.getStyleClass.add("editor-header")
  header.setAlignment(Pos.CENTER_LEFT)
  header.setSpacing(8)

  title.getStyleClass.add("editor-title")

  HBox.setHgrow(spacer, Priority.ALWAYS)

  minBtn.setGraphic(IconUtils.icon("mdi2w-window-minimize", 20))
  minBtn.getStyleClass.add("t-btn")

  content.getStyleClass.add("editor-content")
  VBox.setVgrow(canvas, Priority.ALWAYS)

  overlay.getStyleClass.add("editor-overlay")
  overlay.setVisible(false)
  overlay.setPickOnBounds(true)

  // Init: event handlers
  header.setOnMouseClicked(e => e.consume())
  title.setOnMouseClicked(e => e.consume())
  minBtn.setOnAction(_ => exitEditorMode())

  // Init: build hierarchy
  header.getChildren.addAll(title, spacer, minBtn)
  content.getChildren.addAll(header, canvas)
  overlay.getChildren.add(content)

  /** Returns editor canvas. */
  def getCanvas: JagEnvelopeEditorCanvas = canvas

  /** Returns overlay view. */
  def getView: StackPane = overlay

  /** Exits editor mode and hides overlay. */
  def exitEditorMode(): Unit =
    editorModeCell = None
    overlay.setVisible(false)

  /** Toggles editor mode for specified cell. */
  def toggleEditorMode(cellIdx: Int): Unit =
    editorModeCell match
      case Some(current) if current == cellIdx => exitEditorMode()
      case _                                   =>
        val cellDef = RackDefs.cellDefs(cellIdx)
        cellDef.cellType match
          case CellType.Envelope(getter, _) =>
            val tone = viewModel.getActiveTone
            val env = getter(tone)
            canvas.setViewModel(env)
            title.setText(s"${cellDef.title} EDITOR")
            editorModeCell = Some(cellIdx)
            overlay.setVisible(true)
            overlay.toFront()
          case _ =>
