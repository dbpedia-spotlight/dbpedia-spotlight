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
import org.dbpedia.spotlight.disambiguate.{ParagraphDisambiguator, Disambiguator}
import org.dbpedia.spotlight.model.{Paragraph, SurfaceFormOccurrence}

/**
 * Created with IntelliJ IDEA.
 * User: hector
 * Date: 5/30/12
 * Time: 3:56 PM
 */

class GraphBasedDisambiguator extends ParagraphDisambiguator {
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
  def disambiguate(paragraph: Paragraph) = null

  /**
   * Executes disambiguation per occurrence, returns a list of possible candidates.
   * Can be seen as a ranking (rather than classification) task: query instance in, ranked list of target URIs out.
   *
   * @param sfOccurrences
   * @param k
   * @return
   * @throws SearchException
   * @throws ItemNotFoundException    when a surface form is not in the index
   * @throws InputException
   */
  def bestK(paragraph: Paragraph, k: Int) = null
}
