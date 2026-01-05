package jagfx.ui.viewmodel

import scala.collection.mutable.ArrayBuffer

/** Base trait for `ViewModel`s with change listener support. */
trait IViewModel:
  private val _listeners = ArrayBuffer[() => Unit]()

  /** Register callback to be notified when this `ViewModel` changes. */
  def addChangeListener(cb: () => Unit): Unit =
    _listeners += cb
    registerPropertyListeners(cb)

  /** Unregister callback to be notified when this `ViewModel` changes. */
  def removeChangeListener(cb: () => Unit): Unit =
    _listeners -= cb

  /** Override to wire up property-specific listeners. */
  protected def registerPropertyListeners(cb: () => Unit): Unit = ()

  /** Notify all registered listeners of change. */
  protected def notifyListeners(): Unit =
    var i = 0
    while i < _listeners.length do
      _listeners(i)()
      i += 1
