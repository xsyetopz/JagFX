package jagfx.ui.controller.inspector

import jagfx.ui.controller.ControllerLike
import jagfx.ui.viewmodel.*
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*

/** Inspector panel for editing envelope or filter parameters. */
class InspectorController(viewModel: SynthViewModel)
    extends ControllerLike[ScrollPane]:
  // Fields
  protected val view: ScrollPane = ScrollPane()
  private val content = VBox()
  private val envInspector = EnvelopeInspector()
  private val fltInspector = FilterInspector()
  private val timingInspector = TimingInspector()
  private val topSection = VBox(8)
  private val midSection = VBox(8)
  private val infoSection = VBox(8)
  private val sep1 = new Separator()
  private val sep2 = new Separator()
  private val infoLabel = Label()

  // Init: styling
  view.getStyleClass.add("inspector-scroll")
  view.setFitToWidth(true)
  view.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER)

  content.getStyleClass.add("inspector-content")
  content.setSpacing(8)

  topSection.setAlignment(Pos.TOP_LEFT)
  midSection.setAlignment(Pos.TOP_LEFT)
  infoSection.setAlignment(Pos.TOP_LEFT)

  infoLabel.getStyleClass.add("help-text")
  infoLabel.setWrapText(true)

  // Init: build hierarchy
  infoSection.getChildren.add(infoLabel)
  topSection.getChildren.addAll(envInspector, fltInspector)
  midSection.getChildren.add(timingInspector)

  content.getChildren.addAll(topSection, sep1, midSection, sep2, infoSection)
  view.setContent(content)

  // Init: initial state
  envInspector.setVisible(false); envInspector.setManaged(false)
  fltInspector.setVisible(false); fltInspector.setManaged(false)
  timingInspector.setVisible(false); timingInspector.setManaged(false)
  view.setVisible(false)

  /** Binds envelope view model to inspector. */
  def bind(envelope: EnvelopeViewModel, desc: String): Unit =
    show()
    envInspector.setVisible(true); envInspector.setManaged(true)
    fltInspector.setVisible(false); fltInspector.setManaged(false)
    envInspector.bind(envelope)
    timingInspector.setVisible(true); timingInspector.setManaged(true)
    timingInspector.bind(viewModel.getActiveVoice)
    infoLabel.setText(s"$desc")

  /** Binds filter view model to inspector. */
  def bindFilter(filter: FilterViewModel, desc: String): Unit =
    show()
    envInspector.setVisible(false); envInspector.setManaged(false)
    fltInspector.setVisible(true); fltInspector.setManaged(true)
    fltInspector.bind(filter)
    timingInspector.setVisible(true); timingInspector.setManaged(true)
    timingInspector.bind(viewModel.getActiveVoice)
    infoLabel.setText(s"$desc")

  /** Hides inspector panel. */
  def hide(): Unit =
    view.setVisible(false)
    envInspector.setVisible(false); envInspector.setManaged(false)
    fltInspector.setVisible(false); fltInspector.setManaged(false)
    timingInspector.setVisible(false); timingInspector.setManaged(false)

  private def show(): Unit =
    view.setVisible(true)
