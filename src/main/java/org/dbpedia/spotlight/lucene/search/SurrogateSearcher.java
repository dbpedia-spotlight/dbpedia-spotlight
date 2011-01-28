package org.dbpedia.spotlight.lucene.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.ScoreDoc;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.model.DBpediaResource;
import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.lucene.LuceneManager;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SurrogateSearcher extends BaseSearcher implements org.dbpedia.spotlight.model.SurrogateSearcher {

    final static Log LOG = LogFactory.getLog(SurrogateSearcher.class);

    /**
     * For a caseInsensitive behavior, use {@link org.dbpedia.spotlight.lucene.LuceneManager.CaseInsensitiveSurfaceForms}.
     * @param searchManager
     * @throws IOException
     */
    public SurrogateSearcher(LuceneManager searchManager) throws IOException {
        super(searchManager);
    }

    /**
     * SURROGATE SEARCHER
     * @param sf
     * @return
     * @throws org.dbpedia.spotlight.exceptions.SearchException
     */
    @Override
    public Set<DBpediaResource> get(SurfaceForm sf) throws SearchException {
        Set<DBpediaResource> surrogates = new HashSet<DBpediaResource>();

        // search index for surface form, iterate through the results
        for (ScoreDoc hit : getHits(mLucene.getQuery(sf))) {
            int docNo = hit.doc;
            DBpediaResource resource = getDBpediaResource(docNo);
            surrogates.add(resource);
        }

        LOG.debug("Surrogates for "+sf+"("+surrogates.size()+"): "+surrogates);

        //TODO for the evaluation, this exception creates problems. But maybe we want to have it at a later stage.
        //if (surrogates.size() == 0)
        //    throw new SearchException("Problem retrieving surrogates for "+sf, new ItemNotFoundException(sf + " not found in (surrogate) index"));

        // return set of surrogates
        return surrogates;
    }

    public Set<SurfaceForm> get(DBpediaResource res) throws SearchException {
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

    public Double getAmbiguity(SurfaceFormOccurrence sfOcc) throws SearchException {
        ScoreDoc[] hits = getHits(mLucene.getQuery(sfOcc.surfaceForm()));
        return new Double(hits.length);
    }
    
}