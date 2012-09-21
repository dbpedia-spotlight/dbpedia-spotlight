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

import java.io.File
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.string.ContextExtractor
import org.dbpedia.spotlight.util.IndexingConfiguration
import org.dbpedia.spotlight.filter.occurrences.{RedirectResolveFilter, UriWhitelistFilter, ContextNarrowFilter}
import org.dbpedia.spotlight.io._
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence
import org.dbpedia.spotlight.BzipUtils

/**
 * Saves Occurrences to a TSV file.
 * - Surface forms are taken from anchor texts
 * - Redirects are resolved
 *
 * TODO think about having a two file output, one with (id, sf, uri) and another with (id, context)
 *
 * Used to be called SurrogatesUtil
 *
 * @author maxjakob
 * @author pablomendes (small fixes)
 */
object ExtractOccsFromWikipedia {

    private val LOG = LogFactory.getLog(this.getClass)

    def main(args : Array[String]) {
        val indexingConfigFileName = args(0)
        val targetFileName = args(1)

        val config = new IndexingConfiguration(indexingConfigFileName)
        val wikiDumpFileName    = BzipUtils.extract(config.get("org.dbpedia.spotlight.data.wikipediaDump"))
        val conceptURIsFileName = config.get("org.dbpedia.spotlight.data.conceptURIs")
        val redirectTCFileName  = config.get("org.dbpedia.spotlight.data.redirectsTC")
        val maxContextWindowSize  = config.get("org.dbpedia.spotlight.data.maxContextWindowSize").toInt
        val minContextWindowSize  = config.get("org.dbpedia.spotlight.data.minContextWindowSize").toInt


        val conceptUriFilter = UriWhitelistFilter.fromFile(new File(conceptURIsFileName))

        val redirectResolver = RedirectResolveFilter.fromFile(new File(redirectTCFileName))

        val narrowContext = new ContextExtractor(minContextWindowSize, maxContextWindowSize)
        val contextNarrowFilter = new ContextNarrowFilter(narrowContext)

        val filters = (conceptUriFilter :: redirectResolver :: contextNarrowFilter :: Nil)

        val occSource : Traversable[DBpediaResourceOccurrence] = AllOccurrenceSource.fromXMLDumpFile(new File(wikiDumpFileName))
        //val filter = new OccurrenceFilter(redirectsTC = redirectsTCMap, conceptURIs = conceptUrisSet, contextExtractor = narrowContext)
        //val occs = filter.filter(occSource)

        val occs = filters.foldLeft(occSource){ (o,f) => f.filterOccs(o) }

        FileOccurrenceSource.writeToFile(occs, new File(targetFileName))

        LOG.info("Occurrences saved to: "+targetFileName)

    }
}