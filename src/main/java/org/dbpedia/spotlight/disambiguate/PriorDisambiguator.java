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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.store.FSDirectory;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.io.DataLoader;
import org.dbpedia.spotlight.model.*;
import org.dbpedia.spotlight.lucene.LuceneManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Preliminary class to take a list of DBpediaResource priors as the only cue to decide which surrogate to choose for a given surface form occurrence.
 * @author pablomendes
 */
public class PriorDisambiguator implements Disambiguator {

    Log LOG = LogFactory.getLog(this.getClass());

    Map<String,Double> priors = new HashMap<String,Double>();

    SurrogateSearcher surrogateSearcher;

    public PriorDisambiguator(SurrogateSearcher surrogates, DataLoader loader) {
        this.surrogateSearcher = surrogates;
        priors = loader.loadPriors();
        LOG.debug(loader+": "+priors.size()+" priors loaded.");
    }

    public List<SurfaceFormOccurrence> spotProbability(List<SurfaceFormOccurrence> sfOccurrences) {
        return sfOccurrences; //FIXME IMPLEMENT
    }
    
    public List<DBpediaResourceOccurrence> disambiguate(List<SurfaceFormOccurrence> sfOccurrences) {
        List<DBpediaResourceOccurrence> disambiguated = new ArrayList<DBpediaResourceOccurrence>();
        int errorCount = 0;                           
        for (SurfaceFormOccurrence sfOcc: sfOccurrences) {
            Set<DBpediaResource> candidates = new HashSet<DBpediaResource>();
            try {
                candidates = surrogateSearcher.get(sfOcc.surfaceForm());
                DBpediaResource best = chooseBest(candidates);
                DBpediaResourceOccurrence dbprOcc = new DBpediaResourceOccurrence(best, sfOcc.surfaceForm(), sfOcc.context(), sfOcc.textOffset(), Provenance.Annotation());
                disambiguated.add(dbprOcc);
            } catch (ItemNotFoundException e) {
                errorCount++;
                LOG.debug(e);
            } catch (SearchException e) {
                errorCount++;
                LOG.debug(e);
            }
        }
        if (errorCount > 0) {
            LOG.info("Number of ItemNotFoundException: "+errorCount);
        }
        return disambiguated;
    }

    private DBpediaResource chooseBest(Set<DBpediaResource> candidates) {
        Map<String,Double> priorsForCandidates = new HashMap<String,Double>();
        DBpediaResource chosen = null;
        try {
            for(DBpediaResource r: candidates) {
                String uri = r.uri();
                //LOG.trace("chooseBest URI: "+uri);
                Double prior = priors.get(uri);
                if (prior==null) {
                    LOG.debug("No prior found for URI: "+uri);
                    prior = 0.0;
                }
                priorsForCandidates.put(uri, prior);
            }
            LOG.trace("Priors for candidates: "+priorsForCandidates);
            chosen = max(priorsForCandidates);
        } catch (NullPointerException e2) {
            LOG.error("NullPointerException here. Resource: "+candidates);
        }
        return chosen;
    }

    //TODO this is a copy+paste from DisambiguationStrategy with minor modifications
    class ValueComparator implements Comparator<Map.Entry<String,Double>> {
        @Override
        public int compare(Map.Entry<String,Double> o1, Map.Entry<String,Double> o2) {
            return o1.getValue().compareTo(o2.getValue());
        }
    }
    public DBpediaResource max(Map<String,Double> countForResource) {
        String chosen = Collections.max((Collection<Map.Entry<String,Double>>) countForResource.entrySet(),
                                                  new ValueComparator())
                                     .getKey();
        return new DBpediaResource(chosen);
    }

    //TODO implement
    @Override
    public List<DBpediaResourceOccurrence> bestK(SurfaceFormOccurrence sfOccurrence, int k) throws SearchException, ItemNotFoundException {
        return new LinkedList<DBpediaResourceOccurrence>();
    }

    public static void main(String[] args) throws IOException {
      String luceneIndexFileName = "data/apple-example/LuceneIndex-apple50_test";
      String resourcePriorsFileName = "data/apple-example/3apples-priors.tsv";

      // Lucene Manager - Controls indexing and searching
      LuceneManager luceneManager = new LuceneManager(FSDirectory.open(new File(luceneIndexFileName)));

        try {
            new PriorDisambiguator(new org.dbpedia.spotlight.lucene.search.SurrogateSearcher(luceneManager), new DataLoader(new DataLoader.TSVParser(), new File("data/Distinct-surfaceForm-By-uri.grouped")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String name() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int trainingSetSize(DBpediaResource resource) throws SearchException {
        // for the WikiPageContext, the training size is always 1 page per resource
        return 1;
    }

    @Override
    public int ambiguity(SurfaceForm sf) throws SearchException {
        int s = 0;
        try {
            s = surrogateSearcher.get(sf).size();
        } catch (ItemNotFoundException e) {
            s = 0; // surface form not found
        }
        return s;
    }
}