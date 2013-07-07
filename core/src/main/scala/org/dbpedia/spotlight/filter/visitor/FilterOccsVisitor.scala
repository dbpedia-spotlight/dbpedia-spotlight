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

import org.dbpedia.spotlight.filter.annotations._
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence

/**
 * Visitor interface
 */
trait FilterOccsVisitor {


  /**
   * SparqlFilter
   *
   * @param sparqlFilter  Filter instance
   * @param occs  DBpedia resource occurrences list
   * @return  DBpedia resource occurrences list filtered
   */
  def visit(sparqlFilter: SparqlFilter, occs : java.util.List[DBpediaResourceOccurrence]): List[DBpediaResourceOccurrence]

  /**
   *  ConfidenceFilter
   *
   * @param confidenceFilter Filter instance
   * @param occs DBpedia resource occurrences list
   * @return DBpedia resource occurrences list filtered
   */
  def visit(confidenceFilter: ConfidenceFilter, occs : java.util.List[DBpediaResourceOccurrence]): List[DBpediaResourceOccurrence]

  /**
   * TypeFilter
   *
   * @param typeFilter Filter instance
   * @param occs DBpedia resource occurrences list
   * @return DBpedia resource occurrences list filtered
   */
  def visit(typeFilter: TypeFilter, occs : java.util.List[DBpediaResourceOccurrence]): List[DBpediaResourceOccurrence]

  /**
   *
   * SupportFilter
   *
   * @param supportFilter Filter instance
   * @param occs DBpedia resource occurrences list
   * @return DBpedia resource occurrences list filtered
   */
  def visit(supportFilter: SupportFilter,occs : java.util.List[DBpediaResourceOccurrence]): List[DBpediaResourceOccurrence]

  /**
   *
   * PercentageOfSecondFilter
   *
   * @param percentageOfSecondFilter Filter instance
   * @param occs DBpedia resource occurrences list
   * @return DBpedia resource occurrences list filtered
   */
  def visit(percentageOfSecondFilter:PercentageOfSecondFilter,occs : java.util.List[DBpediaResourceOccurrence]): List[DBpediaResourceOccurrence]


}
