/*
 * Copyright 2011 Pablo Mendes, Max Jakob
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

import org.dbpedia.spotlight.exceptions.InputException
import org.dbpedia.spotlight.exceptions.ItemNotFoundException
import org.dbpedia.spotlight.exceptions.SearchException
import org.dbpedia.spotlight.model._
import scalaj.collection.Imports._
import java.util.HashMap

/**
 * This is a wrapper to the scala interface to make it easier to call from Java code.
 *
 * @author pablomendes
 */
class ParagraphDisambiguatorJ (val disambiguator : ParagraphDisambiguator) {
    /**
     * Executes disambiguation per paragraph (collection of occurrences).
     * Can be seen as a classification task: unlabeled instances in, labeled instances out.
     *
     * @param paragraph
     * @return
     * @throws org.dbpedia.spotlight.exceptions.SearchException
     * @throws org.dbpedia.spotlight.exceptions.InputException
     */
    def disambiguate(paragraph: Paragraph): java.util.List[DBpediaResourceOccurrence] = {
        disambiguator.disambiguate(paragraph).asJava
    }

    /**
     * Executes disambiguation per occurrence, returns a list of possible candidates.
     * Can be seen as a ranking (rather than classification) task: query instance in, ranked list of target URIs out.
     *
     * @param sfOccurrences
     * @param k
     * @return
     * @throws org.dbpedia.spotlight.exceptions.SearchException
     * @throws org.dbpedia.spotlight.exceptions.ItemNotFoundException    when a surface form is not in the index
     * @throws org.dbpedia.spotlight.exceptions.InputException
     */
    @throws(classOf[SearchException])
    @throws(classOf[ItemNotFoundException])
    @throws(classOf[InputException])
    def bestK(paragraph: Paragraph, k: Int): java.util.Map[SurfaceFormOccurrence, java.util.List[DBpediaResourceOccurrence]] = {
        val acc = new HashMap[SurfaceFormOccurrence,java.util.List[DBpediaResourceOccurrence]]();
        disambiguator.bestK(paragraph,k).foreach(b => { acc.put(b._1, b._2.asJava) })
        return acc;
    }

    /**
     * Every disambiguator has a name that describes its settings (used in evaluation to compare results)
     * @return a short description of the Disambiguator
     */
    def name: String = {
        disambiguator.name
    }

}