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

package org.dbpedia.spotlight.model

import org.junit.Test
import org.junit.Assert._
import org.dbpedia.spotlight.io.AnnotatedTextSource
import org.dbpedia.spotlight.filter.annotations.CoreferenceFilter


/**
 * Illustrative test.
 * @author pablomendes
 */

class AnnotationFilterTests {


    /**
     * This test feeds in two occurrences of Marilyn Manson, but one only contains the first name.
     * The filter should be able to mark them as coreferent.
     */
    val exampleOccs = "Terra_Vibe_Park-p10l10\tWRONG\tMarilyn\tIron Maiden Black Sabbath Velvet Revolver Black Label Society  Dragonforce Marilyn Manson  Moby Slayer  Accept  Candlemass  Twisted Sister  Marilyn Dio  Anthrax Cure  Korn\t128\n"+
                      "Terra_Vibe_Park-p10l10\tMarilyn_Manson\tMarilyn Manson\tIron Maiden Black Sabbath Velvet Revolver Black Label Society  Dragonforce Marilyn Manson  Moby Slayer  Accept  Candlemass  Twisted Sister  Marilyn Dio  Anthrax Cure  Korn\t75\n";
    @Test
    def testCoreference {
        val paragraphs = AnnotatedTextSource.fromOccurrencesString(exampleOccs).toList
        println("%s paragraphs".format(paragraphs.size))
        println("%s occurrences".format(paragraphs.foldLeft(0)( (acc,p) => acc + p.occurrences.size)))
        val originalEntities = paragraphs.map( p => p.occurrences.map(_.resource) ).flatten
        println(originalEntities)
        val filter = new CoreferenceFilter
        val coreferentEntities = paragraphs.map( p => filter.filterOccs(p.occurrences).map(_.resource)).flatten
        println(coreferentEntities)
        assertTrue(coreferentEntities.forall( r => r.uri=="Marilyn_Manson" ))
    }

}