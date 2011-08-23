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

package org.dbpedia.spotlight.lucene.index

import scalaj.collection.Imports._
import org.dbpedia.spotlight.lucene.LuceneManager
import org.dbpedia.spotlight.util.{ExtractCandidateMap, IndexingConfiguration}
import java.io.File
import io.Source
import org.dbpedia.spotlight.model.DBpediaResource
import org.apache.lucene.store.FSDirectory
import org.apache.avalon.framework.configuration.ConfigurationException

/**
 *TODO UNTESTED!!!!!!!!!!!!
 * @author pablomendes
 */

object AddPriorsToIndex {

    def uri2prior(line : String) = {
        if (line.trim != null) {
            val fields = line.split("\\s+");
            (fields(0), fields(1).toDouble) // extracts a map from uri2prior from the csv line
        } else {
            ("",0.0) //
        }
    }

    def main(args : Array[String]) {
        val indexingConfigFileName = args(0)

        val config = new IndexingConfiguration(indexingConfigFileName)
        val indexFileName = config.get("org.dbpedia.spotlight.index.dir")
        val priorsFileName = config.get("org.dbpedia.spotlight.data.priors")

        val lowerCased = true

        val indexFile = new File(indexFileName)
        if (!indexFile.exists)
            throw new ConfigurationException("index dir "+indexFile+" does not exist; can't add priors")
        if (!new File(priorsFileName).exists)
            throw new ConfigurationException("priors file "+priorsFileName+" does not exist; can't add priors")

        val luceneManager = new LuceneManager.BufferedMerging(FSDirectory.open(indexFile))

        val sfIndexer = new IndexEnricher(luceneManager)

        var uriPriorMap = Map[String,Double]()
        var total = 0.0
        Source.fromFile(priorsFileName).getLines.foreach(line => {
            val entry  = uri2prior(line);
            uriPriorMap += entry; // append entry to map
            total = total + entry._2
        });
        total = Math.min(total, 11.5 * 1000000000) // http://www.cs.uiowa.edu/~asignori/papers/the-indexable-web-is-more-than-11.5-billion-pages/
        val priors = uriPriorMap.map( e => new DBpediaResource(e._1) -> new java.lang.Double(e._2 / total) ).asJava
        sfIndexer.enrichWithPriors(priors)
        sfIndexer.close
    }

}