/**
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

package org.dbpedia.spotlight.filter.visitor

import org.dbpedia.spotlight.model.DBpediaResourceOccurrence
import org.dbpedia.spotlight.filter.annotations._
import org.dbpedia.spotlight.model.Factory._
import org.dbpedia.spotlight.sparql.SparqlQueryExecuter
import scala.collection.JavaConverters._
import java.util.{Comparator, Collections}


class OccsFilter(confidence: Double, support: Int,
                 ontologyTypes: String, sparqlQuery: String, blacklist: Boolean, coreferenceResolution: Boolean,
                 simThresholds: java.util.List[java.lang.Double], sparqlExecuter: SparqlQueryExecuter) extends FilterElement {

  //Converting thresholds to scala list
  private val thresholds: List[Double] = simThresholds.asScala.map(v => v.toDouble).toList

  /**
   *
   * Create/return a ConfidenceFilter instance
   *
   * @param simThresholds
   * @param confidence
   * @return
   */
  private def confidenceFilter(simThresholds: List[Double],
                               confidence: Double): ConfidenceFilter = new ConfidenceFilter(simThresholds, confidence)


  /**
   *
   * Create/return a SupportFilter instance
   *
   * @param targetSupport
   * @return
   */
  private def supportFilter(targetSupport: Int): SupportFilter = new SupportFilter(targetSupport)


  /**
   *
   * Create/return a TypeFilter instance
   *
   * @param ontologyTypes
   * @param listColor
   * @return
   */
  private def typeFilter(ontologyTypes: String, listColor: FilterPolicy.ListColor) = {
    val dbpediaTypes = ontologyType.fromCSVString(ontologyTypes)
    new TypeFilter(dbpediaTypes.toList, listColor)
  }


  /**
   *
   * Create/return a SparqlFilter instance
   *
   * @param sparqlExecuter
   * @param sparqlQuery
   * @param listColor
   * @return
   */
  private def sparqlFilter(sparqlExecuter: SparqlQueryExecuter, sparqlQuery: String,
                           listColor: FilterPolicy.ListColor) = new SparqlFilter(sparqlExecuter, sparqlQuery, listColor)

  /**
   *
   * Create/return a PercentageOfSecondFilter instance
   *
   * @param confidence
   * @return
   */
  private def percentageOfSecondFilter(confidence: Double) = new PercentageOfSecondFilter(confidence)


  private val listColor = if (blacklist) FilterPolicy.Blacklist else FilterPolicy.Whitelist


  /**
   * List of available filters
   */
  private val elements: List[FilterElement] = if (thresholds.size == 0)
    List(supportFilter(support),
      typeFilter(ontologyTypes, listColor),
      sparqlFilter(sparqlExecuter, sparqlQuery, listColor),
      confidenceFilter(thresholds, confidence),
      percentageOfSecondFilter(confidence))
  else
    List(supportFilter(support),
      typeFilter(ontologyTypes, listColor),
      sparqlFilter(sparqlExecuter, sparqlQuery, listColor),
      percentageOfSecondFilter(confidence))


  /**
   *
   * Filtering DBpedia resource occurrences using available filters in the list
   *
   * @param visitor
   * @param occs
   * @return
   */
  def accept(visitor: FilterOccsVisitor, occs: java.util.List[DBpediaResourceOccurrence]): java.util.List[DBpediaResourceOccurrence] = {

    var result: java.util.List[DBpediaResourceOccurrence] = new java.util.ArrayList[DBpediaResourceOccurrence]

    //if (coreferenceResolution)
    //  result.addAll(new CoreferenceFilter().filterOccs(occs.asScala.toTraversable).toList.asJava)
    //NOTE: This filter does not work atm, uncomment once fixed

    var unfiltered: java.util.List[DBpediaResourceOccurrence] = occs
    elements.foreach(elem => unfiltered = elem.accept(visitor, unfiltered))

    result.addAll(unfiltered)

    Collections.sort(result, new Comparator[DBpediaResourceOccurrence] {
      def compare(o1: DBpediaResourceOccurrence, o2: DBpediaResourceOccurrence): Int = {
        if (o1.textOffset > o2.textOffset)
          return  1
        -1
      }
    })

    result
  }


}


