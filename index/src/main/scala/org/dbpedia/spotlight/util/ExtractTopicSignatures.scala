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


import java.io.PrintWriter
import io.Source
import org.dbpedia.spotlight.model.{SpotlightConfiguration, DBpediaResource}
import org.dbpedia.spotlight.log.SpotlightLog

/**
 * Extract topic signatures for DBpedia Resources according to DBpedia Spotlight's index.
 * @author pablomendes
 */
object ExtractTopicSignatures {
    def main(args: Array[String]) {

        if (args.size!=4)
            println("Usage: java -jar dbpedia-spotlight.jar org.dbpedia.spotlight.util.ExtractTopicSignatures server.properties uri.set stopwords.set nKeywords ")

        val spotlightConfigFileName = args(0) // configuration for spotlight conf/server.properties
        val uriSetFile = args(1)              // set of URIs for which you'd like to extract topic signatures
        val stopwordsFile = args(2)           // set of words to exclude from the topic signature
        val nKeywords = args(3).toInt         // number of keywords to include in the topic signature

        val sConfig = new SpotlightConfiguration(spotlightConfigFileName);
        val extractor = new KeywordExtractor(sConfig, nKeywords)
        val signatures = new PrintWriter(uriSetFile+".topicsig")
        val uriSet = Source.fromFile(uriSetFile).getLines
        val stopwords = Source.fromFile(stopwordsFile).getLines.toSet
        var i = 0;
        uriSet.foreach( uri => {
            i = i + 1
            SpotlightLog.info(this.getClass, "URI %s : %s", i.toString, uri)
            try {
                val keywords = extractor.getKeywords(new DBpediaResource(uri))
                val filteredKeywords = keywords.filterNot(stopwords.contains(_)).mkString(" ");
                //TODO allow another stopwording here?
                signatures.print(String.format("%s\t%s\n", uri, filteredKeywords))
                if (i % 100 == 0) {
                    signatures.flush
                }
            } catch {
                case any: Exception => SpotlightLog.error(this.getClass, "Unknown Error: %s", any)
            }
        });

        //counts.close
        signatures.close
    }

}
