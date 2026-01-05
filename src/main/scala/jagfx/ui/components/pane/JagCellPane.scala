package jagfx.ui.components.pane

import javafx.beans.property._
import javafx.scene.layout._
import javafx.scene.control.Label
import javafx.scene.control._
import javafx.geometry._
import jagfx.ui.viewmodel.EnvelopeViewModel
import jagfx.utils.IconUtils
import jagfx.model.WaveForm
import javafx.beans.value.ChangeListener
import jagfx.ui.components.canvas._
import jagfx.ui.components.button.JagButton

class JagCellPane(title: String) extends StackPane:
  private val _selected = SimpleBooleanProperty(false)

  def selectedProperty: BooleanProperty = _selected

  getStyleClass.add("jag-cell")
  setMinWidth(0)
  setMinHeight(0)

  private val container = VBox()
  container.getStyleClass.add("cell-container")
  container.setPickOnBounds(true) // force capture of clicks on empty space

  // IF container (or trans child) clicked, THEN fire on 'StackPane`
  container.setOnMouseClicked(e =>
    this.fireEvent(e.copyFor(this, this))
    e.consume()
  )

  private val _header = HBox()
  _header.getStyleClass.add("cell-head")
  _header.setSpacing(4)
  _header.setPickOnBounds(false)

  private val _titleLabel = Label(title)
  _titleLabel.getStyleClass.add("cell-title")
  _titleLabel.setMaxWidth(Double.MaxValue)
  _titleLabel.setAlignment(Pos.CENTER_LEFT)
  HBox.setHgrow(_titleLabel, Priority.ALWAYS)

  private var _onMaximizeToggle: Option[() => Unit] = None

  /** Set callback for when maximize (editor mode) toggled via double-click on
    * title.
    */
  def setOnMaximizeToggle(handler: () => Unit): Unit =
    _onMaximizeToggle = Some(handler)

  _titleLabel.setOnMouseClicked(e =>
    if e.getClickCount == 2 then
      _onMaximizeToggle.foreach(_())
      e.consume()
  )

  private val _toolbar = HBox()
  _toolbar.setSpacing(1)

  private val _btnX1 = _createToolButton("X1")
  private val _btnX2 = _createToolButton("X2")
  private val _btnX4 = _createToolButton("X4")

  private val _btnMenu = _createToolButton()
  _btnMenu.setGraphic(IconUtils.icon("mdi2d-dots-horizontal"))

  private val _zooms = Seq((_btnX1, 1), (_btnX2, 2), (_btnX4, 4))
  private var _alternateCanvas: Option[JagBaseCanvas] = None

  /** Set alternate canvas that zoom buttons will control instead of built-in
    * envelope canvas.
    */
  def setAlternateCanvas(alt: JagBaseCanvas): Unit =
    _alternateCanvas = Some(alt)
    _btnX1.fire() // force [X1] zoom

  _zooms.foreach { case (btn, level) =>
    btn.setOnAction(_ =>
      _zooms.foreach(_._1.setActive(false))
      btn.setActive(true)
      _alternateCanvas.getOrElse(_canvas).setZoom(level)
      // allow drag/scroll panning via mouse when zoomed in
      _canvasWrapper.setMouseTransparent(level == 1)
    )
  }
  _btnX1.setActive(true)

  private val _contextMenu = new ContextMenu()

  def updateMenu(): Unit =
    _contextMenu.getItems.clear()
    val iX1 = new MenuItem("x1"); iX1.setOnAction(_ => _btnX1.fire())
    val iX2 = new MenuItem("x2"); iX2.setOnAction(_ => _btnX2.fire())
    val iX4 = new MenuItem("x4"); iX4.setOnAction(_ => _btnX4.fire())
    _contextMenu.getItems.addAll(iX1, iX2, iX4)

  _btnMenu.setOnAction(e =>
    updateMenu()
    _contextMenu.show(_btnMenu, Side.BOTTOM, 0, 0)
  )

  private var _showCollapse = true
  private var _showZoomButtons = true

  def setFeatures(showMute: Boolean, showCollapse: Boolean): Unit =
    this._showCollapse = showCollapse
    _updateToolbar()

  /** Hide zoom buttons. */
  def setShowZoomButtons(show: Boolean): Unit =
    _showZoomButtons = show
    _updateToolbar()

  widthProperty.addListener((_, _, _) => _updateToolbar())
  _updateToolbar()
  _header.getChildren.addAll(_titleLabel, _toolbar)

  private val _canvasWrapper = new Pane()
  _canvasWrapper.setMouseTransparent(true) // make sure clicks pass thru
  VBox.setVgrow(_canvasWrapper, Priority.ALWAYS)

  private val _canvas = JagEnvelopeCanvas()
  _canvas.setPickOnBounds(false)
  _canvas.widthProperty.bind(_canvasWrapper.widthProperty)
  _canvas.heightProperty.bind(_canvasWrapper.heightProperty)
  _canvasWrapper.getChildren.add(_canvas)
  container.getChildren.addAll(_header, _canvasWrapper)
  getChildren.add(container)

  _selected.addListener((_, _, isSelected) =>
    if isSelected then getStyleClass.add("selected")
    else getStyleClass.remove("selected")
  )

  private var _currentVm: Option[EnvelopeViewModel] = None

  private val _dimmingListener: () => Unit = () =>
    _currentVm.foreach(vm => _updateDimming(vm))

  def setViewModel(vm: EnvelopeViewModel): Unit =
    _currentVm.foreach(_.removeChangeListener(_dimmingListener))
    _currentVm = Some(vm)

    vm.addChangeListener(_dimmingListener)
    _updateDimming(vm)

    _canvas.setViewModel(vm)

  def getCanvas: JagEnvelopeCanvas = _canvas

  private def _createToolButton(text: String = ""): JagButton =
    val b = JagButton(text)
    b.getStyleClass.add("t-btn")
    b

  private def _updateToolbar(): Unit =
    _toolbar.getChildren.clear()
    if !_showZoomButtons then return

    val w = getWidth

    val titleWidth = _titleLabel.prefWidth(-1)
    var toolsCount = 3 // X1, X2, X4
    if _showCollapse then toolsCount += 1

    val toolsWidth = toolsCount * 25
    val padding = 5

    val isNarrow = w > 0 && w < (titleWidth + toolsWidth + padding)
    if isNarrow then _toolbar.getChildren.add(_btnMenu)
    else _toolbar.getChildren.addAll(_btnX1, _btnX2, _btnX4)

  private def _updateDimming(vm: EnvelopeViewModel): Unit =
    container.setOpacity(if vm.isZero then 0.5 else 1.0)
