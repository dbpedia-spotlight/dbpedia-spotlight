/*
 * Copyright 2012 DBpedia Spotlight Development Team
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
 */

package org.dbpedia.spotlight.disambiguate

import org.apache.commons.logging.LogFactory
import com.officedepot.cdap2.collection.CompactHashSet
import org.dbpedia.spotlight.lucene.LuceneManager
import java.io.File
import org.dbpedia.spotlight.lucene.similarity.{CachedInvCandFreqSimilarity, JCSTermCache}
import org.dbpedia.spotlight.lucene.search.{MergedOccurrencesContextSearcher, LuceneCandidateSearcher}
import scalaj.collection.Imports._
import org.dbpedia.spotlight.exceptions.{ItemNotFoundException, SearchException, InputException}
import org.dbpedia.spotlight.exceptions.SearchException
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguator
import org.dbpedia.spotlight.model._

/**
 * Created with IntelliJ IDEA.
 * User: hector
 * Date: 5/30/12
 * Time: 3:56 PM
 */

/**
 * This class implements ParagraphDisambiguator. A graph relation graph
 * will be constructed to leverage the overall disambiguation decisions.
 *
 * Disambiguators implemented in the collective module can only accept
 * paragraphs as parameters. One occurrence cannot be disambiguated
 * collectively
 *
 * @author hectorliu
 */

class GraphBasedDisambiguator(val factory: SpotlightFactory) extends ParagraphDisambiguator {

  val configuration = factory.configuration

  private val LOG = LogFactory.getLog(this.getClass)

  LOG.info("Initializing disambiguator object ...")

  //similar intialization copied from TwoStepDisambiguator
  val contextIndexDir = LuceneManager.pickDirectory(new File(configuration.getContextIndexDirectory))
  val contextLuceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(contextIndexDir)
  // use this if all surface forms in the index are lower-cased
  val cache = JCSTermCache.getInstance(contextLuceneManager, configuration.getMaxCacheSize);
  contextLuceneManager.setContextSimilarity(new CachedInvCandFreqSimilarity(cache)) // set most successful Similarity
  contextLuceneManager.setDBpediaResourceFactory(configuration.getDBpediaResourceFactory)
  contextLuceneManager.setDefaultAnalyzer(configuration.getAnalyzer)
  val contextSearcher: MergedOccurrencesContextSearcher = new MergedOccurrencesContextSearcher(contextLuceneManager)

  var candidateSearcher: CandidateSearcher = null
  //TODO move to factory
  var candLuceneManager: LuceneManager = contextLuceneManager;
  if (configuration.getCandidateIndexDirectory != configuration.getContextIndexDirectory) {
    val candidateIndexDir = LuceneManager.pickDirectory(new File(configuration.getCandidateIndexDirectory))
    //candLuceneManager = new LuceneManager.CaseSensitiveSurfaceForms(candidateIndexDir)
    candLuceneManager = new LuceneManager(candidateIndexDir)
    candLuceneManager.setDBpediaResourceFactory(configuration.getDBpediaResourceFactory)
    candidateSearcher = new LuceneCandidateSearcher(candLuceneManager, true) // or we can provide different functionality for surface forms (e.g. n-gram search)
    LOG.info("CandidateSearcher initialized from %s".format(candidateIndexDir))
  } else {
    candidateSearcher = contextSearcher match {
      case cs: CandidateSearcher => cs
      case _ => new LuceneCandidateSearcher(contextLuceneManager, false) // should never happen
    }
  }


  /**
   * Every disambiguator has a name that describes its settings (used in evaluation to compare results)
   * @return a short description of the Disambiguator
   */
  def name = {
    this.getClass.getSimpleName
  }

  /**
   * Executes disambiguation per paragraph (collection of occurrences).
   * Can be seen as a classification task: unlabeled instances in, labeled instances out.
   *
   * Will use a graph based method to leverage all entity disambiguation decision in paragraph together
   *
   * @param paragraph
   * @return
   * @throws SearchException
   * @throws InputException
   */
  def disambiguate(paragraph: Paragraph) = {
    //TODO Could consider implement this method in ParagraphDisambiguatorJ
    // Actually this function could be the same for all disambiguators,
    // given that they all needs to implements bestK
    // return first from each candidate set
    bestK(paragraph, 5)
      .filter(kv =>
      kv._2.nonEmpty)
      .map(kv =>
      kv._2.head)
      .toList
  }

  /**
   * Executes disambiguation per paragraph, returns a list of possible candidates.
   * Can be seen as a ranking (rather than classification) task: query instance in, ranked list of target URIs out.
   *
   * @param paragraph
   * @param k
   * @return
   * @throws SearchException
   * @throws ItemNotFoundException    when a surface form is not in the index
   * @throws InputException
   */
  def bestK(paragraph: Paragraph, k: Int): Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]] = {

    LOG.debug("Running bestK for paragraph %s.".format(paragraph.id))

    if (paragraph.occurrences.size == 0) return Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]()

    val sf2Candidates = paragraph.occurrences.foldLeft(
      Map[SurfaceFormOccurrence, List[DBpediaResource]]()
    )(
      (sf2Cands, sfOcc) => {
        val cands = getCandidates(sfOcc.surfaceForm).toList
        //debug
        cands.foreach(println)
        sf2Cands + (sfOcc -> cands)
      })

    val graph = new ReferentGraph(sf2Candidates)

    null
  }

  def getInitalImportance(sfOcc: SurfaceFormOccurrence, cand: DBpediaResource) {

  }

  //Quite a mess of conversion between Java Hashset, Set and Scala Set
  def getCandidates(sf: SurfaceForm): Set[DBpediaResource] = {
    var candidates = new java.util.HashSet[DBpediaResource]().asScala

    try {
      candidates = candidateSearcher.getCandidates(sf).asScala
    } catch {
      case se:
        SearchException => LOG.debug(se)
      case infe:
        ItemNotFoundException => LOG.debug(infe)
    }
    candidates
  }

}