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
import org.dbpedia.spotlight.io.{TypeAdder, FileOccurrenceSource}
import org.dbpedia.spotlight.model.{DBpediaType, DBpediaResource}
import java.io.{FileInputStream, File}
import org.dbpedia.spotlight.util.{IndexingConfiguration, TypesLoader}

/**
 * Reads file instance_types_en.nt from DBpedia in order to add DBpedia Resource Types to the index.
 *
 * @author maxjakob
 */
object AddTypesToIndex {

    def loadTypes(instanceTypesFileName: String) = {
        TypesLoader.getTypesMap_java(new FileInputStream(instanceTypesFileName));
    }

    def main(args : Array[String]) {
        val indexingConfigFileName = args(0)

        val config = new IndexingConfiguration(indexingConfigFileName)
        val indexFileName = config.get("org.dbpedia.spotlight.index.dir")
        val instanceTypesFileName = config.get("org.dbpedia.spotlight.data.instanceTypes")

        val indexFile = new File(indexFileName)
        if (!indexFile.exists) {
            throw new IllegalArgumentException("index dir "+indexFile+" does not exist; can't add types")
        }
        val luceneManager = new LuceneManager.BufferedMerging(FSDirectory.open(indexFile))

        val typesIndexer = new IndexEnricher(luceneManager)

        val typesMap = loadTypes(instanceTypesFileName)
        typesIndexer.enrichWithTypes(typesMap)
        typesIndexer.close
    }

}
