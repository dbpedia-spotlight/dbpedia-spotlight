package org.dbpedia.spotlight.db

import model._
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.disambiguate.mixtures.Mixture
import org.apache.commons.logging.LogFactory

/**
 * @author Joachim Daiber
 */

class DBTwoStepDisambiguator(
  tokenStore: TokenStore,
  surfaceFormStore: SurfaceFormStore,
  resourceStore: ResourceStore,
  candidateMap: CandidateMapStore,
  contextStore: ContextStore,
  tokenizer: Tokenizer,
  mixture: Mixture
) {

  private val LOG = LogFactory.getLog(this.getClass)

  def tf(token: Token, candidate: Candidate) = {
    contextStore.getContextCount(candidate.resource, token)
  }

  def icf(token: Token, candidate: Candidate, allCandidates: Set[Candidate]): Double = {

    val nCandidatesWithToken = allCandidates.map{ cand: Candidate =>
      contextStore.getContextCount(cand.resource, token)
    }.filter( count => count > 0 ).size

    val nCandidates = allCandidates.size

    if (nCandidatesWithToken == 0)
      0.0
    else
      math.log(nCandidates / (nCandidatesWithToken + 1.0)) //TODO Why the +1.0?
  }

  def tficf(token: Token, candidate: Candidate, allCandidates: Set[Candidate]): Double = {
    tf(token, candidate).toDouble * icf(token, candidate, allCandidates)
  }


  def getScores(text: Text, candidates: Set[Candidate]): Map[String, (Int, Double)] = {
    val tokens = tokenizer.tokenize(text).map{ ts: String => tokenStore.getToken(ts) }

    candidates.map {
      candidate =>
        (candidate.resource.uri -> (candidate.resource.support,
          tokens.map{ token: Token => tficf(token, candidate, candidates) }.sum //TODO add cosine
        ))
     }.toMap

  }


  def bestK(paragraph: Paragraph, k: Int): Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]] = {

    LOG.debug("Running bestK for paragraph %s.".format(paragraph.id))

    if (paragraph.occurrences.size == 0)
      return Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]()

    // step1: get candidates for all surface forms
    var allCandidates = Set[Candidate]();
    val occs = paragraph.occurrences.foldLeft(
      Map[SurfaceFormOccurrence, List[Candidate]]())(
      (acc, sfOcc) => {

        LOG.debug("Searching...")
        val candidates = candidateMap.getCandidates(sfOcc.surfaceForm)

        LOG.debug("# candidates for: %s = %s.".format(sfOcc.surfaceForm, candidates.size))
        allCandidates ++= candidates

        acc + (sfOcc -> candidates.toList)
      })


    // step2: query once for the paragraph context, get scores for each candidate resource
    val contextScores = getScores(paragraph.text, allCandidates)

    // pick the best k for each surface form
    occs.keys.foldLeft(Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]())( (acc, aSfOcc) => {
      val candOccs = occs.getOrElse(aSfOcc, List[Candidate]())
        .map{ cand: Candidate => {
          Factory.DBpediaResourceOccurrence.from(
            aSfOcc,
            cand.resource,
            contextScores.getOrElse(cand.resource.uri, (0,0.0))
          )}
        }
        .sortBy( o => mixture.getScore(o) )
        .reverse
        .take(k)

      acc + (aSfOcc -> candOccs)
    })

  }

}
