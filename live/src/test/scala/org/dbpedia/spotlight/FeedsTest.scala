package org.dbpedia.spotlight

import model.{FeedListener, Feed}
import org.junit.Test
import org.dbpedia.spotlight.model.{DBpediaResource, Topic}


class FeedsTest {

  @Test
  def test {
    val testItem:(Topic,Int,Set[DBpediaResource],Set[DBpediaResource]) = (new Topic("1"),2, Set(new DBpediaResource("3")),Set(new DBpediaResource("4")))
    val feed = new Feed[(Topic,Int,Set[DBpediaResource],Set[DBpediaResource])](false) {
      def act{
        notifyListeners(testItem)
      }
    }
    var ctr = 0
    val listener1 = new FeedListener[(Topic,Int)]() {
      def update(item: (Topic,Int)) {
        assert(item._1.equals(testItem._1) && item._2.equals(testItem._2))
        ctr+= 1
      }
    }
    listener1.subscribeToAllFeeds

    val listener2 = new FeedListener[(Topic,Set[DBpediaResource],Set[Double])]() {
      def update(item: (Topic,Set[DBpediaResource],Set[Double])) {
        assert(false)
        ctr+=1
      }
    }
    listener2.subscribeToAllFeeds

    val listener3 = new FeedListener[(Topic,Set[DBpediaResource],Set[DBpediaResource])]() {
      def update(item: (Topic,Set[DBpediaResource],Set[DBpediaResource])) {
        assert(item._1.equals(testItem._1) && item._2.equals(testItem._3) && item._3.equals(testItem._4))
        ctr+=1
      }
    }
    listener3.subscribeToAllFeeds

    feed.start()
    Thread.sleep(2000)
    assert(ctr==2)
  }

}
