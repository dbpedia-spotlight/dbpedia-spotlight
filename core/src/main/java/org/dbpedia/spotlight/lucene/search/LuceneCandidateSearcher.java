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
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.model.CandidateSearcher;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.lucene.LuceneManager;

import java.io.File;
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
public class LuceneCandidateSearcher extends BaseSearcher implements org.dbpedia.spotlight.model.CandidateSearcher {

    final static Log LOG = LogFactory.getLog(LuceneCandidateSearcher.class);

    /**
     * Searches associations between surface forms and URIs
     *
     * @param searchManager  For a caseInsensitive behavior, use {@link org.dbpedia.spotlight.lucene.LuceneManager.CaseInsensitiveSurfaceForms}.
     * @param inMemory if true, will create RAMDirectory, if false, will use directory in searchmanager.
     * @throws IOException
     */
    public LuceneCandidateSearcher(LuceneManager searchManager, boolean inMemory) throws IOException {
        this.mLucene = searchManager;
        if (inMemory) {
            LOG.info("Creating in-memory LuceneCandidateSearcher.");
            this.mLucene.mContextIndexDir = new RAMDirectory(this.mLucene.mContextIndexDir);
        }
        this.mReader = IndexReader.open(this.mLucene.mContextIndexDir); // read-only=true
        this.mSearcher = new IndexSearcher(this.mReader);
        LOG.info(String.format("Opened LuceneCandidateSearcher from %s.", this.mLucene.mContextIndexDir));
    }

    /**
     * LuceneCandidateSearcher method.
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
        for (ScoreDoc hit : getHits(q,100)) {  //TODO Attention: study impact of topResultsLimit
            int docNo = hit.doc;
            //DBpediaResource resource = getDBpediaResource(docNo, fields);
            DBpediaResource resource = getCachedDBpediaResource(docNo);
            candidates.add(resource);
        }

        LOG.debug("Candidates for "+sf+"("+candidates.size()+"): "+candidates);
        if (candidates.size()==0) LOG.debug(String.format("Used index:"+mLucene.mContextIndexDir));

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

    /**
     * Although one could get ambiguity by counting the size of getCandidates(), this method is faster since it does not require loading docs.
     *
     * @param sf
     * @return
     * @throws SearchException
     */
    public int getAmbiguity(SurfaceForm sf) throws SearchException {
        ScoreDoc[] hits = getHits(mLucene.getQuery(sf));
        return hits.length;
    }

    public static void main(String[] args) throws IOException, SearchException, ItemNotFoundException {
        //String dir = "/home/pablo/workspace/spotlight/output/candidateIndexTitRedDis";
        String dir = "/home/pablo/workspace/spotlight/index/output/candidateIndexTitRedDis";
        LuceneManager luceneManager = new LuceneManager.CaseSensitiveSurfaceForms(FSDirectory.open(new File(dir)));
        CandidateSearcher searcher = new LuceneCandidateSearcher(luceneManager, true);
        System.out.println(searcher.getCandidates(new SurfaceForm("berlin")));
        System.out.println(searcher.getCandidates(new SurfaceForm("Berlin")));
        System.out.println(searcher.getCandidates(new SurfaceForm("sdaf")));
    }
}