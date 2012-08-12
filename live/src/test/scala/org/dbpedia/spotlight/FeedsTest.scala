package org.dbpedia.spotlight

import db.model.HashMapTopicalPriorStore
import feed.trec.TrecTopicTextFromAnnotationsFeed
import feed.{FeedListener, Feed}
import model._
import org.junit.Test
import java.io.File
import collection.mutable._


class FeedsTest {

    @Test
    def test {
        val testItem: (Topic, Int, Set[DBpediaResource], Set[DBpediaResource]) = (new Topic("1"), 2, Set(new DBpediaResource("3")), Set(new DBpediaResource("4")))
        val feed = new Feed[(Topic, Int, Set[DBpediaResource], Set[DBpediaResource])](false) {
            def act {
                notifyListeners(testItem)
            }
        }
        var ctr = 0
        val listener1 = new FeedListener[(Topic, Int)]() {
            def update(item: (Topic, Int)) {
                assert(item._1.equals(testItem._1) && item._2.equals(testItem._2))
                ctr += 1
            }
        }
        listener1.subscribeToAllFeeds

        val listener2 = new FeedListener[(Topic, Set[DBpediaResource], Set[Double])]() {
            def update(item: (Topic, Set[DBpediaResource], Set[Double])) {
                assert(false)
                ctr += 1
            }
        }
        listener2.subscribeToAllFeeds

        val listener3 = new FeedListener[(Topic, Set[DBpediaResource], Set[DBpediaResource])]() {
            def update(item: (Topic, Set[DBpediaResource], Set[DBpediaResource])) {
                assert(item._1.equals(testItem._1) && item._2.equals(testItem._3) && item._3.equals(testItem._4))
                ctr += 1
            }
        }
        listener3.subscribeToAllFeeds

        feed.start()
        Thread.sleep(2000)
        assert(ctr == 2)
    }

    @Test
    def testTrecTopicFeed {
        val countsFolder = "/media/Data/Wikipedia/counts" //getClass.getClassLoader.getResource("counts").getPath
        HashMapTopicalPriorStore.fromDir(new File(countsFolder))

        val testFeed = new Feed[(Set[DBpediaResource], Text, Map[DBpediaResource, Double])](true) {
            def act {
                notifyListeners(Set(new DBpediaResource("MP3")), new Text("Mathematics"),
                    Map(new DBpediaResource("MP3") -> 1.0, new DBpediaResource("Track") -> 1.0))
            }
        }

        val listener = new FeedListener[(Map[Topic, Double], Text)]() {
            def update(item: (Map[Topic, Double], Text)) {
                item._1.foreach {
                    case (topic, prob) => assert(prob <= 1)
                }
            }
        }

        val annotationFeed = new TrecTopicTextFromAnnotationsFeed(HashMapTopicalPriorStore, testFeed)
        listener.subscribeToAllFeeds
        annotationFeed.start()
        testFeed.start()
        Thread.sleep(1000)
    }

}
