package jagfx.ui

import javafx.beans.property._
import javafx.beans.value.ChangeListener
import scala.collection.mutable

/** Manages JavaFX property bindings with automatic cleanup. */
class BindingManager:
  private case class ListenerBinding[T](
      property: Property[T],
      listener: ChangeListener[T]
  )
  private case class BidirectionalBinding(
      source: Property[?],
      target: Property[?]
  )

  private val listeners = mutable.ArrayBuffer[ListenerBinding[?]]()
  private val bidirectionals = mutable.ArrayBuffer[BidirectionalBinding]()

  /** Adds change listener with auto-removal on `unbindAll()`. */
  def listen[T](property: Property[T])(handler: T => Unit): Unit =
    val listener: ChangeListener[T] = (_, _, newVal) => handler(newVal)
    property.addListener(listener)
    listeners += ListenerBinding(property, listener)

  /** Creates bidirectional binding with auto-unbind on `unbindAll()`. */
  def bindBidirectional(
      source: IntegerProperty,
      target: IntegerProperty
  ): Unit =
    source.bindBidirectional(target)
    bidirectionals += BidirectionalBinding(source, target)

  /** Removes all registered listeners and bindings. */
  def unbindAll(): Unit =
    listeners.foreach { case ListenerBinding(prop, listener) =>
      prop.removeListener(listener.asInstanceOf[ChangeListener[Any]])
    }
    listeners.clear()

    bidirectionals.foreach { case BidirectionalBinding(src, tgt) =>
      src
        .asInstanceOf[Property[Any]]
        .unbindBidirectional(tgt.asInstanceOf[Property[Any]])
    }
    bidirectionals.clear()

object BindingManager:
  def apply(): BindingManager = new BindingManager()
