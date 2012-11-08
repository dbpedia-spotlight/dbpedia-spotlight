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

import java.io.{FileInputStream, File}
import org.dbpedia.spotlight.util.{IndexingConfiguration, TypesLoader}
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

/**
 * Reads file instance_types_en.nt from DBpedia in order to add Ontology Resource Types to the index.
 *
 * Usage: see bin/index.sh
 *
 */
object AddTypesToIndex {

    def loadTypes(instanceTypesFileName: String) = {
        val input = new BZip2CompressorInputStream(new FileInputStream(instanceTypesFileName), true)
        val typesMap = instanceTypesFileName.contains(".tsv") match {
          case true  => TypesLoader.getTypesMapFromTSV_java(input)
          case false => TypesLoader.getTypesMap_java(input)
        }
        input.close()
        typesMap
    }

    def main(args : Array[String]) {

        val indexingConfigFileName = args(0)
        val sourceIndexFileName = args(1)

        val config = new IndexingConfiguration(indexingConfigFileName)
        val targetIndexFileName = sourceIndexFileName+"-withTypes"
        val instanceTypesFileName = config.get("org.dbpedia.spotlight.data.instanceTypes")

        val typesIndexer = new IndexEnricher(sourceIndexFileName,targetIndexFileName, config)

        val typesMap = loadTypes(instanceTypesFileName)
        typesIndexer.enrichWithTypes(typesMap)
        typesIndexer.close
    }

}
