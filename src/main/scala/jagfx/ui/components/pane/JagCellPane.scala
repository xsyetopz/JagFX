package jagfx.ui.components.pane

import javafx.beans.property._
import javafx.scene.layout._
import javafx.scene.control.Label
import javafx.scene.control._
import javafx.geometry.Pos
import jagfx.ui.viewmodel.EnvelopeViewModel
import jagfx.utils.IconUtils
import jagfx.model.WaveForm
import javafx.beans.value.ChangeListener
import jagfx.ui.components.canvas._
import jagfx.ui.components.button.JagButton

class JagCellPane(title: String) extends StackPane:
  private val selected = SimpleBooleanProperty(false)

  def selectedProperty: BooleanProperty = selected

  getStyleClass.add("jag-cell")
  setMinWidth(0)
  setMinHeight(0)

  private val container = VBox()
  container.getStyleClass.add("cell-container")
  container.setPickOnBounds(true) // FORCE capture of clicks on empty space

  // IF container (or trans child) clicked, THEN fire on 'StackPane`
  container.setOnMouseClicked(e =>
    this.fireEvent(e.copyFor(this, this))
    e.consume()
  )

  private val header = HBox()
  header.getStyleClass.add("cell-head")
  header.setSpacing(4)
  header.setPickOnBounds(false)

  private val titleLabel = Label(title)
  titleLabel.getStyleClass.add("cell-title")
  titleLabel.setMaxWidth(Double.MaxValue)
  titleLabel.setAlignment(Pos.CENTER_LEFT)
  HBox.setHgrow(titleLabel, Priority.ALWAYS)

  private val toolbar = HBox()
  toolbar.setSpacing(1)

  private val btnX1 = createToolButton("X1")
  private val btnX2 = createToolButton("X2")
  private val btnX4 = createToolButton("X4")

  private val btnMenu = createToolButton()
  btnMenu.setGraphic(IconUtils.icon("mdi2d-dots-horizontal"))

  private val zooms = Seq((btnX1, 1), (btnX2, 2), (btnX4, 4))
  private var alternateCanvas: Option[JagBaseCanvas] = None

  /** Set alternate canvas that zoom buttons will control instead of built-in
    * envelope canvas.
    */
  def setAlternateCanvas(alt: JagBaseCanvas): Unit =
    alternateCanvas = Some(alt)
    btnX1.fire() // force [X1] zoom

  zooms.foreach { case (btn, level) =>
    btn.setOnAction(_ =>
      zooms.foreach(_._1.setActive(false))
      btn.setActive(true)
      alternateCanvas.getOrElse(canvas).setZoom(level)
    )
  }
  btnX1.setActive(true)

  private val contextMenu = new ContextMenu()

  def updateMenu(): Unit =
    contextMenu.getItems.clear()
    val iX1 = new MenuItem("x1"); iX1.setOnAction(_ => btnX1.fire())
    val iX2 = new MenuItem("x2"); iX2.setOnAction(_ => btnX2.fire())
    val iX4 = new MenuItem("x4"); iX4.setOnAction(_ => btnX4.fire())
    contextMenu.getItems.addAll(iX1, iX2, iX4)

  btnMenu.setOnAction(e =>
    updateMenu()
    contextMenu.show(btnMenu, javafx.geometry.Side.BOTTOM, 0, 0)
  )

  private var showCollapse = true

  def setFeatures(showMute: Boolean, showCollapse: Boolean): Unit =
    this.showCollapse = showCollapse
    updateToolbar()

  private def updateToolbar(): Unit =
    toolbar.getChildren.clear()
    val w = getWidth

    val titleWidth = titleLabel.prefWidth(-1)
    var toolsCount = 3 // X1, X2, X4
    if showCollapse then toolsCount += 1

    val toolsWidth = toolsCount * 25
    val padding = 5

    val isNarrow = w > 0 && w < (titleWidth + toolsWidth + padding)
    if isNarrow then toolbar.getChildren.add(btnMenu)
    else toolbar.getChildren.addAll(btnX1, btnX2, btnX4)

  widthProperty.addListener((_, _, _) => updateToolbar())
  updateToolbar()
  header.getChildren.addAll(titleLabel, toolbar)

  private val canvasWrapper = new Pane()
  canvasWrapper.setMouseTransparent(true) // make sure clicks pass thru
  VBox.setVgrow(canvasWrapper, Priority.ALWAYS)

  private val canvas = JagEnvelopeCanvas()
  canvas.setPickOnBounds(false)
  canvas.widthProperty.bind(canvasWrapper.widthProperty)
  canvas.heightProperty.bind(canvasWrapper.heightProperty)
  canvasWrapper.getChildren.add(canvas)

  container.getChildren.addAll(header, canvasWrapper)
  getChildren.add(container)

  selected.addListener((_, _, isSelected) =>
    if isSelected then getStyleClass.add("selected")
    else getStyleClass.remove("selected")
  )

  private var currentVm: Option[EnvelopeViewModel] = None

  private val dimmingListener: () => Unit = () =>
    currentVm.foreach(vm => updateDimming(vm))

  private def updateDimming(vm: EnvelopeViewModel): Unit =
    container.setOpacity(if vm.isZero then 0.5 else 1.0)

  def setViewModel(vm: EnvelopeViewModel): Unit =
    currentVm.foreach(_.removeChangeListener(dimmingListener))
    currentVm = Some(vm)

    vm.addChangeListener(dimmingListener)
    updateDimming(vm)

    canvas.setViewModel(vm)

  def getCanvas: JagEnvelopeCanvas = canvas

  private def createToolButton(text: String = ""): JagButton =
    val b = JagButton(text)
    b.getStyleClass.add("t-btn")
    b
