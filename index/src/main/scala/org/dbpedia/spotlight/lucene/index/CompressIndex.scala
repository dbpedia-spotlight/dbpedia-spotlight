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

import org.dbpedia.spotlight.lucene.LuceneManager
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.util.IndexingConfiguration


/**
 * This utility removes some fields, and optimizes the index to a few segments in order to increase space efficiency.
 * The only fields we need to read from the search classes are URI and URI_COUNT. The rest does not need to be retrievable, only searchable.
 * Keeping multiple segments in the index may allow better multithreading, so do not optimize to just one.
 * You can also specify a minimum URI count. Every document with less than the specified count will be removed.
 *
 * Usage:
 *    mvn scala:run CompressIndex [indexing config file] [mininum URI count]
 * @author maxjakob
 * @author pablomendes (separated source and target dirs)
 */
object CompressIndex
{
    val unstoreFields = List( LuceneManager.DBpediaResourceField.CONTEXT,
                              LuceneManager.DBpediaResourceField.SURFACE_FORM
    )

    val optimizeSegments = 4

    def main(args : Array[String]) {
        val indexingConfigFileName = args(0)
        val minCount = if (args.length>1) args(1).toInt else 0;
        val sourceIndexFileName = args(2)

        val config = new IndexingConfiguration(indexingConfigFileName)
        val targetIndexFileName = sourceIndexFileName+"-compressed"
        
        val compressor = new IndexEnricher(sourceIndexFileName, targetIndexFileName, config)
        compressor.unstore(unstoreFields, optimizeSegments, minCount)
        compressor.close
    }

}