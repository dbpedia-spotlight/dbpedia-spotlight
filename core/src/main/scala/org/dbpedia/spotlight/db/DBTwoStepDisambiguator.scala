package org.dbpedia.spotlight.db

import model._
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.disambiguate.mixtures.Mixture
import org.apache.commons.logging.LogFactory
import scala.collection.JavaConverters._
import similarity.{ContextSimilarity, TFICFSimilarity}
import org.dbpedia.spotlight.disambiguate.{ParagraphDisambiguator, Disambiguator}
import org.dbpedia.spotlight.exceptions.{SurfaceFormNotFoundException, InputException}
import collection.mutable
import scala.Predef._
import org.dbpedia.spotlight.util.MathUtil
import breeze.{numerics, linalg}


/**
 * A database-backed paragraph disambiguator working in two steps:
 * candidate search and disambiguation.
 *
 * Candidates are searched using a [[org.dbpedia.spotlight.db.DBCandidateSearcher]].
 * For each candidate, we calculate scores, including the context similarity, which
 * are then combined in a Mixture and assigned to the [[org.dbpedia.spotlight.model.DBpediaResourceOccurrence]].
 *
 * @author Joachim Daiber
 * @author pablomendes (Lucene-based TwoStepDisambiguator)
 */

class DBTwoStepDisambiguator(
  tokenStore: TokenTypeStore,
  surfaceFormStore: SurfaceFormStore,
  resourceStore: ResourceStore,
  candidateSearcher: DBCandidateSearcher,
  contextStore: ContextStore,
  mixture: Mixture,
  contextSimilarity: ContextSimilarity
) extends ParagraphDisambiguator {

  private val LOG = LogFactory.getLog(this.getClass)

  /* Tokenizer that may be used for tokenization if the text is not already tokenized. */
  var tokenizer: Tokenizer = null

  private def getQuery(tokenTypes: Seq[TokenType]): java.util.Map[TokenType, Int]
    = tokenTypes.groupBy(identity).mapValues(_.size).asJava

  private def getQuery(text: Text): java.util.Map[TokenType, Int]
    = getQuery(text.featureValue[List[Token]]("tokens").get.map{ t: Token => t.tokenType })

  /**
   * Calculate the context similarity given the text for all candidates in the set.
   *
   * @param text the context
   * @param candidates the set of candidates for a surface form
   * @return
   */
  def getContextSimilarityScores(text: Text, candidates: Set[DBpediaResource]): mutable.Map[DBpediaResource, Double] = {

    val query = getQuery(text)

    val contextCounts = candidates.map{ candRes: DBpediaResource =>
      (candRes -> contextStore.getContextCounts(candRes))
    }.toMap

    val totalContextCounts = candidates.map{ candRes: DBpediaResource =>
      (candRes -> contextStore.getTotalTokenCount(candRes))
    }.toMap

    contextSimilarity.score(query, contextCounts, totalContextCounts)
  }

  //maximum number of considered candidates
  val MAX_CANDIDATES = 20

  def bestK(paragraph: Paragraph, k: Int): Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]] = {

    LOG.debug("Running bestK for paragraph %s.".format(paragraph.id))

    if (paragraph.occurrences.size == 0)
      return Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]()

    //Tokenize the text if it wasn't tokenized before:
    if (tokenizer != null) {
      LOG.info("Tokenizing input text...")
      val tokens = tokenizer.tokenize(paragraph.text)
      paragraph.text.setFeature(new Feature("tokens", tokens))
    }

    // step1: get candidates for all surface forms
    var allCandidateResources = Set[DBpediaResource]()
    val occs = paragraph.occurrences.foldLeft(
      Map[SurfaceFormOccurrence, List[Candidate]]())(
      (acc, sfOcc) => {

        LOG.debug("Searching...")

        val candidateRes = {
          val sf = try {
            surfaceFormStore.getSurfaceForm(sfOcc.surfaceForm.name)
          } catch {
            case e: SurfaceFormNotFoundException => sfOcc.surfaceForm
          }

          val cands = candidateSearcher.getCandidates(sf)
          LOG.debug("# candidates for: %s = %s.".format(sf, cands.size))

          if (cands.size > MAX_CANDIDATES) {
            LOG.debug("Reducing number of candidates to %d.".format(MAX_CANDIDATES))
            cands.toList.sortBy( _.prior ).reverse.take(MAX_CANDIDATES).toSet
          } else {
            cands
          }
        }


        allCandidateResources ++= candidateRes.map(_.resource)

        acc + (sfOcc -> candidateRes.toList)
      })


    // step2: query once for the paragraph context, get scores for each candidate resource
    val contextScores = getContextSimilarityScores(paragraph.text, allCandidateResources)

    // pick the best k for each surface form
    occs.keys.foldLeft(Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]())( (acc, aSfOcc) => {

      //Get the NIL entity:
      val eNIL = new DBpediaResourceOccurrence(
        new DBpediaResource("--nil--"),
        aSfOcc.surfaceForm,
        paragraph.text,
        -1
      )

      aSfOcc.featureValue[Array[TokenType]]("token_types") match {
        case Some(t) => eNIL.setFeature(new Score("P(s|e)", contextSimilarity.nilScore(getQuery(t))))
        case _ =>
      }

      val nilContextScore = contextSimilarity.nilScore(getQuery(aSfOcc.context))
      eNIL.setFeature(new Score("P(c|e)", nilContextScore))
      eNIL.setFeature(new Score("P(e)",   MathUtil.ln( 1 / surfaceFormStore.getTotalAnnotatedCount.toDouble ) )) //surfaceFormStore.getTotalAnnotatedCount = total number of entity mentions
      val nilEntityScore = mixture.getScore(eNIL)

      //Get all other entities:
      val candOccs = occs.getOrElse(aSfOcc, List[Candidate]())
        .map{ cand: Candidate => {
        val resOcc = new DBpediaResourceOccurrence(
          "",
          cand.resource,
          cand.surfaceForm,
          aSfOcc.context,
          aSfOcc.textOffset,
          Provenance.Undefined,
          0.0,
          0.0,
          contextScores.getOrElse(cand.resource, 0.0)
        )

        //Set the scores as features for the resource occurrence:

        //Note that this is not mathematically correct, since the candidate prior is P(e|s),
        //the correct P(s|e) should be MathUtil.ln( cand.support / cand.resource.support.toDouble )
        resOcc.setFeature(new Score("P(s|e)", MathUtil.ln( cand.prior )))
        resOcc.setFeature(new Score("P(c|e)", resOcc.contextualScore))
        resOcc.setFeature(new Score("P(e)",   MathUtil.ln( cand.resource.prior )))

        //Use the mixture to combine the scores
        resOcc.setSimilarityScore(mixture.getScore(resOcc))

        resOcc
      }
      }
        .filter{ o => !java.lang.Double.isNaN(o.similarityScore) && o.similarityScore > nilEntityScore }
        .sortBy( o => o.similarityScore )
        .reverse
        .take(k)

      (1 to candOccs.size-1).foreach{ i: Int =>
        val top = candOccs(i-1)
        val bottom = candOccs(i)
        top.setPercentageOfSecondRank(MathUtil.exp(bottom.similarityScore - top.similarityScore))
      }

      //Compute the final score as a softmax function, get the total score first:
      val similaritySoftMaxTotal = linalg.softmax(candOccs.map(_.similarityScore) :+ nilEntityScore)
      val contextSoftMaxTotal    = linalg.softmax(candOccs.map(_.contextualScore) :+ nilContextScore )

      candOccs.foreach{ o: DBpediaResourceOccurrence =>
        o.setSimilarityScore( MathUtil.exp(o.similarityScore - similaritySoftMaxTotal) ) // e^xi / \sum e^xi
        o.setContextualScore( MathUtil.exp(o.contextualScore - contextSoftMaxTotal) )    // e^xi / \sum e^xi
      }

      acc + (aSfOcc -> candOccs)
    })

  }

  @throws(classOf[InputException])
  def disambiguate(paragraph: Paragraph): List[DBpediaResourceOccurrence] = {
    // return first from each candidate set
    bestK(paragraph, 5)
      .filter(kv =>
      kv._2.nonEmpty)
      .map( kv =>
      kv._2.head)
      .toList
      .sortBy(_.textOffset)
  }

  def name = "Database-backed 2 Step disambiguator (%s, %s)".format(contextSimilarity.getClass.getSimpleName, mixture.toString)

}
