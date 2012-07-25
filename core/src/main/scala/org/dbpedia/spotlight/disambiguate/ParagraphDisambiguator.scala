/*
 * *
 *  * Copyright 2011 Pablo Mendes, Max Jakob
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package org.dbpedia.spotlight.disambiguate

import org.apache.lucene.search.Explanation
import org.dbpedia.spotlight.exceptions.InputException
import org.dbpedia.spotlight.exceptions.ItemNotFoundException
import org.dbpedia.spotlight.exceptions.SearchException
import org.dbpedia.spotlight.model._
import java.io.IOException

/**
 * Interface for objects that perform disambiguation at the paragraph level (collective disambiguation of occurrences)
 * TODO model UnnanotatedParagraph class to hold List<SurfaceFormOccurrence> and rename AnnotatedParagraph to AnnotatedParagraph to hold List<DBpediaResourceOccurrence>?</DBpediaResourceOccurrence>
 *
 * @author pablomendes
 * @author maxjakob
 */
trait ParagraphDisambiguator {
    /**
     * Executes disambiguation per paragraph (collection of occurrences).
     * Can be seen as a classification task: unlabeled instances in, labeled instances out.
     *
     * @param paragraph
     * @return
     * @throws org.dbpedia.spotlight.exceptions.SearchException
     * @throws org.dbpedia.spotlight.exceptions.InputException
     */
    def disambiguate(paragraph: Paragraph): List[DBpediaResourceOccurrence]

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
    def bestK(paragraph: Paragraph, k: Int): Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]

    /**
     * Every disambiguator has a name that describes its settings (used in evaluation to compare results)
     * @return a short description of the Disambiguator
     */
    def name: String

    /**
     * Every disambiguator should know how to measure the ambiguity of a surface form.
     * @param sf
     * @return ambiguity of surface form (number of candidates)
     */
    //@throws(classOf[SearchException])
    //def ambiguity(sf: SurfaceForm): Int

    /**
     * Counts how many occurrences we indexed for a given URI. (size of training set for that URI)
     * @param resource
     * @return
     * @throws org.dbpedia.spotlight.exceptions.SearchException
     */
    //@throws(classOf[SearchException])
    //def support(resource: DBpediaResource): Int

    //@throws(classOf[SearchException])
    //def contextTermsNumber(resource: DBpediaResource): Int

    //@throws(classOf[IOException])
    //def averageIdf(context: Text): Double
}