package org.dbpedia.spotlight.feed

import actors.Actor
import org.dbpedia.spotlight.log.SpotlightLog
import scala.Product
import ClassManifest.fromClass

/**
 * FeedListeners have the ability to listen to feeds and get updated. A feed listener can subscribe to all feeds whose feed items at least
 * contain the subset of objects defined by T (which is a tuple). <br> <br>
 * E.g.: There is a feed which pushes items of type (Topic, Text) to its listeners. The following types T (for listeners) can handle
 * such a feed: (Topic), (Text), (Topic, Text), (Text, Topic). I.e. that a feedlistener just takes the stuff he needs from
 * a feed.
 *
 * @param m manifest of T
 * @tparam T Type of feed item that will be consumed
 */
abstract class FeedListener[T <: Product](implicit m: Manifest[T]) extends Actor {
    protected def update(item: T)

    def subscribeTo(feed: Feed[_]) {
        feed.subscribe(this)
    }

    def unSubscribeTo(feed: Feed[_]) {
        feed.unSubscribe(this)
    }

    //TODO possible mailbox overflow??
    def act {
        loop {
            receive {
                case item: (Product, Manifest[_]) => notify(item._1, item._2)
                case _ => SpotlightLog.error(this.getClass, "Received wrong feed item!")
            }
        }
    }

    def notify(item: Product, iMan: Manifest[_]) {
        if (iMan.equals(m))
            update(item.asInstanceOf[T])
        else {
            val iManIt = iMan.typeArguments.iterator
            var possibleArgs = item.productIterator.foldLeft(List[(Object, Manifest[_])]())((list, element) => list.::((element.asInstanceOf[Object], iManIt.next()))).reverse
            //try {
            val args =
                m.typeArguments.foldLeft(List[Object]())((list, manifest) => {
                    val obj = possibleArgs.find(element =>
                        element._2.equals(manifest)
                    ).get
                    possibleArgs = possibleArgs.diff(List(obj))
                    list.::(obj._1)
                }).reverse.toArray

            if (m.typeArguments.length.equals(args.length)) {
                val constructor = m.erasure.getConstructors()(0)
                update(constructor.newInstance(args: _*).asInstanceOf[T])
            }
            //  }
        }
    }

    def subscribeToAllFeeds {
        FeedRegistry.getFeedsByManifest(m).foreach(
            _.subscribe(this)
        )
    }

    def unSubscribeToAllFeeds {
        FeedRegistry.getFeedsByManifest(m).foreach(
            _.unSubscribe(this)
        )
    }

    this.start()
}
