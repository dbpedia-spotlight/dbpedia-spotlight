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
import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.Text;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Sees if spotters run.
 * Tests if offsets are correct.
 * TODO Need to provide tough examples that test each of the spotters
 *
 * @author pablomendes
 */
public class OpenNLPSpottersTest {

/*
The financial crunch in the U.S. threatens to undermine a foreign policy described as “smart power” in Texas by President Obama and Secretary of State Hillary Rodham Clinton, one that emphasizes diplomacy and development as a complement to American military power. It also would begin to reverse the increase in foreign aid that President George W. Bush supported after the attacks of Sept. 11, 2001, as part of an effort to combat the roots of extremism and anti-American sentiment, especially in the most troubled countries.
*/
    Text t = new Text("The financial crunch in the U.S. threatens to undermine a foreign policy described as “smart power” in Texas by President Obama and Secretary of State Hillary Rodham Clinton, one that emphasizes diplomacy and development as a complement to American military power. It also would begin to reverse the increase in foreign aid that President George W. Bush supported after the attacks of Sept. 11, 2001, as part of an effort to combat the roots of extremism and anti-American sentiment, especially in the most troubled countries. ");
    SurfaceFormOccurrence financialCrunch = new SurfaceFormOccurrence(new SurfaceForm("financial crunch"), t, 4);
    SurfaceFormOccurrence smartPower = new SurfaceFormOccurrence(new SurfaceForm("smart power"), t, 87);
    SurfaceFormOccurrence quotedSmartPower = new SurfaceFormOccurrence(new SurfaceForm("“smart power”"), t, 86);
    SurfaceFormOccurrence us = new SurfaceFormOccurrence(new SurfaceForm("U.S."), t, 28);
    List<SurfaceFormOccurrence> occs = Arrays.asList(financialCrunch, smartPower, quotedSmartPower, us);

    SpotlightConfiguration config = null;



    public OpenNLPSpottersTest() throws ConfigurationException {
        config = new SpotlightConfiguration("conf/dev.properties");
    }


    private void print(List<SurfaceFormOccurrence> spots) {
        for (SurfaceFormOccurrence spot: spots) {
            System.err.println(String.format("%s at %s",spot.surfaceForm(), spot.textOffset()));
        }
    }

    private void testOffsets(List<SurfaceFormOccurrence> spots) {
        for (SurfaceFormOccurrence spot: spots) {
            assertTrue(testOffset(spot));
        }
    }

    private boolean testOffset(SurfaceFormOccurrence occ) {
        String name = occ.surfaceForm().name();
        int offset = occ.textOffset();
        int length = name.length();
        String text = occ.context().text();
        String extracted = text.substring(offset, offset+length);
        System.err.println(String.format("(%s) -> (%s)", name, extracted).replaceAll(" ","_")); //make spaces more visible
        return name.equals(extracted);
    }

    @Test
    public void assureOccsAreCorrectlyCreated() throws ConfigurationException, SpottingException {
        for (SurfaceFormOccurrence spot: occs) {
            assertTrue(testOffset(spot));
        }
    }

    @Test
    public void assureNESpotterRuns() throws ConfigurationException, SpottingException {
        Spotter ner = new NESpotter(config.getSpotterConfiguration().getOpenNLPModelDir() + "/"+config.getLanguage().toLowerCase()+"/", config.getI18nLanguageCode(), config.getSpotterConfiguration().getOpenNLPModelsURI());
        List<SurfaceFormOccurrence> spots = ner.extract(t);
        print(spots);
    }

    @Test
    public void correctNEOffsets() throws ConfigurationException, SpottingException {
        Spotter ner = new NESpotter(config.getSpotterConfiguration().getOpenNLPModelDir() + "/"+config.getLanguage().toLowerCase()+"/", config.getI18nLanguageCode(), config.getSpotterConfiguration().getOpenNLPModelsURI());
        List<SurfaceFormOccurrence> spots = ner.extract(t);
        testOffsets(spots);
    }

    @Test
    public void assureOpenNLPNGramSpotterRuns() throws ConfigurationException, SpottingException {
        OpenNLPNGramSpotter chunkSpotter = new OpenNLPNGramSpotter(config.getSpotterConfiguration().getOpenNLPModelDir() + "/"  + config.getLanguage().toLowerCase(), config.getI18nLanguageCode());
        List<SurfaceFormOccurrence> spots = chunkSpotter.extract(t);
        print(spots);
    }

    @Test
    public void correctChunkerOffsets() throws SpottingException, ConfigurationException {
        OpenNLPNGramSpotter chunkSpotter = new OpenNLPNGramSpotter(config.getSpotterConfiguration().getOpenNLPModelDir()+ "/"  + config.getLanguage().toLowerCase(), config.getI18nLanguageCode());
        List<SurfaceFormOccurrence> spots = chunkSpotter.extract(t);
        System.err.println(financialCrunch);
        testOffsets(spots);
        //assertTrue(spots.contains(financialCrunch));
        //assertTrue(spots.contains(new SurfaceFormOccurrence(new SurfaceForm("smart power"), t, 75)));
        //assertTrue(!spots.contains(new SurfaceFormOccurrence(new SurfaceForm("“smart power”"),t,75)));
    }
}
