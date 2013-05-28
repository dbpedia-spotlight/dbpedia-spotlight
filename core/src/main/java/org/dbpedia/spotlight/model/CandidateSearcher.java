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

package org.dbpedia.spotlight.model;

import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.exceptions.SearchException;

import java.util.Set;

/**
 * Interface describing the functionality of a candidate map (surface form -> URIs)
 *
 * @author pablomendes
 */
public interface CandidateSearcher {

    /**
     * Retrieves all DBpedia Resources that can be confused with surface form sf.
     * @param sf
     * @return
     * @throws SearchException
     * @throws ItemNotFoundException
     */
    public Set<DBpediaResource> getCandidates(SurfaceForm sf) throws SearchException, ItemNotFoundException;

    /**
     * Retrieves the number of DBpedia Resources that can be confused with surface form sf.
     * Separated from getCandidates since some implementations have faster ways or computing ambiguity without retrieving candidates (e.g.Lucene)
     *
     * @param sf
     * @return
     * @throws SearchException
     */
    public int getAmbiguity(SurfaceForm sf) throws SearchException;

}
