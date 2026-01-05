package jagfx.ui.controller.inspector

import javafx.scene.Node
import javafx.scene.control._
import javafx.scene.layout._
import javafx.geometry.Pos
import jagfx.ui.viewmodel._
import jagfx.ui.controller.IController
import jagfx.ui.components.button.JagButton

/** Inspector panel for editing envelope or filter parameters. */
class InspectorController(viewModel: SynthViewModel)
    extends IController[ScrollPane]:
  protected val view = ScrollPane()
  view.getStyleClass.add("inspector-scroll")
  view.setFitToWidth(true)
  view.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER)

  private val _content = VBox()
  _content.getStyleClass.add("inspector-content")
  _content.setSpacing(8)
  view.setContent(_content)

  private val _envInspector = EnvelopeInspector()
  private val _fltInspector = FilterInspector()
  private val _timingInspector = TimingInspector()

  // Sections
  private val _topSection = VBox(8)
  _topSection.setAlignment(Pos.TOP_LEFT)

  private val _midSection = VBox(8)
  _midSection.setAlignment(Pos.TOP_LEFT)

  private val _infoSection = VBox(8)
  _infoSection.setAlignment(Pos.TOP_LEFT)

  // Separators
  private val _sep1 = new Separator()
  private val _sep2 = new Separator()

  // Info Text
  private val _infoLabel = Label()
  _infoLabel.getStyleClass.add("help-text")
  _infoLabel.setWrapText(true)
  _infoSection.getChildren.add(_infoLabel)

  // Assemble
  _topSection.getChildren.addAll(_envInspector, _fltInspector)
  _midSection.getChildren.add(_timingInspector)

  _content.getChildren.addAll(
    _topSection,
    _sep1,
    _midSection,
    _sep2,
    _infoSection
  )

  // Initial State
  _envInspector.setVisible(false); _envInspector.setManaged(false)
  _fltInspector.setVisible(false); _fltInspector.setManaged(false)
  _timingInspector.setVisible(false); _timingInspector.setManaged(false)

  view.setVisible(false)

  def bind(envelope: EnvelopeViewModel, title: String, desc: String): Unit =
    _show()

    _envInspector.setVisible(true); _envInspector.setManaged(true)
    _fltInspector.setVisible(false); _fltInspector.setManaged(false)

    _envInspector.bind(envelope)

    _timingInspector.setVisible(true); _timingInspector.setManaged(true)
    _timingInspector.bind(viewModel.getActiveTone)

    _infoLabel.setText(s"$desc")

  def bindFilter(filter: FilterViewModel, title: String, desc: String): Unit =
    _show()

    _envInspector.setVisible(false); _envInspector.setManaged(false)
    _fltInspector.setVisible(true); _fltInspector.setManaged(true)

    _fltInspector.bind(filter)

    _timingInspector.setVisible(true); _timingInspector.setManaged(true)
    _timingInspector.bind(viewModel.getActiveTone)

    _infoLabel.setText(s"$desc")

  def hide(): Unit =
    view.setVisible(false)
    _envInspector.setVisible(false)
    _envInspector.setManaged(false)
    _fltInspector.setVisible(false)
    _fltInspector.setManaged(false)
    _timingInspector.setVisible(false)
    _timingInspector.setManaged(false)

  private def _show(): Unit =
    view.setVisible(true)
