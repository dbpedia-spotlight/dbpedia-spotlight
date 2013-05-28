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

package org.dbpedia.spotlight.disambiguate;

import org.apache.lucene.search.Explanation;
import org.dbpedia.spotlight.exceptions.InputException;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.model.*;

import java.io.IOException;
import java.util.List;

/**
 * Interface for occurrence-based disambiguators.
 * TODO consider renaming to OccurrenceDisambiguator
 * @author pablomendes
 * @author maxjakob
 */
public interface Disambiguator {

    public List<SurfaceFormOccurrence> spotProbability(List<SurfaceFormOccurrence> sfOccurrences) throws SearchException;

    /**
     * Executes disambiguation per individual occurrence.
     * Can be seen as a classification task: unlabeled instance in, labeled instance out.
     *
     * @param sfOccurrence
     * @return
     * @throws SearchException
     * @throws ItemNotFoundException
     * @throws InputException
     */
    public DBpediaResourceOccurrence disambiguate(SurfaceFormOccurrence sfOccurrence) throws SearchException, ItemNotFoundException, InputException; //TODO DisambiguationException

    /**
     * Executes disambiguation per paragraph (collection of occurrences).
     * Can be seen as a classification task: unlabeled instances in, labeled instances out.
     *
     * @param sfOccurrences
     * @return
     * @throws SearchException
     * @throws InputException
     */
    public List<DBpediaResourceOccurrence> disambiguate(List<SurfaceFormOccurrence> sfOccurrences) throws SearchException, InputException; //TODO DisambiguationException


    /**
     * Executes disambiguation per occurrence, returns a list of possible candidates.
     * Can be seen as a ranking (rather than classification) task: query instance in, ranked list of target URIs out.
     *
     * @param sfOccurrence
     * @param k
     * @return
     * @throws SearchException
     * @throws ItemNotFoundException
     * @throws InputException
     */
    //TODO consider moving this to CandidateSelector / CandidateSearcher interface
    public List<DBpediaResourceOccurrence> bestK(SurfaceFormOccurrence sfOccurrence, int k) throws SearchException, ItemNotFoundException, InputException;

    /**
     * Every disambiguator has a name that describes its settings (used in evaluation to compare results)
     * @return a short description of the Disambiguator
     */
    public String name();

    /**
     * Every disambiguator should know how to measure the ambiguity of a surface form.
     * @param sf
     * @return ambiguity of surface form (number of candidates)
     */
    public int ambiguity(SurfaceForm sf) throws SearchException;

    /**
     * Counts how many occurrences we indexed for a given URI. (size of training set for that URI)
     * @param resource
     * @return
     * @throws SearchException
     */
    public int support(DBpediaResource resource) throws SearchException;

    public List<Explanation> explain(DBpediaResourceOccurrence goldStandardOccurrence, int nExplanations) throws SearchException;

    public int contextTermsNumber(DBpediaResource resource) throws SearchException;

    public double averageIdf(Text context) throws IOException;

}