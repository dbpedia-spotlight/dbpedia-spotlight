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

package org.dbpedia.spotlight.util;

import org.apache.lucene.search.ScoreDoc;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.model.ContextSearcher;
import org.dbpedia.spotlight.model.DBpediaResource;

import java.util.*;

/**
 * Strategy for picking a DBpediaResource from a ranking of ScoreDoc
 * @author pablomendes
 */
public abstract class RankingStrategy {

    ContextSearcher searcher;

    /**
     * Can't be instantiated. Use one of the factory methods topDoc, mostCommonAmongTopK or avgAmongTopK
     * @param searcher
     */
    protected RankingStrategy(ContextSearcher searcher) {
        this.searcher = searcher;
    }

    public abstract DBpediaResource choose(ScoreDoc[] hits) throws SearchException;

    //TODO I would like this to be Double so that we can have probabilities here at some point
    class DoubleValueComparator implements Comparator<Map.Entry<DBpediaResource,Double>> {
        @Override
        public int compare(Map.Entry<DBpediaResource,Double> o1, Map.Entry<DBpediaResource,Double> o2) {
            return o1.getValue().compareTo(o2.getValue()); 
        }
    }

    class IntValueComparator implements Comparator<Map.Entry<DBpediaResource,Integer>> {
        @Override
        public int compare(Map.Entry<DBpediaResource,Integer> o1, Map.Entry<DBpediaResource,Integer> o2) {
            return o1.getValue().compareTo(o2.getValue());
        }
    }

    public DBpediaResource maxCount(Map<DBpediaResource,Integer> countForResource) {
        DBpediaResource chosen = Collections.max((Collection<Map.Entry<DBpediaResource,Integer>>) countForResource.entrySet(),
                                                  new IntValueComparator())
                                     .getKey();
        return chosen;
    }

    public DBpediaResource maxAvg(Map<DBpediaResource,Double> avgForResource) {
        DBpediaResource chosen = Collections.max((Collection<Map.Entry<DBpediaResource,Double>>) avgForResource.entrySet(),
                                                  new DoubleValueComparator())
                                     .getKey();
        return chosen;
    }

    /**
     * Returns the top document in a list of hits as the chosen disambiguation
     * @author pablomendes
     */
    public static class TopDoc extends RankingStrategy {

        public TopDoc(ContextSearcher searcher) { super(searcher); }

        public DBpediaResource choose(ScoreDoc[] hits) throws SearchException {
            return this.searcher.getDBpediaResource(hits[0].doc);
        }
    }

    /**
     * Returns the resource that appears the most in the top occurrences
     * NOTE if used with merged resources this is meaningless.
     * TODO how can we make this safe either way?
     * @author pablomendes
     */
    public static class MostCommonAmongTopK extends RankingStrategy {

        public MostCommonAmongTopK(ContextSearcher searcher) { super(searcher); }

        public DBpediaResource choose(ScoreDoc[] hits) throws SearchException {
            Map<DBpediaResource,Integer> countForResource = new HashMap<DBpediaResource,Integer>();
            // Iterate through the results:
            for (ScoreDoc hit : hits) {
                DBpediaResource r = this.searcher.getDBpediaResource(hit.doc);
                if (countForResource.containsKey(r)) {
                    countForResource.put(r, countForResource.get(r)+1);
                } else {
                    countForResource.put(r, 1);
                }
            }            
            return  maxCount(countForResource);
        }
    }

    public static class BestAverageTopK extends RankingStrategy {

        public BestAverageTopK(ContextSearcher searcher) { super(searcher); }

        public DBpediaResource choose(ScoreDoc[] hits) throws SearchException {
            Map<DBpediaResource,Double> sumForResource = new HashMap<DBpediaResource,Double>();
            Map<DBpediaResource,Integer> countForResource = new HashMap<DBpediaResource,Integer>();
            // Iterate through the results:
            for (ScoreDoc hit : hits) {
                DBpediaResource r = this.searcher.getDBpediaResource(hit.doc);
                if (countForResource.containsKey(r)) {
                    countForResource.put(r, countForResource.get(r)+1);
                    sumForResource.put(r, sumForResource.get(r)+hit.score);
                } else {
                    countForResource.put(r, 1);
                    sumForResource.put(r, new Double(hit.score));
                }
            }
            for (Map.Entry<DBpediaResource,Double> entry: sumForResource.entrySet()) {
                DBpediaResource key = entry.getKey();
                Double sum = entry.getValue();
                Integer count = countForResource.get(key);
                sumForResource.put(key, sum/count);
            }
            return  maxAvg(sumForResource);
        }
    }

}
