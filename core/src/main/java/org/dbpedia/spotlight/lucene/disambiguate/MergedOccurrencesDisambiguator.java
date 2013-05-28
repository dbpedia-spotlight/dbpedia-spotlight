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

package org.dbpedia.spotlight.lucene.disambiguate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.ScoreDoc;
import org.dbpedia.spotlight.disambiguate.Disambiguator;
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguator;
import org.dbpedia.spotlight.exceptions.*;
import org.dbpedia.spotlight.lucene.LuceneFeatureVector;
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher;
import org.dbpedia.spotlight.model.*;
import org.dbpedia.spotlight.model.vsm.FeatureVector;

import java.io.IOException;
import java.util.*;

public class MergedOccurrencesDisambiguator implements Disambiguator {

    final Log LOG = LogFactory.getLog(this.getClass());

    MergedOccurrencesContextSearcher mMergedSearcher;

    public MergedOccurrencesDisambiguator(ContextSearcher searcher) throws IOException, ConfigurationException {
        //TODO this is horrible, but it's a temp fix until we organize the interfaces
        //FIXME
        if (searcher instanceof MergedOccurrencesContextSearcher) {
            mMergedSearcher = (MergedOccurrencesContextSearcher) searcher;
        } else {
            throw new ConfigurationException("You cannot use MergedOccurrencesDisambiguator with a searcher that is not MergedOccurrencesContextSearcher.");
        }
    }

    public MergedOccurrencesDisambiguator(MergedOccurrencesContextSearcher searcher) throws IOException {
        this.mMergedSearcher = searcher;
    }

    /*
      IDF of surface form
      */
    public Double importance(SurfaceForm sf) throws SearchException {
        long freqUnannotated = this.mMergedSearcher.getConceptNeighborhoodCount(sf);
        long totalDocs = 1;
        try {
            totalDocs = this.mMergedSearcher.getNumberOfResources();
        } catch (IOException e) {
            throw new SearchException("Error getting count of resources. ", e);
        }
        //long freqAnnotated = this.mMergedSearcher.getAmbiguity(sf);
        double prob = (freqUnannotated == 0) ?  0.0 : Math.log(totalDocs / (1+freqUnannotated)); //TODO throw exception: freqUnannotated==0 should not happen!
        //LOG.info(String.format("FreqAnnotated %s; FreqUnnanotated %s; Prob: %s", freqAnnotated, freqUnannotated, prob));
        LOG.info(String.format("FreqAnnotated %s; FreqUnnanotated %s; Prob: %s", 1, freqUnannotated, prob));
        return prob;
    }

    @Override
    @Deprecated
    public List<SurfaceFormOccurrence> spotProbability(List<SurfaceFormOccurrence> sfOccurrences) throws SearchException {
        return sfOccurrences;
    }

    public DBpediaResourceOccurrence disambiguate(SurfaceFormOccurrence sfOcc) throws SearchException, ItemNotFoundException, InputException  {
        List<DBpediaResourceOccurrence> occs = bestK(sfOcc,1);
        if (occs.size()==0)
            throw new ItemNotFoundException(String.format("Surface form not found: %s",sfOcc.surfaceForm().toString()));
        return occs.get(0);
    }

    public List<DBpediaResourceOccurrence> bestK(SurfaceFormOccurrence sfOccurrence, int k) throws SearchException, ItemNotFoundException, InputException {
        LOG.debug("Disambiguating "+sfOccurrence.surfaceForm());

        // search index for surface form
        ScoreDoc[] hits = mMergedSearcher.getHits(sfOccurrence);

        if (hits.length == 0) { //TODO this hack can be implemented correctly as an analyzer that sits within getQuery in LuceneManager.
            String sfName = sfOccurrence.surfaceForm().name().trim();
            if (sfName.toLowerCase().startsWith("the ")) {
                LOG.debug("Trying to HACK(the) -> not found in index: "+sfOccurrence);
                String newName = sfName.substring(3).trim();
                hits = mMergedSearcher.getHitsSurfaceFormHack(sfOccurrence, new SurfaceForm(newName));
                LOG.debug("New sfName="+newName+" hits="+hits.length);
            } else if (sfName.toLowerCase().startsWith("a ")) {
                LOG.debug("Trying to HACK(a) -> not found in index: "+sfOccurrence);
                String newName = sfName.substring(1).trim();
                hits = mMergedSearcher.getHitsSurfaceFormHack(sfOccurrence, new SurfaceForm(newName));
                LOG.debug("New sfName="+newName+" hits="+hits.length);
            }
            if (hits.length == 0 && sfName.toLowerCase().endsWith("'s")) {
                LOG.debug("Trying to HACK('s) -> not found in index: "+sfOccurrence);
                String newName = sfName.substring(0,sfName.length()-2).trim();
                hits = mMergedSearcher.getHitsSurfaceFormHack(sfOccurrence, new SurfaceForm(newName));
                LOG.debug("New sfName="+newName+" hits="+hits.length);
            }
            if (hits.length == 0 && sfName.toLowerCase().endsWith("s")) {
                LOG.debug("Trying to HACK(s) -> not found in index: "+sfOccurrence);
                String newName = sfName.substring(0,sfName.length()-1).trim();
                hits = mMergedSearcher.getHitsSurfaceFormHack(sfOccurrence, new SurfaceForm(newName));
                LOG.debug("New sfName="+newName+" hits="+hits.length);
            }
            if (hits.length == 0 && sfName.toLowerCase().endsWith("'")) {
                LOG.debug("Trying to HACK(') -> not found in index: "+sfOccurrence);
                String newName = sfName.substring(0,sfName.length()-1).trim();
                hits = mMergedSearcher.getHitsSurfaceFormHack(sfOccurrence, new SurfaceForm(newName));
                LOG.debug("New sfName="+newName+" hits="+hits.length);
            }
        }

        // Loop through all hits, build a map from URI to score
        List<DBpediaResourceOccurrence> rankedOccs = new LinkedList<DBpediaResourceOccurrence>();

        if (hits.length > 0) {
            for (int i=0; i < hits.length && i < k; i++) {
                DBpediaResource resource = mMergedSearcher.getDBpediaResource(hits[i].doc);
                //resource can be null! not handled here
                //if (resource==null)
                //    throw new ItemNotFoundException("Could not choose a URI for "+sfOcc.surfaceForm());

                Double score = new Double(hits[i].score);
                Double percentageOfSecond = new Double(-1);
                if (hits.length > i+1) {
                    percentageOfSecond = hits[i+1].score / score;
                }
                DBpediaResourceOccurrence resultOcc = new DBpediaResourceOccurrence("",
                        resource,
                        sfOccurrence.surfaceForm(),
                        sfOccurrence.context(),
                        sfOccurrence.textOffset(),
                        Provenance.Annotation(),
                        score,
                        percentageOfSecond,
                        score); //TODO abusing what was spotProb here. now we have contextual score. need better way to do this
                rankedOccs.add(resultOcc);
            }

            LOG.debug(String.format("Object creation time took %f ms.",mMergedSearcher.objectCreationTime/1000000.0));
            mMergedSearcher.objectCreationTime = 0;
        } else {
            LOG.debug(String.format("Not found in index: %s", sfOccurrence.surfaceForm().toString()));
        }

        return rankedOccs;
    }

    public List<DBpediaResourceOccurrence> disambiguate(List<SurfaceFormOccurrence> sfOccs) throws InputException {
        long startTime = System.currentTimeMillis();

        List<DBpediaResourceOccurrence> results = new LinkedList<DBpediaResourceOccurrence>();

        for (SurfaceFormOccurrence sfOcc : sfOccs) {
            try {
                results.add(disambiguate(sfOcc));
            } catch (ItemNotFoundException e) {
                LOG.error("Could not disambiguate "+sfOcc.surfaceForm()+": "+e);
            } catch (SearchException e) {
                LOG.error("Could not disambiguate "+sfOcc.surfaceForm()+": "+e);
            }
        }

        double totalSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
        LOG.info("Total time of all disambiguations: "+totalSeconds+" s");
        LOG.info("Average time of one disambiguation: "+totalSeconds/sfOccs.size()+" s");  // counts a cache look-up as disambiguation
        
        return results;
    }

    @Override
    public String name() {
        return this.getClass().getSimpleName() + ":" + mMergedSearcher.getSimilarity().getClass().getSimpleName();
    }

    @Override
    public int ambiguity(SurfaceForm sf) throws SearchException {
        return mMergedSearcher.getAmbiguity(sf);
    }

    public int support(DBpediaResource res) throws SearchException {
        int n = 0;
        try {
         n = mMergedSearcher.getSupport(res);
        } catch (SearchException e) {
            if (!e.getCause().getMessage().equals("read past EOF"))
                throw e;
        }
        return n;
    }

    /**
     * Generates explanations for how a given SurfaceFormOccurrence has been disambiguated into a DBpediaResourceOccurrence
     * @param goldStandardOccurrence
     * @param nExplanations
     * @return a list of explanations
     * @throws SearchException
     */
    public List<Explanation> explain(DBpediaResourceOccurrence goldStandardOccurrence, int nExplanations) throws SearchException {
        return mMergedSearcher.explain(goldStandardOccurrence, nExplanations);
    }


    @Override
    public int contextTermsNumber(DBpediaResource resource) throws SearchException {
        int termsCount = 0;
        for (ScoreDoc hit : mMergedSearcher.getHits(resource)) {
            TermFreqVector vector = mMergedSearcher.getVector(hit.doc);
            termsCount += vector.getTerms().length;
        }
        return termsCount;
    }

    @Override
    public double averageIdf(Text context) throws IOException {
        return mMergedSearcher.getAverageIdf(context);
    }

}
