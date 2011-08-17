/**
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dbpedia.spotlight.lucene.index

import org.dbpedia.spotlight.lucene.LuceneManager
import org.apache.lucene.store.FSDirectory
import java.io.File
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.model.SurfaceForm
import org.dbpedia.spotlight.util.{IndexingConfiguration, SurrogatesUtil}

/**
 * In our first implementation we used to index all anchor text found in Wikipedia as surface forms to a target URI.
 * We have since moved to indexing without surface forms, then pre-processing the anchors before indexing.
 * Some possible pre-processing steps: eliminating low count surface forms, adding acronyms, name variations, etc.
 * TODO think of some stemming to provide more alternative matches for a sf (e.g. http://www.mpi-inf.mpg.de/yago-naga/javatools/doc/javatools/parsers/PlingStemmer.html)
 *
 * @author maxjakob
 */

object AddSurfaceFormsToIndex
{

    def main(args : Array[String]) {
        val indexingConfigFileName = args(0)

        val config = new IndexingConfiguration(indexingConfigFileName)
        val indexFileName = config.get("org.dbpedia.spotlight.index.dir")
        val surfaceFormsFileName = config.get("org.dbpedia.spotlight.data.surfaceForms")

        val lowerCased = true

        val indexFile = new File(indexFileName)
        if (!indexFile.exists)
            throw new IllegalArgumentException("index dir "+indexFile+" does not exist; can't add surface forms")
        val luceneManager = new LuceneManager.BufferedMerging(FSDirectory.open(indexFile))

        val sfIndexer = new IndexEnricher(luceneManager)
        val sfMap = SurrogatesUtil.getSurfaceFormsMap_java(new File(surfaceFormsFileName), lowerCased)
        sfIndexer.enrichWithSurfaceForms(sfMap)
        sfIndexer.close
    }

}