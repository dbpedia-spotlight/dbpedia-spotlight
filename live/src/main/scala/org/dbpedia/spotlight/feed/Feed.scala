package org.dbpedia.spotlight.feed

import scala.collection.mutable._
import actors.Actor

/**
 * Baseclass for all feeds, which handles subscriptions and information distribution.
 *
 * @tparam T Type of feed item (has to be a tuple)
 */
abstract class Feed[T <: Product](synchronous: Boolean)(implicit m: Manifest[T]) extends Actor {
    private var listeners = Set[FeedListener[_]]()

    def subscribe(listener: FeedListener[_]) {
         listeners += (listener)
    }

    def unSubscribe(listener: FeedListener[_]) {
        listeners.remove(listener)
    }

    protected def notifyListeners(item: T) {
        if (synchronous)
            listeners.foreach(listener => {
                var thread: Thread = null
                listener.synchronized {
                    thread = new Thread(new Runnable {
                        def run() {
                            listener.synchronized {
                                listener.notify(item, m)
                            }
                        }
                    })
                }
                thread.start()
            })
        else
            listeners.foreach {
                _ !(item, m)
            }
    }

    FeedRegistry.register(this, m)
}
