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

import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.model.vsm.FeatureVector;

import java.util.List;

/**
 * TODO Merge with ContextStore
 */
public interface ContextSearcher { //TODO We should have a ResourceSearcher, OccurrenceSearcher and DefinitionSearcher

    public List<FeatureVector> get(DBpediaResource resource) throws SearchException;

    //TODO this only makes sense for ResourceSearcher
    public DBpediaResource getDBpediaResource(int docNo) throws SearchException;

    public int getNumberOfEntries();

}
