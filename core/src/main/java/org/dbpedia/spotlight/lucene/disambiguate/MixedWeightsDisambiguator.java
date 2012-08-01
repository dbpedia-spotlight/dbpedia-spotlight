/*
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
import org.dbpedia.spotlight.disambiguate.mixtures.Mixture;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.exceptions.InputException;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.model.ContextSearcher;
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 08.03.11
 * Time: 13:09
 * To change this template use File | Settings | File Templates.
 */
public class MixedWeightsDisambiguator extends MergedOccurrencesDisambiguator {
    private Log LOG = LogFactory.getLog(getClass());

    Mixture mixture;

    public MixedWeightsDisambiguator(ContextSearcher searcher, Mixture mixture) throws IOException, ConfigurationException {
        super(searcher);
        this.mixture = mixture;
    }

    @Override
    public DBpediaResourceOccurrence disambiguate(SurfaceFormOccurrence sfOcc) throws SearchException, ItemNotFoundException, InputException {
        DBpediaResourceOccurrence resultOcc = null;

        List<DBpediaResourceOccurrence> bestKsuper = super.bestK(sfOcc, Integer.MAX_VALUE);

        double best = -Double.MAX_VALUE;
        double second;
        for(DBpediaResourceOccurrence occ : bestKsuper) {
            double mixedScore = mixture.getScore(occ);
            if(mixedScore > best) {
                second = best;
                best = mixedScore;
                resultOcc = occ;
                resultOcc.setSimilarityScore(mixedScore);
                if(second > -Double.MAX_VALUE) {
                    resultOcc.setPercentageOfSecondRank(second/best);
                }
            }
        }

        if(resultOcc == null) {
            LOG.error("SearchException: Error when reranking mixture weights "+sfOcc.surfaceForm()); //DisambiguationException
        }

        return resultOcc;
    }

    private class SimScoreComparator implements Comparator<DBpediaResourceOccurrence> {
        public int compare(DBpediaResourceOccurrence me, DBpediaResourceOccurrence other) {
            return Double.compare(other.similarityScore(), me.similarityScore());
        }
        public boolean equals(Object other) {
            return false;
        }
    }

    @Override
    public List<DBpediaResourceOccurrence> bestK(SurfaceFormOccurrence sfOccurrence, int k) throws SearchException, ItemNotFoundException, InputException {
        List<DBpediaResourceOccurrence> bestK = super.bestK(sfOccurrence, k);

        for(DBpediaResourceOccurrence occ : bestK) {
            occ.setSimilarityScore(mixture.getScore(occ));
        }

        Collections.sort(bestK, new SimScoreComparator());

        for(int i=1; i < bestK.size(); i++) {
            DBpediaResourceOccurrence top = bestK.get(i-1);
            DBpediaResourceOccurrence bottom = bestK.get(i);
            top.setPercentageOfSecondRank(bottom.similarityScore()/top.similarityScore()); //TODO similarityScore or contextualScore??
        }

        return bestK;
    }

}
