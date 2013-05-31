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

package org.dbpedia.spotlight.lucene;

import org.apache.lucene.index.TermFreqVector;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.vsm.FeatureVector;

/**
 * A lucene vector is a surrogate for a dbpedia resource in our vector space model implemented with lucene.
 * TODO Wondering if this is the same as TFVector (note: it uses TermFreqVector which is a Lucene specific class)
 *
 * User: PabloMendes
 * Date: Jun 29, 2010
 * Time: 1:36:45 PM
 */
public class LuceneFeatureVector implements FeatureVector {

    DBpediaResource resource;
    TermFreqVector vector;

    public LuceneFeatureVector(DBpediaResource resource, TermFreqVector vector) {
        this.resource = resource;
        this.vector = vector;
    }

    public DBpediaResource resource() {
        return resource;
    }

    public TermFreqVector vector() {
        return vector;        
    }

    @Override
    public String toString() {
        return "LuceneFeatureVector["+
                this.resource.uri()+": "+
                this.vector.toString()+"]";
    }
}
