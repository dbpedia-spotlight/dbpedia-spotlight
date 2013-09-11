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

package org.dbpedia.spotlight.filter.occurrences

import org.dbpedia.spotlight.model.{DBpediaResource, DBpediaResourceOccurrence}
import io.Source
import java.io.File
import org.dbpedia.spotlight.log.SpotlightLog


class RedirectResolveFilter(val redirects : Map[String,String]) extends OccurrenceFilter {

    def touchOcc(occ : DBpediaResourceOccurrence) : Option[DBpediaResourceOccurrence] = {
        redirects.get(occ.resource.uri) match {
            case Some(targetUri) => {
                val resolvedResource = new DBpediaResource(targetUri, occ.resource.support, occ.resource.prior, occ.resource.types)
                Some(new DBpediaResourceOccurrence(occ.id, resolvedResource, occ.surfaceForm, occ.context, occ.textOffset, occ.provenance, occ.similarityScore, occ.percentageOfSecondRank, occ.contextualScore))
            }
            case None => Some(occ)
        }
    }

}

object RedirectResolveFilter {

    def fromFile(redirectTCFileName: File) : RedirectResolveFilter = {
        SpotlightLog.info(this.getClass, "Loading redirects transitive closure from %s...", redirectTCFileName)
        val redirectsTCMap = Source.fromFile(redirectTCFileName, "UTF-8").getLines.map{ line =>
            val elements = line.split("\t")
            (elements(0), elements(1))
        }.toMap
        new RedirectResolveFilter(redirectsTCMap)
    }
}