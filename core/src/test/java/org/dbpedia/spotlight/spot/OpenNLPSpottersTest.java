/*
 * Copyright 2011 Pablo Mendes, Max Jakob
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

package org.dbpedia.spotlight.spot;

import org.dbpedia.spotlight.exceptions.ConfigurationException;
import org.dbpedia.spotlight.exceptions.SpottingException;
import org.dbpedia.spotlight.model.SpotlightConfiguration;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.Text;
import org.junit.Test;

import java.util.List;

/**
 * Current test just tries to run it.
 * TODO Need to provide examples that test each of the NERs
 *
 * @author pablomendes
 */
public class OpenNLPSpottersTest {

     @Test
     public void assureNESpotterRuns() throws ConfigurationException, SpottingException {
        SpotlightConfiguration config = new SpotlightConfiguration("conf/dev.properties");
        Spotter spotter = new NESpotter(config.getSpotterConfiguration().getOpenNLPModelDir());
        Text t = new Text("The financial crunch threatens to undermine a foreign policy described as “smart power” in Texas by President Obama and Secretary of State Hillary Rodham Clinton, one that emphasizes diplomacy and development as a complement to American military power. It also would begin to reverse the increase in foreign aid that President George W. Bush supported after the attacks of Sept. 11, 2001, as part of an effort to combat the roots of extremism and anti-American sentiment, especially in the most troubled countries. ");
        List<SurfaceFormOccurrence> spots = spotter.extract(t);
        System.out.println(spots);
    }

         @Test
     public void assureOpenNLPNGramSpotterRuns() throws ConfigurationException, SpottingException {
        SpotlightConfiguration config = new SpotlightConfiguration("conf/dev.properties");
        Spotter spotter = new OpenNLPNGramSpotter(config.getSpotterConfiguration().getOpenNLPModelDir());
        Text t = new Text("The financial crunch threatens to undermine a foreign policy described as “smart power” in Texas by President Obama and Secretary of State Hillary Rodham Clinton, one that emphasizes diplomacy and development as a complement to American military power. It also would begin to reverse the increase in foreign aid that President George W. Bush supported after the attacks of Sept. 11, 2001, as part of an effort to combat the roots of extremism and anti-American sentiment, especially in the most troubled countries. ");
        List<SurfaceFormOccurrence> spots = spotter.extract(t);
        System.out.println(spots);
    }
}
