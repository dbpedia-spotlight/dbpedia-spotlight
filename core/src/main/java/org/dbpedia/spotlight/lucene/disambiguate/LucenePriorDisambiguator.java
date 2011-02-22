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
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.search.Explanation;
import org.dbpedia.spotlight.disambiguate.Disambiguator;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.lucene.LuceneManager;
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher;
import org.dbpedia.spotlight.model.*;

import java.io.IOException;
import java.util.*;

/**
 * Does something similar to DBpedia Lookup Service.
 *
 * Gets the URI with the highest prior - given that this URI has appeared at least once with surface form.
 * Prior here is defined as c(uri)/N
 * N = total number of occurrences
 *
 * @author pablomendes
 */
public class LucenePriorDisambiguator implements Disambiguator {

    Log LOG = LogFactory.getLog(this.getClass());
    
    MergedOccurrencesContextSearcher mSearcher;

    public LucenePriorDisambiguator(MergedOccurrencesContextSearcher searcher) throws IOException {
       this.mSearcher = searcher;
    }

    public List<SurfaceFormOccurrence> spotProbability(List<SurfaceFormOccurrence> sfOccurrences) {
        return sfOccurrences; //FIXME IMPLEMENT
    }
    

    @Override
    public List<DBpediaResourceOccurrence> disambiguate(List<SurfaceFormOccurrence> sfOccurrences) {
        //LOG.debug("Retrieving surrogates for surface form: "+sf);

        List<DBpediaResourceOccurrence> disambiguated = new ArrayList<DBpediaResourceOccurrence>();

        for (SurfaceFormOccurrence sfOcc: sfOccurrences) {
            String maxUri = null;
            Long maxCount = Long.MIN_VALUE;
            String[] onlyUri = {LuceneManager.DBpediaResourceField.URI.toString()};
            FieldSelector fieldSelector = new MapFieldSelector(onlyUri);
            try {
                for (Document doc: mSearcher.getDocuments(sfOcc.surfaceForm(), fieldSelector)) { //TODO can create a Map<String,Double> here and give it to a RankingStrategy
                    String[] fields = doc.getValues(LuceneManager.DBpediaResourceField.URI.toString());
                    long numUris = fields.length;
                    if (numUris > maxCount) {
                        maxCount = numUris;
                        maxUri = fields[0];
                    }
                }
            } catch (SearchException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

//            if (docs.size()==0)
//                throw new SearchException("Could not find surface form "+sfOcc.surfaceForm());
            
            //if (maxUri == null)
            //    throw new SearchException("Could not find surface form "+sfOcc.surfaceForm());
               // don't want SearchExceptions; just ignoring this SurfaceFormOccurrence

            disambiguated.add(new DBpediaResourceOccurrence(new DBpediaResource(maxUri),
                    sfOcc.surfaceForm(),
                    sfOcc.context(),
                    sfOcc.textOffset()));
        }

        return disambiguated;
    }

    @Override
    public List<DBpediaResourceOccurrence> bestK(SurfaceFormOccurrence sfOccurrence, int k) throws SearchException, ItemNotFoundException {
        List<DBpediaResourceOccurrence> resultOccs = new LinkedList<DBpediaResourceOccurrence>();

        String[] onlyUri = {LuceneManager.DBpediaResourceField.URI.toString()};
        FieldSelector fieldSelector = new MapFieldSelector(onlyUri);
        for (Document doc : mSearcher.getDocuments(sfOccurrence.surfaceForm(), fieldSelector)) { //TODO can create a Map<String,Double> here and give it to a RankingStrategy
            String[] fields = doc.getValues(LuceneManager.DBpediaResourceField.URI.toString());
            long numUris = fields.length;
            String uri = fields[0];
            new DBpediaResourceOccurrence(new DBpediaResource(uri),
                                          sfOccurrence.surfaceForm(),
                                          sfOccurrence.context(), //TODO can gain some speedup by lazy loading context
                                          sfOccurrence.textOffset(),
                                          1/numUris);  // returns CONDITIONAL prior (given the surface form)!
        }

        if (resultOccs.isEmpty())
            throw new SearchException("Could not find surface form "+sfOccurrence.surfaceForm());

        Collections.sort(resultOccs);
        return resultOccs.subList(0,k);
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
    public int trainingSetSize(DBpediaResource res) throws SearchException {
        return mSearcher.getSupport(res);
    }

    @Override
    public List<Explanation> explain(DBpediaResourceOccurrence goldStandardOccurrence, int nExplanations) throws SearchException {
        return mSearcher.explain(goldStandardOccurrence,nExplanations);
    }

}
