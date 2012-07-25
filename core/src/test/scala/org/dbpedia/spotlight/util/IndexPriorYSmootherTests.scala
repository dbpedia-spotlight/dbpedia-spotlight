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

package org.dbpedia.spotlight.util


import org.junit.Test
import org.dbpedia.spotlight.model.{SpotlightConfiguration, DBpediaResource}

/**
 * Created by IntelliJ IDEA.
 * User: pablo
 * Date: 4/13/11
 * Time: 5:23 PM
 * To change this template use File | Settings | File Templates.
 */

class IndexPriorYSmootherTests {

    val extractor = new KeywordExtractor(new SpotlightConfiguration("conf/eval.properties"))

    @Test
    def uriToKeywords {
        val examples = Map("Huge"->"+\"Huge\"",
            "Huge_(TV_series)"->"+\"Huge\" +\"TV series\"",
            "Huge_cardinal"->"+\"Huge cardinal\"",
            "Apple_(disambiguation)"->"+\"Apple\"",
            "Apple_%28disambiguation%29"->"+\"Apple\"");

        examples.keys.foreach( title => {
            val s = extractor.createKeywordsFromDBpediaResourceURI(new DBpediaResource(title))
            printf("%-30s=%30s \n",examples(title),s)
            assert(s.equals(examples(title)));
        });
    }

    @Test
    def augmentKeywords {
        val examples = Map("Huge"->"+\"Huge\"",
            "Huge_(TV_series)"->"+\"Huge\" +\"TV series\"",
            "Huge_cardinal"->"+\"Huge cardinal\"",
            "Apple_(disambiguation)"->"+\"Apple\"",
            "Apple_%28disambiguation%29"->"+\"Apple\"");

        examples.keys.foreach( title => {
            val r = new DBpediaResource(title)
            println(r);
            println(extractor.getKeywords(r))
            println;
        });

    }



}