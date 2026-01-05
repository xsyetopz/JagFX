package jagfx.ui.components.pane

import jagfx.ui.components.button.JagButton
import jagfx.ui.components.canvas.*
import jagfx.ui.viewmodel.EnvelopeViewModel
import jagfx.utils.IconUtils
import javafx.beans.property.*
import javafx.geometry.*
import javafx.scene.control.*
import javafx.scene.layout.*

/** Container pane for rack cells with header, zoom controls, and canvas. */
class JagCellPane(title: String) extends StackPane:
  // Fields
  private val selected = SimpleBooleanProperty(false)
  private val container = VBox()
  private val header = HBox()
  private val titleLabel = Label(title)
  private val toolbar = HBox()
  private val btnX1 = createToolButton("X1")
  private val btnX2 = createToolButton("X2")
  private val btnX4 = createToolButton("X4")
  private val btnMenu = createToolButton()
  private val zooms = Seq((btnX1, 1), (btnX2, 2), (btnX4, 4))
  private val contextMenu = new ContextMenu()
  private val canvasWrapper = new Pane()
  private val canvas = JagEnvelopeCanvas()
  private val dimmingListener: () => Unit = () =>
    currentVm.foreach(updateDimming)

  private var onMaximizeToggle: Option[() => Unit] = None
  private var alternateCanvas: Option[JagBaseCanvas] = None
  private var showCollapse = true
  private var showZoomButtons = true
  private var currentVm: Option[EnvelopeViewModel] = None

  // Init: styling
  getStyleClass.add("jag-cell")
  setMinWidth(0)
  setMinHeight(0)

  container.getStyleClass.add("cell-container")
  container.setPickOnBounds(true)

  header.getStyleClass.add("cell-head")
  header.setSpacing(4)
  header.setPickOnBounds(false)

  titleLabel.getStyleClass.add("cell-title")
  titleLabel.setMaxWidth(Double.MaxValue)
  titleLabel.setAlignment(Pos.CENTER_LEFT)
  HBox.setHgrow(titleLabel, Priority.ALWAYS)

  toolbar.setSpacing(1)
  btnMenu.setGraphic(IconUtils.icon("mdi2d-dots-horizontal"))
  btnX1.setActive(true)

  canvasWrapper.setMouseTransparent(true)
  VBox.setVgrow(canvasWrapper, Priority.ALWAYS)
  canvas.setPickOnBounds(false)
  canvas.widthProperty.bind(canvasWrapper.widthProperty)
  canvas.heightProperty.bind(canvasWrapper.heightProperty)

  // Init: event handlers
  container.setOnMouseClicked(e =>
    if e.getClickCount == 2 then
      onMaximizeToggle.foreach(_())
      e.consume()
    else
      this.fireEvent(e.copyFor(this, this))
      e.consume()
  )

  titleLabel.setOnMouseClicked(e => if e.getClickCount == 2 then e.consume())

  zooms.foreach { case (btn, level) =>
    btn.setOnAction(_ =>
      zooms.foreach(_._1.setActive(false))
      btn.setActive(true)
      alternateCanvas.getOrElse(canvas).setZoom(level)
      canvasWrapper.setMouseTransparent(level == 1)
    )
  }

  btnMenu.setOnAction(_ =>
    updateMenu()
    contextMenu.show(btnMenu, Side.BOTTOM, 0, 0)
  )

  selected.addListener((_, _, isSelected) =>
    if isSelected then getStyleClass.add("selected")
    else getStyleClass.remove("selected")
  )

  widthProperty.addListener((_, _, _) => updateToolbar())

  // Init: build hierarchy
  updateToolbar()
  header.getChildren.addAll(titleLabel, toolbar)
  canvasWrapper.getChildren.add(canvas)
  container.getChildren.addAll(header, canvasWrapper)
  getChildren.add(container)

  /** Selection state property. */
  def selectedProperty: BooleanProperty = selected

  /** Set callback for maximize toggle via double-click. */
  def setOnMaximizeToggle(handler: () => Unit): Unit =
    onMaximizeToggle = Some(handler)

  /** Set alternate canvas for zoom control. */
  def setAlternateCanvas(alt: JagBaseCanvas): Unit =
    alternateCanvas = Some(alt)
    btnX1.fire()

  /** Updates context menu items. */
  def updateMenu(): Unit =
    contextMenu.getItems.clear()
    val iX1 = new MenuItem("x1"); iX1.setOnAction(_ => btnX1.fire())
    val iX2 = new MenuItem("x2"); iX2.setOnAction(_ => btnX2.fire())
    val iX4 = new MenuItem("x4"); iX4.setOnAction(_ => btnX4.fire())
    contextMenu.getItems.addAll(iX1, iX2, iX4)

  /** Configures whether collapse button is shown. */
  def setFeatures(showCollapse: Boolean): Unit =
    this.showCollapse = showCollapse
    updateToolbar()

  /** Configures whether zoom buttons are shown. */
  def setShowZoomButtons(show: Boolean): Unit =
    showZoomButtons = show
    updateToolbar()

  /** Binds envelope ViewModel to canvas and enables dimming. */
  def setViewModel(vm: EnvelopeViewModel): Unit =
    currentVm.foreach(_.removeChangeListener(dimmingListener))
    currentVm = Some(vm)
    vm.addChangeListener(dimmingListener)
    updateDimming(vm)
    canvas.setViewModel(vm)

  /** Returns envelope canvas. */
  def getCanvas: JagEnvelopeCanvas = canvas

  private def createToolButton(text: String = ""): JagButton =
    val b = JagButton(text)
    b.getStyleClass.add("t-btn")
    b

  private def updateToolbar(): Unit =
    toolbar.getChildren.clear()
    if !showZoomButtons then return

    val width = getWidth
    val titleWidth = titleLabel.prefWidth(-1)
    var toolsCount = 3
    if showCollapse then toolsCount += 1

    val toolsWidth = toolsCount * 25
    val padding = 5

    val isNarrow = width > 0 && width < (titleWidth + toolsWidth + padding)
    if isNarrow then toolbar.getChildren.add(btnMenu)
    else toolbar.getChildren.addAll(btnX1, btnX2, btnX4)

  private def updateDimming(vm: EnvelopeViewModel): Unit =
    container.setOpacity(if vm.isZero then 0.5 else 1.0)
