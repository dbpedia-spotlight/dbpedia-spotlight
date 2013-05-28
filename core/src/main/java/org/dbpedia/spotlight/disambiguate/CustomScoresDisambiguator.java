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

import com.google.common.collect.Ordering;
import com.google.common.primitives.Doubles;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.store.FSDirectory;
import org.dbpedia.spotlight.exceptions.InputException;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.exceptions.TimeoutException;
import org.dbpedia.spotlight.io.DataLoader;
import org.dbpedia.spotlight.lucene.search.LuceneCandidateSearcher;
import org.dbpedia.spotlight.model.*;
import org.dbpedia.spotlight.lucene.LuceneManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Preliminary class to take a list of weights for each DBpediaResource as the only cue to decide which surrogate to choose for a given surface form occurrence.
 * For example, this has been used to get a list of prior probabilities computed offline in a Hadoop/Pig job
 *
 * @author pablomendes
 */
public class CustomScoresDisambiguator implements Disambiguator {

    Log LOG = LogFactory.getLog(this.getClass());

    Map<String,Double> scores = new HashMap<String,Double>();

    CandidateSearcher surrogateSearcher;

    public CustomScoresDisambiguator(CandidateSearcher surrogates, DataLoader loader) {
        this.surrogateSearcher = surrogates;
        if (loader!=null)
            scores = loader.loadPriors();
        LOG.debug(loader+": "+ scores.size()+" scores loaded.");
    }

    public List<SurfaceFormOccurrence> spotProbability(List<SurfaceFormOccurrence> sfOccurrences) {
        return sfOccurrences; //FIXME IMPLEMENT
    }

    @Override
    public DBpediaResourceOccurrence disambiguate(SurfaceFormOccurrence sfOccurrence) throws SearchException, ItemNotFoundException, InputException {
        List<DBpediaResourceOccurrence> candidates = bestK(sfOccurrence, 1);
        if (candidates.size()==0)
            throw new ItemNotFoundException(sfOccurrence.surfaceForm()+" was not found in the index.");
        return candidates.get(0);
    }

    public List<DBpediaResourceOccurrence> disambiguate(List<SurfaceFormOccurrence> sfOccurrences) throws SearchException, InputException {
        List<DBpediaResourceOccurrence> disambiguated = new ArrayList<DBpediaResourceOccurrence>();
        for (SurfaceFormOccurrence sfOcc: sfOccurrences) {
            try {
                disambiguated.add(disambiguate(sfOcc));
            } catch (TimeoutException e) {
                LOG.error("Could not disambiguate. Surface form took too long: "+sfOcc.surfaceForm()+": "+e);
            } catch (ItemNotFoundException e) {
                LOG.error("Could not disambiguate. Surface form not found: "+sfOcc.surfaceForm()+": "+e);
            }
        }
        return disambiguated;
    }

    @Override
    public List<DBpediaResourceOccurrence> bestK(SurfaceFormOccurrence sfOccurrence, int k) throws SearchException, ItemNotFoundException {
        Set<DBpediaResource> candidates = surrogateSearcher.getCandidates(sfOccurrence.surfaceForm());

        if (candidates.size()==0)
            return new LinkedList<DBpediaResourceOccurrence>();

        List<DBpediaResourceOccurrence> all = getScores(sfOccurrence, candidates);

        Ordering descOrder = new Ordering<DBpediaResourceOccurrence>() {
            public int compare(DBpediaResourceOccurrence left, DBpediaResourceOccurrence right) {
                return Doubles.compare(right.similarityScore(), left.similarityScore());

            }
        };

        return descOrder.sortedCopy(all).subList(0, Math.min(k, all.size()));
    }

    protected List<DBpediaResourceOccurrence> getScores(SurfaceFormOccurrence sfOccurrence, Set<DBpediaResource> candidates) {
         List<DBpediaResourceOccurrence> occurrences = new ArrayList<DBpediaResourceOccurrence>();
        try {
            for(DBpediaResource r: candidates) {
                Double score = scores.get(r);
                if (score ==null) {
                    LOG.debug("No score found for URI: "+r);
                    score = 0.0;
                }
                DBpediaResourceOccurrence occ = new DBpediaResourceOccurrence(r,
                    sfOccurrence.surfaceForm(),
                    sfOccurrence.context(),
                    sfOccurrence.textOffset(),
                    score);
                occurrences.add(occ);
            }
        } catch (NullPointerException e2) {
            LOG.error("NullPointerException here. Resource: "+candidates);
        }
        return occurrences;
    }




    public static void main(String[] args) throws IOException {
      String luceneIndexFileName = "data/apple-example/LuceneIndex-apple50_test";
      String resourcePriorsFileName = "data/apple-example/3apples-scores.tsv";

      // Lucene Manager - Controls indexing and searching
      LuceneManager luceneManager = new LuceneManager(FSDirectory.open(new File(luceneIndexFileName)));

        try {
            new CustomScoresDisambiguator(new LuceneCandidateSearcher(luceneManager, false), new DataLoader(new DataLoader.TSVParser(), new File("data/Distinct-surfaceForm-By-uri.grouped")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String name() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int support(DBpediaResource resource) throws SearchException {
        // for the WikiPageContext, the training size is always 1 page per resource
        return 1;
    }

    @Override
    public List<Explanation> explain(DBpediaResourceOccurrence goldStandardOccurrence, int nExplanations) throws SearchException {
        throw new SearchException("Not implemented yet.");
    }

    @Override
    public int ambiguity(SurfaceForm sf) throws SearchException {
        int s = 0;
        try {
            s = surrogateSearcher.getCandidates(sf).size();
        } catch (ItemNotFoundException e) {
            s = 0; // surface form not found
        }
        return s;
    }

    @Override
    public int contextTermsNumber(DBpediaResource resource) throws SearchException {
        return 0;  // prior works without context
    }

    @Override
    public double averageIdf(Text context) throws IOException {
        throw new IOException(this.getClass()+" has no index available to calculate averageIdf");
    }
}