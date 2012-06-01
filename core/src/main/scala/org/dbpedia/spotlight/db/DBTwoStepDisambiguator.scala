package org.dbpedia.spotlight.db

import model._
import com.officedepot.cdap2.collection.CompactHashSet
import org.dbpedia.spotlight.model._

/**
 * @author Joachim Daiber
 */

class DBTwoStepDisambiguator(
  tokenStore: TokenStore,
  surfaceFormStore: SurfaceFormStore,
  resourceStore: ResourceStore,
  candidateSearcher: DBCandidateSearcher,
  contextStore: ContextStore
) {

  def getContextScores(text: Text, candidates: Set[DBpediaResource]): Map[DBpediaResource, Float] = {

    val tokens = text.text.split(" ").map(tokenStore.get(_)) filter( _ != null)

    candidates.map {
      candidate =>
        contextStore.get(candidate, token)
    }


  }

  def bestK(paragraph: Paragraph, k: Int): Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]] = {

    LOG.debug("Running bestK for paragraph %s.".format(paragraph.id))

    if (paragraph.occurrences.size==0)
      return Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]()

    // step1: get candidates for all surface forms
    var allCandidates = CompactHashSet[DBpediaResource]();
    val occs = paragraph.occurrences.foldLeft(
      Map[SurfaceFormOccurrence, List[DBpediaResource]]())(
      (acc, sfOcc) => {

        LOG.debug("Searching...")
        val candidates = candidateSearcher.getCandidates(sfOcc.surfaceForm)

        LOG.debug("# candidates for: %s = %s.".format(sfOcc.surfaceForm, candidates.size))
        allCandidates ++= candidates

        acc + (sfOcc -> candidates.toList)
      })


    // step2: query once for the paragraph context, get scores for each candidate resource
    val contextScores = getContextScores(paragraph.text, allCandidates)

    // pick the best k for each surface form
    val ranked = occs.keys.foldLeft(Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]())( (acc,aSfOcc) => {
      val candOccs = occs.getOrElse(aSfOcc, List[DBpediaResource]())
        .map( resource =>
        Factory.DBpediaResourceOccurrence.from(
          aSfOcc,
          resource,
          scores.getOrElse(resource.uri,(0,0.0)))
        )
        .sortBy(o => o.contextualScore) //TODO should be final score
        .reverse
        .take(k)
      acc + (aSfOcc -> candOccs)
    });

    ranked
  }

}
