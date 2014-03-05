package org.dbpedia.spotlight.io

import org.dbpedia.spotlight.model.{Text, DBpediaResourceOccurrence, AnnotatedParagraph}
import org.dbpedia.spotlight.db.{DBCandidateSearcher, WikipediaToDBpediaClosure}
import java.io.File
import io.Source
import org.dbpedia.spotlight.exceptions.NotADBpediaResourceException
import org.dbpedia.extraction.util.WikiUtil
import java.net.URLEncoder

/**
 * AnnotatedTextSource for heldout data generated from Wikipedia using the
 * Pig-based import scripts.
 *
 * @author Joachim Daiber
 * @param lines Iterator of lines containing single MediaWiki paragraphs that
 *              were extracted as heldout data from the MediaWiki dump.
 *
 */
class WikipediaHeldoutCorpus(val lines: Seq[String],
                             val wikiToDBpediaClosure: Option[WikipediaToDBpediaClosure],
                             val candidateSearcher: Option[DBCandidateSearcher]) extends AnnotatedTextSource {

  override def foreach[U](f : AnnotatedParagraph => U) {
    WikiOccurrenceSource.fromPigHeldoutFile(lines.iterator).groupBy(_.context).foreach {
      m: (Text, Traversable[DBpediaResourceOccurrence]) =>
        f(new AnnotatedParagraph(m._1, resolveRedirectsAndFilter(m._2)))
    }
  }

  def resolveRedirectsAndFilter(occs: Traversable[DBpediaResourceOccurrence]): List[DBpediaResourceOccurrence] =
    if(wikiToDBpediaClosure.isEmpty && candidateSearcher.isEmpty)
       occs.toList
    else
      occs.flatMap({ occ: DBpediaResourceOccurrence =>
        try {
          occ.resource.uri = WikiUtil.wikiEncode(wikiToDBpediaClosure.get.wikipediaToDBpediaURI(occ.resource.uri))

          if (candidateSearcher.get.getAmbiguity(occ.surfaceForm) > 1)
            Some(occ)
          else
            None
        } catch {
          case e: NotADBpediaResourceException => None
        }
      }).toList

}

object WikipediaHeldoutCorpus {

  def fromFile(corpus: File, wikiToDBpediaClosure: WikipediaToDBpediaClosure, candidateSearcher: DBCandidateSearcher): WikipediaHeldoutCorpus = {
    new WikipediaHeldoutCorpus(Source.fromFile(corpus).getLines().toSeq, Option(wikiToDBpediaClosure), Option(candidateSearcher))
  }

  def fromFile(corpus: File): WikipediaHeldoutCorpus = fromFile(corpus, null, null)

}