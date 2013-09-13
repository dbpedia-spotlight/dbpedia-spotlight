package org.dbpedia.spotlight.feed

import java.net.URL
import scala._
import actors.TIMEOUT
import java.util
import org.dbpedia.spotlight.model.{Text,RssItem, Topic}
import util.Date
import xml.{Node,XML}
import org.dbpedia.spotlight.log.SpotlightLog
import org.apache.commons.lang.time.DateUtils

/**
 * Implementation of a topical RSS feed
 * @param topic
 * @param feedUrls
 * @param interval Time interval to wait between fetching new updates
 */
class RssTopicFeed(val topic: Topic, val feedUrls: Array[URL], protected var interval: Long) extends Feed[(Topic, RssItem)](false) {

    protected var lastUpdate = new Date(Long.MinValue)

    def changeInterval(refreshInterval: Long) {
        interval = refreshInterval
    }

    def act() {
        loop {
            receiveWithin(interval) {
                case TIMEOUT => {
                    var ctr = 0
                    getRssFeeds(feedUrls).foreach {
                        case (channel, items) =>
                            items.filter(item => item.date.compareTo(lastUpdate) > 0).foreach(item => {
                                notifyListeners((topic, item))
                                ctr += 1
                            })
                    }
                    lastUpdate = new util.Date(System.currentTimeMillis())
                    SpotlightLog.info(this.getClass, "Received: %d items, Topic: %s", ctr, topic.getName)
                }
            }
        }
    }

    private def getRssFeeds(feeds: Seq[URL]): List[(String, Seq[RssItem])] = {
        var baseList: List[(String, Seq[RssItem])] = List()
        feeds.map(extractRss).foldLeft(baseList) {
            (l, r) => l ::: r.toList
        }
    }

    private def extractRss(url: URL): Seq[(String, Seq[RssItem])] = {
        // Given a URL, returns a Seq of RSS data in the form:
        // ("channel", ["article 1", "article 2"])
        val conn = url.openConnection
        val xml = XML.load(conn.getInputStream)

        for (channel <- xml \\ "channel")
        yield {
            val channelTitle = (channel \ "title").text
            val items = extractRssItems(channel)
            (channelTitle, items)
        }
    }

    private def extractRssItems(channel: Node): Seq[RssItem] = {
        // Given a channel node from an RSS feed, returns all of the article names
        for (item <- (channel \\ "item")) yield
            new RssItem(
                new Text((item \\ "title").head.text),
                DateUtils.parseDate((item \\ "pubDate").head.text, Array("EEE, dd MMM yyyy HH:mm:ss zzz", "EEE, dd MMM yyyy HH:mm zzz")),
                new Text((item \\ "description").head.text.replaceAll("<!\\[CDATA\\[|\\]\\]>", "").replaceAll("<[^>]*>", "")),
                new URL((item \\ "link").head.text))
    }

    override def toString: String = "RssTopicFeed[" + feedUrls.foldLeft("")(_ + "," + _.getPath).substring(1) + "]"

}
