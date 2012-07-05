package org.dbpedia.spotlight.db

import model._
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.disambiguate.mixtures.Mixture
import org.apache.commons.logging.LogFactory
import scala.collection.JavaConverters._
import similarity.TFICFSimilarity
import collection.mutable.HashMap


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

  val similarity = new TFICFSimilarity()


  def getScores(text: Text, candidates: Set[Candidate]): Map[Candidate, Double] = {

    val tokens = tokenizer.tokenize(text).map{ ts: String => tokenStore.getToken(ts) }
    val query = tokens.groupBy(identity).mapValues(_.size).asJava

    val contextCounts = candidates.map{ cand: Candidate =>
      (cand, contextStore.getContextCounts(cand.resource))
    }.toMap

    similarity.score(query, contextCounts)
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
        val sf = surfaceFormStore.getSurfaceForm(sfOcc.surfaceForm.name)

        LOG.debug("Searching...")
        val candidates = candidateMap.getCandidates(sf)

        LOG.debug("# candidates for: %s = %s.".format(sf, candidates.size))
        allCandidates ++= candidates

        acc + (sfOcc -> candidates.toList)
      })


    // step2: query once for the paragraph context, get scores for each candidate resource
    val contextScores = getScores(paragraph.text, allCandidates)

    // pick the best k for each surface form
    occs.keys.foldLeft(Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]())( (acc, aSfOcc) => {
      val candOccs = occs.getOrElse(aSfOcc, List[Candidate]())
        .map{ cand: Candidate => {
          new DBpediaResourceOccurrence(
            "",
            cand.resource,
            cand.surfaceForm,
            aSfOcc.context,
            aSfOcc.textOffset,
            Provenance.Undefined,
            0.0,
            0.0,
            contextScores.getOrElse(cand, 0.0)
          )
        }
      }
      .sortBy( o => mixture.getScore(o) )
      .reverse
      .take(k)

      acc + (aSfOcc -> candOccs)
    })

  }

}
