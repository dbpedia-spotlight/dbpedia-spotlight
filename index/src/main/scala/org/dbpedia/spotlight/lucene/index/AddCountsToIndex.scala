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

package org.dbpedia.spotlight.lucene.index

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

import scala.collection.JavaConverters._
import org.dbpedia.spotlight.util.IndexingConfiguration
import java.io.File
import io.Source
import org.dbpedia.spotlight.model.DBpediaResource
import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.exceptions.ConfigurationException

/**
 * If you indexed occurrences without storing counts and want to add them to the index.
 * Or if you have a better source of counts besides wikipedia, you can use this script to load counts from a file.
 *
 * @author pablomendes
 */

object AddCountsToIndex {

    def uri2count(line : String) = {
        if (line.trim != null) {
            val fields = line.split("\\s+");
            (fields(0), fields(1).toInt) // extracts a map from uri2count from the csv line
        } else {
            ("",0) //
        }
    }

    def loadCounts(countsFileName: String) = {
        var uriCountMap = Map[String,Int]()
        var total = 0
        SpotlightLog.info(this.getClass, "Loading counts into memory.")
        Source.fromFile(countsFileName).getLines.foreach(line => {
            val entry  = uri2count(line);
            uriCountMap += entry; // append entry to map
            total = total + entry._2
        });
        SpotlightLog.info(this.getClass, "Total count of %d loaded.", total)
        SpotlightLog.info(this.getClass, "Reformatting...")
        val counts = uriCountMap.map( e => new DBpediaResource(e._1).uri -> e._2 ).asJava
        SpotlightLog.info(this.getClass, "Done.")
        counts
    }

    def main(args : Array[String]) {
        val indexingConfigFileName = args(0)
        val countsFileName = args(1)
        val sourceIndexFileName = args(2)

        val config = new IndexingConfiguration(indexingConfigFileName)
        val targetIndexFileName = sourceIndexFileName+"-withCounts"

        if (!new File(countsFileName).exists)
            throw new ConfigurationException("counts file "+countsFileName+" does not exist; can't add counts")

        val sfIndexer = new IndexEnricher(sourceIndexFileName,targetIndexFileName,config)

        val counts : java.util.Map[String, java.lang.Integer]= loadCounts(countsFileName).asInstanceOf[java.util.Map[String, java.lang.Integer]]
        SpotlightLog.info(this.getClass, "Adding to index.")
        sfIndexer.enrichWithCounts(counts)
        SpotlightLog.info(this.getClass, "Done.")
        sfIndexer.close
    }

}