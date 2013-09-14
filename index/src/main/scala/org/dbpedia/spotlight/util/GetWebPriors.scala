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

/*
 * *
 *  * Copyright 2011 Pablo Mendes, Max Jakob
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

import java.io.PrintWriter
import io.Source
import org.dbpedia.spotlight.model.{SpotlightConfiguration, DBpediaResource}
import org.dbpedia.spotlight.log.SpotlightLog

/**
 * Queries the Web to obtain prior probability counts for each resource
 * The idea is that "huge" will appear much more commonly than "huge TV series" on the Web, even though in Wikipedia that is not the case.
 * This is, thus, an attempt to deal with sparsity in Wikipedia.
 *
 * Keywords are created by @link{ExtractTopicSignatures}
 *
 * @author pablomendes
 */
object GetWebPriors {
    /**
     * This class obtains DBpediaResource Web occurrence counts.
     * It uses a configuration file with parameters for the Yahoo!Boss search API, and takes as input a file with one URI per line.
     *
     */
    def main(args: Array[String]) {

        val indexingConfigFileName = args(0)
        val uriSetFile = args(1)

        val wConfig = new WebSearchConfiguration(indexingConfigFileName);
        val prior = new YahooBossSearcher(wConfig)
        val counts = new PrintWriter(uriSetFile+".counts")
        val queries = Source.fromFile(uriSetFile+".topicsig").getLines // this file is created by ExtractTopicSignatures
        var i = 0;
        queries.foreach( line => {
            i = i + 1

            try {
                val fields = line.split("\t")
                val uri = fields(0)
                val keywords = fields(1)
                SpotlightLog.info(this.getClass, "URI %s : %s", i.toString, uri)
                SpotlightLog.info(this.getClass, "Searching for :%s", keywords)
                val results = prior.getYahooAnswer(keywords)
                counts.print(String.format("%s\t%s\t%s\n", uri, results._1.toString, results._2.toString))

                if (i % 100 == 0) {
                    counts.flush
                } else {
                    Thread.sleep(700)
                }
            } catch {
                case e: java.net.ConnectException => {
                    SpotlightLog.error(this.getClass, "ConnectException: ")
                    e.printStackTrace
                    Thread.sleep(5000)
                }
                case any: Exception => SpotlightLog.error(this.getClass, "Unknown Error: %s", any)
            }
        });

        counts.close
    }

}
