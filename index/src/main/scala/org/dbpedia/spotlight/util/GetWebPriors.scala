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
import org.apache.commons.logging.LogFactory

/**
 * Queries the Web to obtain prior probability counts for each resource
 * The idea is that "huge" will appear much more commonly than "huge TV series" on the Web, even though in Wikipedia that is not the case.
 * This is, thus, an attempt to deal with sparsity in Wikipedia.
 * @author pablomendes
 */
object GetWebPriors {
    private val LOG = LogFactory.getLog(this.getClass)

    /**
     * This class obtains DBpediaResource Web occurrence counts.
     * It uses a configuration file with parameters for the Yahoo!Boss search API, and takes as input a file with one URI per line.
     *
     */
    def main(args: Array[String]) {

        val indexingConfigFileName = args(0)
        val spotlightConfigFileName = args(1)
        val uriSetFile = args(2)

        val sConfig = new SpotlightConfiguration(spotlightConfigFileName);
        val wConfig = new WebSearchConfiguration(indexingConfigFileName);
        val prior = new YahooBossSearcher(wConfig)
        val extractor = new KeywordExtractor(sConfig)
        val counts = new PrintWriter(uriSetFile+".counts")
        val queries = new PrintWriter(uriSetFile+".queries")
        val uriSet = Source.fromFile(uriSetFile).getLines
        var i = 0;
        uriSet.foreach( uri => {
            i = i + 1
            LOG.info(String.format("URI %s : %s", i.toString, uri));
            try {
                val keywords = extractor.getAllKeywords(new DBpediaResource(uri));
                queries.print(String.format("%s\t%s\n", uri, keywords))
                LOG.info("Searching for :"+keywords)
                val results = prior.getYahooAnswer(keywords)
                counts.print(String.format("%s\t%s\t%s\n", uri, results._1.toString, results._2.toString))

                if (i % 100 == 0) {
                    counts.flush
                    queries.flush
                } else {
                    //Thread.sleep(700)
                }
            } catch {
                case e: java.net.ConnectException => {
                    LOG.error("ConnectException: ")
                    e.printStackTrace
                    Thread.sleep(5000)
                }
                case any: Exception => LOG.error("Unknown Error: "+any)
            }
        });

        counts.close
        queries.close
    }

}