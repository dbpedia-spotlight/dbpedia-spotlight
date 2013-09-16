package org.dbpedia.spotlight.feed

/**
 * Baseclass for all feeds that decorate (enrich) other feeds with new information
 * @param feed
 * @param synchronous Should be true if the consumers are slower than this feed as a producer
 * @param mA
 * @param mB
 * @tparam A Type of feed to decorate
 * @tparam B Type of this feed
 */
abstract class DecoratorFeed[A <: Product, B <: Product](val feed: Feed[A], synchronous: Boolean)(implicit mA: Manifest[A], mB: Manifest[B]) extends Feed[B](synchronous) {

    val feedListener = new FeedListener[A]() {
        def update(item: A) {
            processFeedItem(item)
        }
    }

    def act {
        feed.subscribe(feedListener)
    }

    def processFeedItem(item: A)
}
