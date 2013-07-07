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


trait FilterElement {

  /**
   *
   * Filtering DBpedia resource occurrences using available filters in the list
   *
   * @param visitor
   * @param occs
   * @return
   */
  def accept(visitor: FilterOccsVisitor,occs : java.util.List[DBpediaResourceOccurrence]): java.util.List[DBpediaResourceOccurrence]

}
