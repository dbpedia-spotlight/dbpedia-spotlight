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

package org.dbpedia.spotlight.lucene.similarity;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Searcher;

import java.io.IOException;

/**
 * Interface for Similarity classes that use our custom term cache
 *
 * @author pablomendes
 */
public interface CachedSimilarity {

    public TermCache getTermCache();

    public void setTermCache(TermCache termCache);

    public Explanation.IDFExplanation idfExplain(final Term surfaceFormTerm, final Term term, final Searcher searcher) throws IOException;
}
