package org.dbpedia.spotlight.evaluation

import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguator
import org.dbpedia.spotlight.exceptions.{ItemNotFoundException, SearchException, SurfaceFormNotFoundException, InputException}
import org.dbpedia.spotlight.db.model.{ResourceStore, CandidateMapStore, SurfaceFormStore}
import scala.Predef._


/**
 * Common sense baseline for evaluation.
 *
 * @author Joachim Daiber
 */

class DBBaselineDisambiguator(
  surfaceFormStore: SurfaceFormStore,
  resourceStore: ResourceStore,
  candidateMapStore: CandidateMapStore
) extends ParagraphDisambiguator {


  @throws(classOf[SearchException])
  @throws(classOf[ItemNotFoundException])
  @throws(classOf[InputException])
  def bestK(paragraph: Paragraph, k: Int): Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]] = {
    paragraph.occurrences.map((occurrence: SurfaceFormOccurrence) => {
      try {
        val sf = surfaceFormStore.getSurfaceForm(occurrence.surfaceForm.name)
        (occurrence, candidateMapStore.getCandidates(sf).toList.sortBy(_.support).reverse.map(_.resource).map(r => new DBpediaResourceOccurrence(r, sf, paragraph.text, occurrence.textOffset)))
      } catch {
        case e: SurfaceFormNotFoundException => (occurrence, List[DBpediaResourceOccurrence]())
      }
    }).toMap
  }

  @throws(classOf[InputException])
  def disambiguate(paragraph: Paragraph): List[DBpediaResourceOccurrence] = {
    bestK(paragraph, 0).filter(kv =>
      kv._2.nonEmpty)
      .map( kv =>
      kv._2.head)
      .toList
      .sortBy(_.textOffset)

  }

  def name = "Common sense baseline disambiguator."

}
