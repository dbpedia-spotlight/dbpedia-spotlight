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
import io.Source
import org.dbpedia.spotlight.model.DBpediaResource
import org.apache.lucene.store.FSDirectory
import org.dbpedia.spotlight.exceptions.ConfigurationException
import org.dbpedia.spotlight.log.SpotlightLog
import java.io.File
import org.dbpedia.spotlight.util.IndexingConfiguration

/**
 * This script goes over the index and adds surface forms, counts and DBpedia Types as provided in input files.
 *
 * @author pablomendes
 */

object PatchIndex {

    def uri2count(line : String) = {
        if (line.trim != null) {
            val fields = line.trim.split("\\s+");
            (fields(0), fields(1).toInt) // extracts a map from uri2count from the csv line
        } else {
            ("",0) //
        }
    }

    def main(args : Array[String]) {
        val indexingConfigFileName = args(0)
        val countsFileName = args(1)

        val config = new IndexingConfiguration(indexingConfigFileName)
        val sourceIndexFileName = config.get("org.dbpedia.spotlight.index.dir")
        val targetIndexFileName = sourceIndexFileName+"-withAllPatched"
        val instanceTypesFileName = config.get("org.dbpedia.spotlight.data.instanceTypes")
        val surfaceFormsFileName = config.get("org.dbpedia.spotlight.data.surfaceForms")

        if (!new File(countsFileName).exists)
            throw new ConfigurationException("counts file "+countsFileName+" does not exist")
        if (!new File(instanceTypesFileName).exists)
            throw new ConfigurationException("types file "+instanceTypesFileName+" does not exist")
        if (!new File(surfaceFormsFileName).exists)
            throw new ConfigurationException("surface forms file "+surfaceFormsFileName+" does not exist")

        val sfIndexer = new IndexEnricher(sourceIndexFileName, targetIndexFileName, config)

        val typesMap = AddTypesToIndex.loadTypes(instanceTypesFileName);
        val countsMap: java.util.Map[String, java.lang.Integer] = AddCountsToIndex.loadCounts(countsFileName).asInstanceOf[java.util.Map[String, java.lang.Integer]]
        val sfMap = AddSurfaceFormsToIndex.loadSurfaceForms(surfaceFormsFileName, AddSurfaceFormsToIndex.fromTitlesToAlternatives)

        SpotlightLog.info(this.getClass, "Expunge deletes.")
        sfIndexer.expunge();
        SpotlightLog.info(this.getClass, "Done.")

        SpotlightLog.info(this.getClass, "Patching up index.")
        sfIndexer.patchAll(typesMap, countsMap, sfMap)
        SpotlightLog.info(this.getClass, "Done.")
        sfIndexer.close
    }

}