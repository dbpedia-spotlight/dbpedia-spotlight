/*
 * Copyright 2012 DBpedia Spotlight Development Team
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
 */

package org.dbpedia.spotlight.evaluation;

import net.sf.json.JSONException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.exceptions.InitializationException;
import org.dbpedia.spotlight.exceptions.SpottingException;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.model.Text;
import org.dbpedia.spotlight.spot.OpenNLPUtil;
import org.dbpedia.spotlight.spot.Spotter;
import org.dbpedia.spotlight.spot.lingpipe.LingPipeSpotter;
import org.dbpedia.spotlight.spot.opennlp.OpenNLPChunkerSpotter;
import org.dbpedia.spotlight.spot.opennlp.ProbabilisticSurfaceFormDictionary;
import org.dbpedia.spotlight.spot.opennlp.SurfaceFormDictionary;

import java.io.File;
import java.io.IOException;


/**
 * Evaluator for {@link org.dbpedia.spotlight.spot.Spotter}s (and spot selectors).
 *
 * @author pablomendes (based on Spotter Evaluator from Joachim Daiber, modified for precision/recall calculations)
 * @author Joachim Daiber
 */
public class SpotterMemoryEvaluator {

    private final static Log LOG = LogFactory.getLog(SpotterMemoryEvaluator.class);

    public static void main(String[] args) throws IOException, JSONException, ConfigurationException, InitializationException, org.json.JSONException, SpottingException {

        SpotlightConfiguration configuration = new SpotlightConfiguration("conf/server.properties");

        File dictionary = new File("/Users/jodaiber/Desktop/lrec_2012_spotting/surface_forms-Wikipedia-TitRedDis.thresh3.spotterDictionary");

        Spotter spotter = null;

        int spotterNr = 0;

        switch(spotterNr) {
            case 0: {
                String openNLPDir = "/Users/jodaiber/Desktop/DBpedia/";
                SurfaceFormDictionary sfDictProbThresh3 = ProbabilisticSurfaceFormDictionary.fromLingPipeDictionary(dictionary, false);
                spotter = new OpenNLPChunkerSpotter(new File(openNLPDir,OpenNLPUtil.OpenNlpModels.SentenceModel.filename()+".bin"),
                        new File(openNLPDir,OpenNLPUtil.OpenNlpModels.TokenizerModel.filename()+".bin"),
                        new File(openNLPDir,OpenNLPUtil.OpenNlpModels.POSModel.filename()+".bin"),
                        new File(openNLPDir, OpenNLPUtil.OpenNlpModels.ChunkModel.filename()+".bin"),
                        sfDictProbThresh3);
                break;
            }
            case 1: {
                spotter = new LingPipeSpotter(dictionary);
                break;
            }
        }

        System.out.println("Using Spotter " + spotter.getName());

        System.out.println("Running GC.");
        System.gc(); System.gc(); System.gc(); System.gc();

        System.out.println("Memory consumption: " +
                (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024));

        System.out.println(spotter.extract(new Text("New York is a city.")));
    }
}
