package org.dbpedia.spotlight.model.trec

import org.dbpedia.spotlight.model.{DecoratorFeed, FeedListener, Feed}
import org.dbpedia.spotlight.model.{Text, DBpediaResource}
import collection.mutable._
import org.dbpedia.spotlight.annotate.{ParagraphAnnotator, Annotator}
import scala.collection.JavaConversions._
import org.apache.commons.logging.LogFactory

/**
 * This class is a decorator for the TrecCorpusFeed which adds Annotations to the streamed texts.
 *
 * @param annotator Annotator to use for annotating streamed in texts
 * @param feed Should be a TrecCorpusFeed
 *
 * @author Dirk Weissenborn
 */
class TrecResourceAnnotationFeed(val annotator: ParagraphAnnotator, feed:Feed[(Set[DBpediaResource], Text)])
  extends DecoratorFeed[(Set[DBpediaResource], Text) , (Set[DBpediaResource], Text, Map[DBpediaResource,Int])](feed, true) {

  private val LOG = LogFactory.getLog(getClass)

  def processFeedItem(item: (Set[DBpediaResource], Text)) {
    LOG.debug("Annotating text with DBpediaResources...")
    LOG.debug("Text: "+item._2.text)
    val annotations = Map[DBpediaResource,Int]()

    for(occurrence <- annotator.annotate(item._2.text)) {
      if (annotations.contains(occurrence.resource))
        annotations(occurrence.resource) += 1
      else
        annotations += (occurrence.resource -> 1)
    }
    LOG.debug("Resources annotated:"+annotations.foldLeft("")((string,annotation) => string+" "+annotation._1.uri))
    notifyListeners((item._1,item._2,annotations))
  }

}
