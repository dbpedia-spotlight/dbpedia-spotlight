package org.dbpedia.spotlight.model.trec

import org.dbpedia.spotlight.model._
import collection.mutable._
import org.dbpedia.spotlight.annotate.{ParagraphAnnotator, Annotator}
import scala.collection.JavaConversions._
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.io.WikiOccurrenceSource

/**
 * This class is a decorator for the TrecCorpusFeed which adds Annotations to the streamed texts.
 *
 * @param annotator Annotator to use for annotating streamed in texts
 * @param feed Should be a TrecCorpusFeed
 *
 * @author Dirk Weissenborn
 */
class TrecResourceAnnotationFeed(val annotator: ParagraphAnnotator, feed:Feed[(Set[DBpediaResource], Text)])
  extends FeedListener[(Set[DBpediaResource], Text)] {
  feed.subscribe(this)

  private val LOG = LogFactory.getLog(getClass)

  val textAnnotationFeed = new Feed[(Set[DBpediaResource],Text,Map[DBpediaResource,Double])](true) {
    def act {
      loop {
        receive {
          case item:(Set[DBpediaResource],Text,Map[DBpediaResource,Double]) => super.notifyListeners(item)
        }
      }
    }
  }

  val resourceAnnotationFeed = new Feed[(Set[DBpediaResource],Map[DBpediaResource,Double])](true) {
    def act {
      loop {
        receive {
          case item:(Set[DBpediaResource],Map[DBpediaResource,Double]) => super.notifyListeners(item)
        }
      }
    }
  }

  def startFeed {
    textAnnotationFeed.start()
    resourceAnnotationFeed.start()
  }


  def update(item: (Set[DBpediaResource], Text)) {
    LOG.debug("Annotating text with DBpediaResources...")
    LOG.debug("Text: "+item._2.text)
    val annotations = Map[DBpediaResource,Double]()

    val text = item._2.text
    text.split("\n").foreach(paragraph => {
      if(paragraph.length > 0) {
        val currentAnnotations = Map[DBpediaResource,Double]()

        for(occurrence <- annotator.annotate(paragraph)) {
          if (occurrence!=null) {
            if (annotations.contains(occurrence.resource))
              annotations(occurrence.resource) += 1
            else
              annotations += (occurrence.resource -> 1)
            if (currentAnnotations.contains(occurrence.resource))
              currentAnnotations(occurrence.resource) += 1
            else
              currentAnnotations += (occurrence.resource -> 1)
          }
        }
        textAnnotationFeed ! (item._1,new Text(paragraph), currentAnnotations)
      }
    } )


    LOG.debug("Resources annotated:"+annotations.foldLeft("")((string,annotation) => string+" "+annotation._1.uri))
    resourceAnnotationFeed ! (item._1,annotations)
  }

}
