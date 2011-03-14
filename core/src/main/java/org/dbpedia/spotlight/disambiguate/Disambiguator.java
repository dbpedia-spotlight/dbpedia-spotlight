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
 * Interface for disambiguators.
 */

public interface Disambiguator {

    public List<SurfaceFormOccurrence> spotProbability(List<SurfaceFormOccurrence> sfOccurrences) throws SearchException;

    public List<DBpediaResourceOccurrence> disambiguate(List<SurfaceFormOccurrence> sfOccurrences) throws SearchException, InputException, ItemNotFoundException; //TODO DisambiguationException

    public List<DBpediaResourceOccurrence> bestK(SurfaceFormOccurrence sfOccurrence, int k) throws SearchException, ItemNotFoundException, InputException;

    /**
     * Every disambiguator has a name that describes its settings (used in evaluation to compare results)
     * @return a short description of the Disambiguator
     */
    public String name();

    /**
     * Every disambiguator should know how to measure the ambiguity of a surface form.
     * @param sf
     * @return ambiguity of surface form (number of surrogates)
     */
    public int ambiguity(SurfaceForm sf) throws SearchException;

    /**
     * Counts how many occurrences we indexed for a given URI. (size of training set for that URI)
     * @param resource
     * @return
     * @throws SearchException
     */
    public int trainingSetSize(DBpediaResource resource) throws SearchException;

    public List<Explanation> explain(DBpediaResourceOccurrence goldStandardOccurrence, int nExplanations) throws SearchException;

    public int contextTermsNumber(DBpediaResource resource) throws SearchException;

    public double averageIdf(Text context) throws IOException;

}