package org.dbpedia.spotlight.db

import org.dbpedia.spotlight.exceptions.{SearchException, ItemNotFoundException}
import org.dbpedia.spotlight.model.{DBpediaResource, SurfaceForm, CandidateSearcher}
import org.dbpedia.spotlight.db.model.CandidateMapStore


/**
 * @author Joachim Daiber
 */

class DBCandidateSearcher(candidateMap: CandidateMapStore) extends CandidateSearcher {

  @throws(SearchException, ItemNotFoundException)
  def getCandidates(sf: SurfaceForm): Set[DBpediaResource] =
    Option(candidateMap.getCandidates(sf)) match {
      case None => throw new SearchException("SurfaceForm not found.")
      case Some(candidates) => candidates.map(_.resource)
    }

  @throws(SearchException)
  def getAmbiguity(sf: SurfaceForm): Int =
    getCandidates(sf).size

}
