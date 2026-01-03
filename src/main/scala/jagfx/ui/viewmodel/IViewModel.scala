package jagfx.ui.viewmodel

/** Base trait for ViewModels with change listener support. */
trait IViewModel:
  private var listeners: List[() => Unit] = Nil

  /** Register callback to be notified when this `ViewModel` changes. */
  def addChangeListener(cb: () => Unit): Unit =
    listeners = cb :: listeners
    registerPropertyListeners(cb)

  /** Unregister callback to be notified when this `ViewModel` changes. */
  def removeChangeListener(cb: () => Unit): Unit =
    listeners = listeners.filter(_ != cb)

  /** Override to wire up property-specific listeners. */
  protected def registerPropertyListeners(cb: () => Unit): Unit = ()

  /** Notify all registered listeners of change. */
  protected def notifyListeners(): Unit =
    listeners.foreach(_())
