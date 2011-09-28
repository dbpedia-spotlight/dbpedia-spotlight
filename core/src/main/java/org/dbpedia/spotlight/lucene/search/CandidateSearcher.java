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

package org.dbpedia.spotlight.lucene.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.RAMDirectory;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.lucene.LuceneManager;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Implements methods for searching an index that associates surface forms to (candidate) URIs.
 * This searcher does not work with Context. In order to use context to help select the correct URI for a surface forms, @see{ContextSearcher}.
 *
 * NOTE: This was previously called SurrogateSearcher, but we abandoned Candidate from the terminology in favor of Candidate.
 * @author pablomendes
 */
public class CandidateSearcher extends BaseSearcher implements org.dbpedia.spotlight.model.CandidateSearcher {

    final static Log LOG = LogFactory.getLog(CandidateSearcher.class);

    /**
     * For a caseInsensitive behavior, use {@link org.dbpedia.spotlight.lucene.LuceneManager.CaseInsensitiveSurfaceForms}.
     * @param searchManager
     * @throws IOException
     */
    public CandidateSearcher(LuceneManager searchManager) throws IOException {
        this.mLucene = searchManager;
        //LOG.info("Creating in-memory searcher for candidates.");
        //this.mLucene.mContextIndexDir = new RAMDirectory(this.mLucene.mContextIndexDir);
        LOG.info("Using index at: "+this.mLucene.mContextIndexDir);
        LOG.debug("Opening IndexSearcher and IndexReader for Lucene directory "+this.mLucene.mContextIndexDir+" ...");
        this.mReader = IndexReader.open(this.mLucene.mContextIndexDir, true); // read-only=true
        this.mSearcher = new IndexSearcher(this.mReader);
        LOG.debug("Done.");
    }

    /**
     * CandidateSearcher method.
     * @param sf
     * @return
     * @throws org.dbpedia.spotlight.exceptions.SearchException
     */
    @Override
    public Set<DBpediaResource> getCandidates(SurfaceForm sf) throws SearchException {
        Set<DBpediaResource> candidates = new HashSet<DBpediaResource>();

        Query q = mLucene.getQuery(sf);
        LOG.debug("Query: "+q);
        String[] fields = {LuceneManager.DBpediaResourceField.URI.toString()};
        // search index for surface form, iterate through the results
        for (ScoreDoc hit : getHits(q)) {
            int docNo = hit.doc;
            DBpediaResource resource = getDBpediaResource(docNo, fields);
            candidates.add(resource);
        }

        LOG.debug("Candidates for "+sf+"("+candidates.size()+"): "+candidates);

        //TODO for the evaluation, this exception creates problems. But maybe we want to have it at a later stage.
        //if (surrogates.size() == 0)
        //    throw new SearchException("Problem retrieving surrogates for "+sf, new ItemNotFoundException(sf + " not found in (surrogate) index"));

        // return set of surrogates
        return candidates;
    }

    public Set<SurfaceForm> getSurfaceForms(DBpediaResource res) throws SearchException {
        Set<SurfaceForm> surfaceForms = new HashSet<SurfaceForm>();

        // search index for resource, iterate through the results
        for (ScoreDoc hit : getHits(mLucene.getQuery(res))) {
            int docNo = hit.doc;
            SurfaceForm sf = getSurfaceForm(docNo);
            surfaceForms.add(sf);
        }

        LOG.debug("Surrogates for "+res+"("+surfaceForms.size()+"): "+surfaceForms);

        //TODO for the evaluation, this exception creates problems. But maybe we want to have it at a later stage.
        //if (surrogates.size() == 0)
        //    throw new SearchException("Problem retrieving surrogates for "+sf, new ItemNotFoundException(sf + " not found in (surrogate) index"));

        // return set of surrogates
        return surfaceForms;
    }

    public int getAmbiguity(SurfaceForm sf) throws SearchException {
        ScoreDoc[] hits = getHits(mLucene.getQuery(sf));
        return hits.length;
    }
    
}