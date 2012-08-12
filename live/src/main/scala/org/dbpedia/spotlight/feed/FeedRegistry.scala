package org.dbpedia.spotlight.feed

import scala.reflect.Manifest
import collection.mutable._

/**
 * Simple registry for Feeds, which helps FeedListeners to find matching feeds.
 */
object FeedRegistry {

    var feeds = Set[(Manifest[_], Feed[_ <: Product])]()

    def register(feed: Feed[_ <: Product], m: Manifest[_]) {
        feeds += (m -> feed)
    }

    def getFeeds[T <: Product](implicit m: Manifest[T]): Set[Feed[_]] = {
        var ret = Set[Feed[_]]()
        val should = listToMap[Manifest[_]](m.typeArguments)
        for (feed <-
             feeds filter {
                 case (manifest, feed) => {
                     val is = listToMap[Manifest[_]](manifest.typeArguments)
                     should.foldLeft(true)((result, element) => result && element._2 >= is.getOrElse(element._1, 0))
                 }
             }) {

            ret += (feed._2)
        }
        ret
    }

    def getFeedsByManifest(m: Manifest[_]): Set[Feed[_]] = {
        var ret = Set[Feed[_]]()
        val should = listToMap[Manifest[_]](m.typeArguments)
        for (feed <-
             feeds filter {
                 case (manifest, feed) => {
                     val is = listToMap[Manifest[_]](manifest.typeArguments)
                     should.foldLeft(true)((result, element) => result && element._2 <= is.getOrElse(element._1, 0))
                 }
             }) {

            ret += (feed._2)
        }
        ret
    }

    private def listToMap[A](seq: List[A]): Map[A, Int] = {
        var result = Map[A, Int]()

        seq.foreach(element => {
            if (!result.contains(element)) {
                result += (element -> seq.count(element2 => element.equals(element2)))
            }
        })
        result
    }
}
