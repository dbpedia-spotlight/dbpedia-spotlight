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
import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.string.ContextExtractor
import org.dbpedia.spotlight.util.IndexingConfiguration
import org.dbpedia.spotlight.filter.occurrences.{RedirectResolveFilter, UriWhitelistFilter, ContextNarrowFilter}
import org.dbpedia.spotlight.io._
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence
import org.dbpedia.spotlight.BzipUtils
import org.dbpedia.extraction.util.Language

/**
 * Saves Occurrences to a TSV file.
 * - Surface forms are taken from anchor texts
 * - Redirects are resolved
 *
 * TODO think about having a two file output, one with (id, sf, uri) and another with (id, context)
 * TODO allow reading from a bzipped wikiDump (needs upgrading our dependency on the DBpedia Extraction Framework)
 *
 * Used to be called SurrogatesUtil
 *
 * @author maxjakob
 * @author pablomendes (small fixes)
 */
object ExtractOccsFromWikipedia {

    def main(args : Array[String]) {
        val indexingConfigFileName = args(0)
        val targetFileName = args(1)

        val config = new IndexingConfiguration(indexingConfigFileName)
        var wikiDumpFileName    = config.get("org.dbpedia.spotlight.data.wikipediaDump")
        val conceptURIsFileName = config.get("org.dbpedia.spotlight.data.conceptURIs")
        val redirectTCFileName  = config.get("org.dbpedia.spotlight.data.redirectsTC")
        val maxContextWindowSize  = config.get("org.dbpedia.spotlight.data.maxContextWindowSize").toInt
        val minContextWindowSize  = config.get("org.dbpedia.spotlight.data.minContextWindowSize").toInt
        val languageCode = config.get("org.dbpedia.spotlight.language_i18n_code")


        if (wikiDumpFileName.endsWith(".bz2")) {
            SpotlightLog.warn(this.getClass, "The DBpedia Extraction Framework does not support parsing from bz2 files. You can stop here, decompress and restart the process with an uncompressed XML.")
            SpotlightLog.warn(this.getClass, "If you do not stop the process, we will decompress the file into the /tmp/ directory for you.")
            wikiDumpFileName = BzipUtils.extract(wikiDumpFileName)
        }

        val conceptUriFilter = UriWhitelistFilter.fromFile(new File(conceptURIsFileName))

        val redirectResolver = RedirectResolveFilter.fromFile(new File(redirectTCFileName))

        val narrowContext = new ContextExtractor(minContextWindowSize, maxContextWindowSize)
        val contextNarrowFilter = new ContextNarrowFilter(narrowContext)

        val filters = (conceptUriFilter :: redirectResolver :: contextNarrowFilter :: Nil)

        val occSource : Traversable[DBpediaResourceOccurrence] = AllOccurrenceSource.fromXMLDumpFile(new File(wikiDumpFileName), Language(languageCode))
        //val filter = new OccurrenceFilter(redirectsTC = redirectsTCMap, conceptURIs = conceptUrisSet, contextExtractor = narrowContext)
        //val occs = filter.filter(occSource)

        val occs = filters.foldLeft(occSource){ (o,f) => f.filterOccs(o) }

        FileOccurrenceSource.writeToFile(occs, new File(targetFileName))

        SpotlightLog.info(this.getClass, "Occurrences saved to: %s", targetFileName)

    }
}