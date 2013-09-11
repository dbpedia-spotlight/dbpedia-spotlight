/**
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dbpedia.spotlight.disambiguate

import mixtures.LinearRegressionMixture
import org.dbpedia.spotlight.lucene.LuceneManager
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher
import java.io.File
import org.dbpedia.spotlight.lucene.similarity._
import org.dbpedia.spotlight.log.SpotlightLog
import org.apache.lucene.search.Explanation
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.lucene.disambiguate.{MixedWeightsDisambiguator, MergedOccurrencesDisambiguator}
import org.dbpedia.spotlight.exceptions.{ItemNotFoundException, SearchException, InputException}
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

/**
 * Default implementation of the disambiguation functionality.
 * Uses classes and parameters we found to perform best (targeting new users that just want to test the system.)
 * This implementation will change with time, as we evolve the system.
 * If you want a stable implementation, copy this class to MyDisambiguator and use that instead.
 *
 * @author maxjakob
 * @author pablomendes
 */
class DefaultDisambiguator(val contextSearcher: ContextSearcher) extends Disambiguator with ParagraphDisambiguator  {

    SpotlightLog.info(this.getClass, "Initializing disambiguator object ...")

    val disambiguator : Disambiguator = new MergedOccurrencesDisambiguator(contextSearcher)

    SpotlightLog.info(this.getClass, "Done.")

    def disambiguate(sfOccurrence: SurfaceFormOccurrence): DBpediaResourceOccurrence = {
        disambiguator.disambiguate(sfOccurrence)
    }

    @throws(classOf[InputException])
    def disambiguate(sfOccurrences: java.util.List[SurfaceFormOccurrence]): java.util.List[DBpediaResourceOccurrence] = {
        disambiguator.disambiguate(sfOccurrences)
    }

    def bestK(sfOccurrence: SurfaceFormOccurrence, k: Int): java.util.List[DBpediaResourceOccurrence] = {
        disambiguator.bestK(sfOccurrence, k)
    }

    /**
     * Executes disambiguation per paragraph (collection of occurrences).
     * Can be seen as a classification task: unlabeled instances in, labeled instances out.
     *
     * @param paragraph
     * @return
     * @throws org.dbpedia.spotlight.exceptions.SearchException
     * @throws org.dbpedia.spotlight.exceptions.InputException
     */
    def disambiguate(paragraph: Paragraph): List[DBpediaResourceOccurrence] = {
        asBuffer(disambiguator.disambiguate(paragraph.occurrences.asJava)).toList
    }

    /**
     * Executes disambiguation per occurrence, returns a list of possible candidates.
     * Can be seen as a ranking (rather than classification) task: query instance in, ranked list of target URIs out.
     *
     * @param k
     * @return
     * @throws org.dbpedia.spotlight.exceptions.SearchException
     * @throws org.dbpedia.spotlight.exceptions.ItemNotFoundException    when a surface form is not in the index
     * @throws org.dbpedia.spotlight.exceptions.InputException
     */
    @throws(classOf[SearchException])
    @throws(classOf[ItemNotFoundException])
    @throws(classOf[InputException])
    def bestK(paragraph: Paragraph, k: Int): Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]] = {
        paragraph.occurrences.foldLeft(Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]())
            { (acc,o) => acc + (o -> asScalaBuffer(disambiguator.bestK(o,k)).toList) }
    }

    def name() : String = {
        "Default:"+disambiguator.name
    }

    def ambiguity(sf : SurfaceForm) : Int = {
        disambiguator.ambiguity(sf)
    }

    def support(resource : DBpediaResource) : Int = {
        disambiguator.support(resource)
    }

    def spotProbability(sfOccurrences: java.util.List[SurfaceFormOccurrence]): java.util.List[SurfaceFormOccurrence] = {
        disambiguator.spotProbability(sfOccurrences)
    }

    @throws(classOf[SearchException])
    def explain(goldStandardOccurrence: DBpediaResourceOccurrence, nExplanations: Int) : java.util.List[Explanation] = {
        disambiguator.explain(goldStandardOccurrence, nExplanations)
    }

    def contextTermsNumber(resource : DBpediaResource) : Int = {
        disambiguator.contextTermsNumber(resource)
    }

    def averageIdf(context : Text) : Double = {
        disambiguator.averageIdf(context)
    }

}