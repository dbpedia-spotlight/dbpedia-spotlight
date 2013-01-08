package org.dbpedia.spotlight.io

import org.dbpedia.spotlight.model.{Text, DBpediaResourceOccurrence, AnnotatedParagraph}

/**
 * AnnotatedTextSource for heldout data generated from Wikipedia using the
 * Pig-based import scripts.
 *
 * @author Joachim Daiber
 * @param lines Iterator of lines containing single MediaWiki paragraphs that
 *              were extracted as heldout data from the MediaWiki dump.
 *
 */
class WikipediaHeldoutCorpus(val lines: Iterator[String]) extends AnnotatedTextSource {
  override def foreach[U](f : AnnotatedParagraph => U) {
    WikiOccurrenceSource.fromPigHeldoutFile(lines).groupBy(_.context).foreach {
      m: (Text, Traversable[DBpediaResourceOccurrence]) =>
        f(new AnnotatedParagraph(m._1, m._2.toList))
    }
  }
}