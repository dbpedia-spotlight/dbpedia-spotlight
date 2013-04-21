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

import com.google.common.collect.Ordering;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.search.Explanation;
import org.dbpedia.spotlight.disambiguate.Disambiguator;
import org.dbpedia.spotlight.exceptions.InputException;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.lucene.LuceneManager;
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher;
import org.dbpedia.spotlight.model.*;

import java.io.IOException;
import java.util.*;

/**
 * Does something similar to DBpedia Lookup Service.
 * Gets the URI with the highest prior - given that this URI has appeared at least once with surface form.
 *
 * Prior here is defined as c(uri)/N
 * N = total number of occurrences
 *
 * Sets support in DBpediaResource and ranks by that.
 * TODO set percentage of second?
 * TODO set score?
 *
 * @author pablomendes
 */
public class LucenePriorDisambiguator implements Disambiguator {

    Log LOG = LogFactory.getLog(this.getClass());
    String[] filter = {LuceneManager.DBpediaResourceField.URI.toString(),LuceneManager.DBpediaResourceField.URI_COUNT.toString()};

    MergedOccurrencesContextSearcher mSearcher;

    public LucenePriorDisambiguator(MergedOccurrencesContextSearcher searcher) throws IOException {
       this.mSearcher = searcher;
    }

    public List<SurfaceFormOccurrence> spotProbability(List<SurfaceFormOccurrence> sfOccurrences) {
        return sfOccurrences; //FIXME IMPLEMENT
    }

    @Override
    public DBpediaResourceOccurrence disambiguate(SurfaceFormOccurrence sfOccurrence) throws SearchException, ItemNotFoundException, InputException {
        return bestK(sfOccurrence,1).get(0);
    }

    /**
     * For backwards compatibility - is able to get a count from multiple URI fields or from a URICOUNT field.
     * @param doc
     * @return
     */
//    private Map.Entry<String,Integer> getURICount(Document doc) throws ItemNotFoundException{
//        Map<String,Integer> result = new HashMap<String,Integer>();
//        Integer count = 0;
//
//        String[] uriValues = doc.getValues(LuceneManager.DBpediaResourceField.URI.toString());
//        if (uriValues == null)
//            throw new ItemNotFoundException("URI is not in the index: "+LuceneManager.DBpediaResourceField.URI.toString());
//        String uri = uriValues[0];
//
//        String[] countValues = doc.getValues(LuceneManager.DBpediaResourceField.URI_COUNT.toString());
//        if (countValues==null) {
//            count = uriValues.length; // count is how many times a URI has been added to the index
//        } else {
//            count = new Integer(countValues[0]); // count has been stored as a field
//        }
//        result.put(uri, count);
//        return result.entrySet().iterator().next();
//    }


      @Override
    public List<DBpediaResourceOccurrence> disambiguate(List<SurfaceFormOccurrence> sfOccurrences) throws SearchException, InputException {
        List<DBpediaResourceOccurrence> disambiguated = new ArrayList<DBpediaResourceOccurrence>();

        for (SurfaceFormOccurrence sfOcc: sfOccurrences) {
            try {
                disambiguated.add(disambiguate(sfOcc));
            } catch (ItemNotFoundException e) {
                throw new SearchException("Error in disambiguate. ",e);
            }
        }

        return disambiguated;
     }

    @Override
    public List<DBpediaResourceOccurrence> bestK(SurfaceFormOccurrence sfOccurrence, int k) throws SearchException, ItemNotFoundException {
        List<DBpediaResourceOccurrence> resultOccs = new LinkedList<DBpediaResourceOccurrence>();

        for (DBpediaResource resource: mSearcher.getCandidates(sfOccurrence.surfaceForm())) {
            DBpediaResourceOccurrence occ = new DBpediaResourceOccurrence(resource,
                    sfOccurrence.surfaceForm(),
                    sfOccurrence.context(),
                    sfOccurrence.textOffset());
                    //1 / numUris); //this is not really similarity score. TODO set this or not?

            resultOccs.add(occ);

        }

        if (resultOccs.isEmpty())
            throw new SearchException("Could not find surface form "+sfOccurrence.surfaceForm());

        Ordering descOrder = new Ordering<DBpediaResourceOccurrence>() {
            public int compare(DBpediaResourceOccurrence left, DBpediaResourceOccurrence right) {
                return Doubles.compare(right.resource().support(), left.resource().support());

            }
        };

        return descOrder.sortedCopy(resultOccs).subList(0, Math.min(k, resultOccs.size()));
    }

    @Override
    public String name() {
        return getClass().getSimpleName();
    }

    @Override
    public int ambiguity(SurfaceForm sf) throws SearchException {
        return mSearcher.getAmbiguity(sf);
    }

    @Override
    public int support(DBpediaResource res) throws SearchException {
        return mSearcher.getSupport(res);
    }

    @Override
    public List<Explanation> explain(DBpediaResourceOccurrence goldStandardOccurrence, int nExplanations) throws SearchException {
        return mSearcher.explain(goldStandardOccurrence,nExplanations);
    }

    @Override
    public int contextTermsNumber(DBpediaResource resource) throws SearchException {
        return 0;  // prior works without context
    }

    @Override
    public double averageIdf(Text context) throws IOException {
        throw new IOException(this.getClass()+" has no context available in the index to calculate averageIdf");
    }


}
